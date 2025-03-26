package hw.celldetection;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.util.Arrays;

public class RoiManagerConverter{
		
	ImagePlus imp;
	Overlay[] overlay;
	RoiManager rm;
	
	public RoiManagerConverter(ImagePlus i, Overlay o[]){
		overlay = o;
		imp = i;
	}
	
	public void convertRoiManager(){
		this.visibleRoiManager();
		
		for(int n = 0; n < imp.getStackSize(); n++){
			Overlay o = overlay[n];
			if(o.size() > 0){
				//imp.setSlice(n+1);
				Roi[] r = o.toArray();
				
				for(int i = 0; i < r.length; i++){
					
					rm.add(imp, r[i], i);
				}
			}
		}

	}
	
	
	public boolean visibleRoiManager(){
		boolean b = false;
		
		rm = RoiManager.getRoiManager();
		if(rm == null){
			rm = new RoiManager();
		}
		b = true;
		return b;
		rm.
	}
	
	public void directMeasure(){

		int currentSlice = imp.getCurrentSlice();
		
		for(int n = 0; n < imp.getStackSize(); n++){
			Overlay o = overlay[n];
			if(o.size() > 0){
				imp.setSlice(n+1);
				Roi[] r = o.toArray();
				for(int i = 0; i < r.length; i++){
					String label = r[i].getName();
					//if(label == null){ 一度計測後にRoiを増やすとその値が反映されなくなる。とは言え、これではユーザー主体のlabel名を反映できない
						label = "s:" + (n+1) + "/" + imp.getStackSize() + ";";
						label = label + "r:" + (i+1) + "/" + r.length;
						r[i].setName(label);
					//}

					imp.setRoi(r[i]);
					IJ.run("Measure");
				}
			}
		}
		
		
		/*20160823　インターン用の急遽変更分。これではすべてのROIを測ることとが出来ない。1枚目のROIの数で決め打ちしているため。
		int roiNum = overlay[0].size();
		for(int i = 0; i < roiNum; i++){
			for(int n = 0; n < imp.getStackSize(); n++){
				imp.setSlice(n+1);
				Roi r = overlay[n].get(i);
				String label = r.getName();
				label = "s:" + (n+1) + "/" + imp.getStackSize() + ";";
				label = label + "r:" + (i+1) + "/" + roiNum;
				r.setName(label);
				imp.setRoi(r);
				IJ.run("Measure");
			}
			
		}
		*/
		
		
		imp.killRoi();
		imp.setSlice(currentSlice);
		imp.updateAndDraw();
	}
	
}