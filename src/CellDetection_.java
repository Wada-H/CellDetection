
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;


import hw.celldetection.*;


/*
 *	CellDetection
 * 		This plugin just put automatic and manual detected Roi on overlay.
 * 		And it can export vertices of polygonal Roi as TSV file.(also import)
 * 		The recorded Rois are measured by ImageJ.(just macro)
 *  
 *	20160719	version 0.8
 *		輝度を元に境界を認識するRoiをoverlayへ登録するplugin
 *		toRoimanagerを行うことでmeasureが可能とする
 *	20160808	version 1.0
 *		manual登録機能追加
 *		tsvファイルへの読み書き機能追加
 *		measureボタン追加
 *			->このためRoiManageerを介さず、measureが可能に。
 *
 *	20160816	version 1.1
 * 		再帰を使わないアルゴリズムによりstackoverflowを回避
 *		roiの拡張時に起こる不具合時（おそらく、Roiの内側にも頂点がある状況）にこの Roiを無視する設定
 *			->なんとかうまく表示できないか試みるも、惨敗。。。このためここまでをv1.1とする。
 *
 *	20160817
 *		stackoverflowを回避したことによる大きすぎるサイズを認識しようとして時間がかかる問題がある。
 *			->CellDetectorにおけるdetectSize(ポリゴンの頂点数)が1000より大きい場合はnullとすることで回避
 *		クリックまたはoverlayへの登録が起こるとtをひとつ進ませる機能を追加する
 *
 *	20160823
 *		saveを行うとfitsplineが切れる
 *			->save中に、removeSplineを行っている。これをしないとsplineした線の点が保存されてしまう。
 *			->この問題を回避するために色々巡って、doubleからの四捨五入である程度回避
 *		measure時の表示順番を変更
 *			->急遽変更したため、不備がありそう。見直す予定
 *
 *	20160923
 *		measure時の表示順の見直し
 *			とりあえず以前の方法にもどす。
 *		
 *		Overlayの任意の位置にROIを登録する機能をつけるためZahyoでもちいたOverlayのラッパーの使用を考える。
 *
 *	20170215
 *		頂点のソートを見直し、軽量化して1000point上限を5000pointに変更
 *	
 *	20170217
 *		progress barを表示したかったが、頂点sort付近(処理前であっても)で記述すると処理後に反映されて意味がないため
 *		マウスクリック操作をトリガーとした。苦肉の策。->要改善項目とする。
 *
 *	20170301
 *		ROIのコピー機能に着手
 *			->ok
 *			この機能により少しの変化等の場合ひとつ前のROIをコピーすることで手動での微調整によりROIを設定することができる
 *		
 *		ROIを書き込んだ画像を作る機能の追加
 *			->ok
 *
 *	20180326
 *		小椋君よりROIが複数同じ場所に登録されてしまう現象を聞く。
 *		 ->Manual mode時にどうやらROIを選択直後に数字部分をクリックすることで発生
 *		 	->mouseReleasedの処理においてoverlayに登録するが、同じROIであることをcheckすることで回避
 *		 		->この問題は他の同様なpluginにて起こりうる。(CrossSectionViewerで確認。同様に対処)
 */



@SuppressWarnings("serial")
public class CellDetection_ extends PlugInFrame implements MouseListener, ImageListener, MouseWheelListener, WindowListener,ItemListener{

	ImagePlus imp;
	ImageCanvas ic;

	Overlay currentOverlay;
	Overlay ol[];//stacksize
	Overlay saveOl[];//for save
	Overlay backupOl[];//stacksize
	
	Color strokeColor;
	
	JLabel intensityLimitLabel;
	JTextField intensityLimit;

	JLabel countLimitLabel;
	JTextField countLimit;

	JLabel radiusLabel;
	JTextField radius;
	
	JLabel expansionLabel;
	JTextField expansion;

	JLabel roughnessLabel;
	JTextField roughness;
	
	JLabel strokeWidthLabel;
	JTextField strokeWidth;
	
	ButtonGroup radioGroup;
	JRadioButton radioAutoDetect;
	JRadioButton radioManual;
	JRadioButton radioEdit;

	
	JCheckBox checkOverlay;
	JCheckBox checkSpline;
	JCheckBox checkAutoForwardT;
	
	JLabel colorBoxLabel;
	JComboBox<String> colorBox;
	String[] colors = {"White","Black","Red","Green","Blue","Yellow","Cyan","Magenta"};	
	//String[] colors = IJ.getLuts();

	JComboBox<String> methodBox;
	String[] methods = {"MyMethod"};

	JComboBox<String> copyBox;
	String[] target = {"Pre T","Pre Z", "Next T", "Next Z"};
	
	JButton buttonCopyROIs;
	
	JButton buttonLoadTSV;
	JButton buttonSaveTSV;
	
	JButton buttonMakeImage;
	JLabel emptyLabel;
	
	
	String title;
	String dir;
	String file;
	String export_extension = "tsv";
	String import_extension = "tsv";
	
	JButton buttonMeasure;
	JLabel noContent;
	
	int clicked_x;
	int clicked_y;
	
	public CellDetection_() {
		super("CellDetection ver.20180326");
		// TODO Auto-generated constructor stub
		
		imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.noImage();
			return;
		}
		
		
		
		ic = imp.getCanvas();		
		showPanel();
		this.setListener();

		this.overlayInitialize();

	}
	
	
	public void setListener(){
		ic.addMouseListener(this);
		ic.addMouseWheelListener(this);
		imp.getWindow().addWindowListener(this);
		
    	radioAutoDetect.addItemListener(this);    	
    	radioManual.addItemListener(this);    	
    	radioEdit.addItemListener(this);
    	colorBox.addItemListener(this);
    	buttonMeasure.addMouseListener(this);
    	buttonLoadTSV.addMouseListener(this);
    	buttonSaveTSV.addMouseListener(this);
    	buttonMakeImage.addMouseListener(this);
    	buttonCopyROIs.addMouseListener(this);

	}
	
	public void removeListener(){
		ic.removeMouseListener(this);
		ic.removeMouseWheelListener(this);
		imp.getWindow().removeWindowListener(this);
		//ImagePlus.removeImageListener(this);
		
    	radioAutoDetect.removeItemListener(this);    	
    	radioManual.removeItemListener(this);    	
    	radioEdit.removeItemListener(this);
    	colorBox.removeItemListener(this);
    	buttonMeasure.removeMouseListener(this);
    	buttonLoadTSV.removeMouseListener(this);
    	buttonSaveTSV.removeMouseListener(this);
    	buttonMakeImage.removeMouseListener(this);
    	buttonCopyROIs.removeMouseListener(this);
	}
	
	public void setOverlayArray(Overlay[] o){
		ol = o;
		imp.updateAndDraw();
	}
	
	public void overlayInitialize(){

		int stackSize = imp.getStackSize();
		

		ol = new Overlay[stackSize];
		for(int cs = 0; cs < stackSize; cs++){
			ol[cs] = new Overlay();
		}
		strokeColor = convertColor(colorBox.getSelectedItem().toString());
		currentOverlay = ol[imp.getCurrentSlice()-1];
		currentOverlay.drawLabels(true);
		currentOverlay.setStrokeColor(strokeColor);
		currentOverlay.setLabelColor(strokeColor);
		imp.setOverlay(currentOverlay);
		backupOl = ol.clone();
		saveOl = ol.clone();
	}
	
	
	public void showPanel(){
		
        FlowLayout gd_layout = new FlowLayout();
    	//gd_layout.setAlignOnBaseline(true);
    	gd_layout.setAlignment(FlowLayout.LEFT);
        
        JPanel gd_panel = new JPanel(gd_layout);
    	gd_panel.setPreferredSize(new Dimension(270, 450));
    	
    	JPanel mainPanel = new JPanel((new GridLayout(15,2)));
    	mainPanel.setAlignmentX(JPanel.BOTTOM_ALIGNMENT);
    	
    	intensityLimitLabel = new JLabel("Intensity Limit:");
    	intensityLimit = new JTextField(String.valueOf(imp.getProcessor().getMax()/4));

    	countLimitLabel = new JLabel("Count Limit:");    	
    	countLimit = new JTextField("1");
    	
    	radiusLabel = new JLabel("Radius");
    	radius = new JTextField("2");
    	
    	roughnessLabel = new JLabel("Roughness");
    	roughness = new JTextField("2");
    	
    	expansionLabel = new JLabel("Expansion");
    	expansion = new JTextField("0");
    	
    	
    	strokeWidthLabel = new JLabel("StrokeWidth");
    	strokeWidth = new JTextField("0");

    	radioGroup = new ButtonGroup();
    	radioAutoDetect = new JRadioButton("AutoDetect");
    	radioManual = new JRadioButton("Manual");
    	radioEdit = new JRadioButton("Edit");

    	
    	radioGroup.add(radioAutoDetect);
    	radioGroup.add(radioManual);
    	radioGroup.add(radioEdit);
    	radioAutoDetect.setSelected(true);
    	
    	checkOverlay = new JCheckBox("Overlay");
    	checkOverlay.setSelected(true);

    	checkSpline = new JCheckBox("Spline");
    	checkSpline.setSelected(true);
    	
    	colorBoxLabel = new JLabel("StrokeColor");
    	colorBox = new JComboBox<String>(colors);
    	
    	methodBox = new JComboBox<String>(methods);

    	buttonMeasure = new JButton("Measure");
    	//noContent = new JLabel("");
    	checkAutoForwardT = new JCheckBox("AutoForward-T");
    	checkAutoForwardT.setSelected(false);

    	buttonCopyROIs = new JButton("Copy ROIs");
    	copyBox = new JComboBox<String>(target);
    	
    	buttonLoadTSV = new JButton("Load TSV");
    	buttonSaveTSV = new JButton("Save TSV");

    	
    	buttonMakeImage = new JButton("Make Image");
    	emptyLabel = new JLabel("");
    	
    	mainPanel.add(intensityLimitLabel);
    	mainPanel.add(intensityLimit);

    	mainPanel.add(countLimitLabel);
    	mainPanel.add(countLimit);
    	
    	mainPanel.add(radiusLabel);
    	mainPanel.add(radius);
    	
    	mainPanel.add(expansionLabel);
    	mainPanel.add(expansion);
    	
    	mainPanel.add(roughnessLabel);
    	mainPanel.add(roughness);
    	
    	mainPanel.add(strokeWidthLabel);
    	mainPanel.add(strokeWidth);

    	mainPanel.add(colorBoxLabel);
    	mainPanel.add(colorBox);
    	
    	mainPanel.add(radioAutoDetect);
    	mainPanel.add(methodBox);
    	
    	mainPanel.add(radioManual);
    	mainPanel.add(radioEdit);

    	mainPanel.add(checkSpline);
    	mainPanel.add(checkAutoForwardT);

    	mainPanel.add(checkOverlay);
    	mainPanel.add(emptyLabel);

    	mainPanel.add(new JSeparator());
    	mainPanel.add(new JSeparator());
    	
    	mainPanel.add(copyBox);
    	mainPanel.add(buttonCopyROIs);
    	
    	mainPanel.add(buttonLoadTSV);    	
    	mainPanel.add(buttonSaveTSV);

    	mainPanel.add(buttonMakeImage);
    	mainPanel.add(buttonMeasure);
    	
    	gd_panel.add(mainPanel);
    	
    	this.add(gd_panel);
		this.pack(); //推奨サイズのｗindow
		
		Point imp_point = imp.getWindow().getLocation();
		int imp_window_width = imp.getWindow().getWidth();
		//int imp_window_height = imp.getWindow().getHeight();

		double set_x_point = imp_point.getX() + imp_window_width;
		double set_y_point = imp_point.getY();
		
		this.setLocation((int)set_x_point, (int)set_y_point);

		this.setVisible(true);//thisの表示

    	
	}	

	
	public boolean showSaveDialog(){
    	title = imp.getTitle();
    	dir = imp.getOriginalFileInfo().directory;
    	
    	SaveDialog sd = new SaveDialog("Export tsv file", dir, title, "." + export_extension);
    	
    	if (sd.getDirectory() == null || sd.getFileName() == null) {
    		return false;
    	}
    	dir = sd.getDirectory(); //save dialog　中に選択したものに変更。
    	file = sd.getFileName(); //save dialog　中に選択したものに変更。
    	return true;
		
	}

	public boolean showLoadDialog(){
    	title = imp.getTitle();
    	dir = imp.getOriginalFileInfo().directory;
    	
    	OpenDialog od = new OpenDialog("Import tsv file", dir, title + import_extension);
    	
    	if (od.getDirectory() == null || od.getFileName() == null) {
    		return false;
    	}
    	dir = od.getDirectory(); //save dialog　中に選択したものに変更。
    	file = od.getFileName(); //save dialog　中に選択したものに変更。
    	return true;		
	}
	
	
	public boolean detectCell(){


		boolean b = false;
		int cs = imp.getCurrentSlice();
		
		double intLimit = Double.valueOf(intensityLimit.getText());
		int cntLimit = Integer.valueOf(countLimit.getText());
		int radiusNum = Integer.valueOf(radius.getText());
		int expansionNum = Integer.valueOf(expansion.getText());
		int roughnessNum = Integer.valueOf(roughness.getText());
		int strokeWidthNum = Integer.valueOf(strokeWidth.getText());
		Color strokeC = convertColor(colorBox.getSelectedItem().toString());
		
		CellDetector cd = new CellDetector();
		cd.setImage(imp);
		cd.setStartPosition(clicked_x, clicked_y);
		cd.setCountLimit(cntLimit);
		cd.setIntensityLimit(intLimit);
		cd.setRadius(radiusNum);
		cd.setExpansion(expansionNum);
		cd.setRoughness(roughnessNum);
		cd.setStrokeWidth(strokeWidthNum);
		cd.setStrokeColor(strokeC);
		
		if(checkSpline.isSelected()){
			cd.setFitSpline(true);
		}else{
			cd.setFitSpline(false);
		}
		
		if(cd.detectNegativePosition()){
			//cd.drawNegativeRegion();
			Roi roi = cd.detectRoi();
			if(roi != null){
				imp.setRoi(roi);
				
				if(checkOverlay.isSelected()){
					Overlay overlay = imp.getOverlay();//RoiManegerから返ってきたとき対策
					if(overlay == null){

						
						currentOverlay = backupOl[cs-1].duplicate();
						currentOverlay.drawLabels(true);
						imp.setOverlay(currentOverlay);
						
						overlay = imp.getOverlay();
					}
					overlay.drawLabels(true);
					overlay.setStrokeColor(strokeC);
					overlay.add(roi);
					b = true;
	
				}
			}
		}
		return b;
	}
	
	public void makeImage(){ //何を作ろうとしていたのか。ROIを書き込んだ画像を作る？ 多分途中

		ImageStack buff_stack = new ImageStack(imp.getWidth(),imp.getHeight());
		for(int i = 0; i < imp.getStackSize(); i++){
			int index = i + 1;
			imp.setOverlay(null);
			ImageProcessor ip = imp.getStack().getProcessor(index).duplicate();
			ip = ip.convertToRGB();

			for(int n = 0; n < ol[i].size(); n++){
				ip.setColor(ol[i].get(n).getStrokeColor());
				ip.draw(ol[i].get(n));
			}
			buff_stack.addSlice(ip);
		}
		ImagePlus resultImp = new ImagePlus();
		resultImp.setStack("drawROIs_" + imp.getOriginalFileInfo().fileName, buff_stack);
		
		resultImp.show();

	}
	
	public void copyROIs(){
		int c = imp.getC();
		int z = imp.getZ();
		int t = imp.getT();
		
		int selectedIndexCopyBox = copyBox.getSelectedIndex();

		switch(selectedIndexCopyBox){
			case 0 :
				if(t > 1) t = t -1;
				break;
			case 1 :
				if(z > 1) z = z -1;
				break;
			case 2 :
				if(t < imp.getNFrames()) t = t +1;
				break;
			case 3 :
				if(z < imp.getNSlices()) z = z +1;
		}
	
		int index = imp.getStackIndex(c, z, t);

		
		Overlay o = ol[index -1].duplicate();
		currentOverlay.clear();
		for(int i = 0; i < o.size(); i++){
			currentOverlay.add(o.get(i));
		}
		imp.updateAndDraw();
	}
	
	
	public boolean addOverlay(){
		boolean b = false;
		Roi r = imp.getRoi();
		int roiTypeID = 0;
		if(r != null){
		  roiTypeID = r.getType();
		}

		if(!currentOverlay.contains(r)) {
			if ((r != null) && (ic.getCursor().getType() != Cursor.HAND_CURSOR)) {
				//System.out.println(r.getTypeAsString());
				if (r.getState() != 0) { //0 == contructing, 3 == nomal
					if ((roiTypeID == 2) || (roiTypeID == 6)) {
						PolygonRoi pr;
						pr = (PolygonRoi) r;
						if (checkSpline.isSelected()) {
							pr.fitSpline();
						}
						currentOverlay.add(pr);
					} else {
						currentOverlay.add(r);
					}
					currentOverlay.setStrokeColor(strokeColor);
					b = true;
				}
			}
		}
		return b;
	}
	
	public Color convertColor(String s){
		Color c = Color.WHITE;
		
		if(s == "White"){
			c = Color.WHITE;
		}else if(s == "Black"){
			c = Color.BLACK;
		}else if(s == "Red"){
			c = Color.RED;
		}else if(s == "Green"){
			c = Color.GREEN;
		}else if(s == "Blue"){
			c = Color.BLUE;
		}else if(s == "Yellow"){
			c = Color.YELLOW;
		}else if(s == "Cyan"){
			c = Color.CYAN;
		}else if(s == "Magenta"){
			c = Color.MAGENTA;
		}

		return c;
	}
	
	public void forwardT(){
		if(checkAutoForwardT.isSelected()){
			int ct = imp.getT();
			imp.setT(ct + 1);
			imp.killRoi();
		}
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getSource() == ic){
			if(radioAutoDetect.isSelected()){
				if((ic.getCursor().getType() != Cursor.HAND_CURSOR)){
					IJ.showStatus("Now Processing..."); //これを表示したいがための苦肉の策// sort付近で記述しても処理後に反映されてしまう。なぜ？

				}
			}else if(radioManual.isSelected()){

			}
		
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {

		if(e.getSource() == ic){
			if(radioAutoDetect.isSelected()){
				if((ic.getCursor().getType() != Cursor.HAND_CURSOR)){
					clicked_x = ic.offScreenX(e.getX());
					clicked_y = ic.offScreenY(e.getY());
					if(detectCell()){
						forwardT();
					}
					IJ.showStatus("Done");
				}
			}else if(radioManual.isSelected()){
				if(addOverlay()){
					forwardT();
				}
			}
		
		}else if(e.getSource() == buttonSaveTSV){
				
			if(showSaveDialog()){
				
				String fileName = file;
				RoiTsvIO rtio = new RoiTsvIO(imp, dir, fileName);
				Overlay[] saveOl = new Overlay[ol.length];//spline設定が切れちゃうため
				for(int i = 0; i < ol.length; i++){
					saveOl[i] = ol[i].duplicate();
				}
				rtio.saveOverlayTSV(saveOl);
				
			}else{
				return;
			}

		}else if(e.getSource() == buttonLoadTSV){
			if(showLoadDialog()){
				String fileName = file;
				RoiTsvIO rtio = new RoiTsvIO(imp, dir, fileName);		
				setOverlayArray(rtio.loadOverlayTSV());
			}else{
				return;
			}
		}else if(e.getSource() == buttonMeasure){

			if(imp.getID() == WindowManager.getCurrentImage().getID()){
				RoiManagerConverter rmc = new RoiManagerConverter(imp,ol);
				rmc.directMeasure();
			}
			
		}else if(e.getSource() == buttonMakeImage){
			makeImage();
			
		}else if(e.getSource() == buttonCopyROIs){
			int selectedIndexCopyBox = copyBox.getSelectedIndex();
			if((selectedIndexCopyBox == 0)&&(imp.getNFrames() == 1)){
				IJ.showMessage("No other T slice");
			}else if((selectedIndexCopyBox == 2)&&(imp.getNFrames() == 1)){
				IJ.showMessage("No other T slice");
			}else if((selectedIndexCopyBox == 1)&&(imp.getNSlices() == 1)){
				IJ.showMessage("No other Z slice");
			}else if((selectedIndexCopyBox == 3)&&(imp.getNSlices() == 1)){
				IJ.showMessage("No other Z slice");
			}else{
				copyROIs();
			}
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if(e.getSource() == ic){
			ImagePlus.addImageListener(this);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if(e.getSource() == ic){
			ImagePlus.removeImageListener(this);
		}
	}

	@Override
	public void imageOpened(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imageUpdated(ImagePlus imp) {

		int cs = imp.getCurrentSlice();

		currentOverlay = ol[cs -1];
		currentOverlay.drawLabels(true);
		imp.setOverlay(currentOverlay);
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		imp.killRoi();
		if(e.getSource() == ic){
			int count = e.getWheelRotation();
			imp.setT(imp.getT() + count);
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		removeListener();
		imp.setOverlay(null);
		this.close();
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent e){
		if(this.isVisible()){
			changeSplineCheck();
		}
	}


	@Override
	public void itemStateChanged(ItemEvent e) {
		//System.out.println(e.getSource());
		if(e.getSource() == colorBox){
			strokeColor = convertColor(colorBox.getSelectedItem().toString());
			currentOverlay.setStrokeColor(strokeColor);	
			currentOverlay.setLabelColor(strokeColor);
			imp.updateAndDraw();
		}else if((e.getSource() == radioAutoDetect)||(e.getSource() == radioManual)||(e.getSource() == radioEdit)){
			if(this.isVisible()){
				changeSplineCheck();
			}
		}
	}

	
	
	
	
	
	public void changeSplineCheck(){
		
		if((radioManual.isSelected())||(radioEdit.isSelected())){
			String toolName = IJ.getToolName();
			if((toolName.equals("polygon"))||(toolName.equals("polyline"))){
				checkSpline.setEnabled(true);
			}else{
				checkSpline.setEnabled(false);
			}
		}else if(radioAutoDetect.isSelected()){
			checkSpline.setEnabled(true);
		}
	}
	
}