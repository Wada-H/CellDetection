package hw.celldetection;

import java.awt.Polygon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class RoiTsvIO{

	String path;
	String fileName;
	
	String title;
	String original_dir;
	String original_fileName;
	
	ImagePlus imp;
	
	int nC;
	int nZ;
	int nT;
	int stackSize;
	

	public RoiTsvIO(ImagePlus im, String file_path, String file_name){

		path = file_path;
		fileName = file_name;
		setInfo(im);
	}
	
	
	public void setInfo(ImagePlus impm){
		imp = impm;
    	nC = impm.getNChannels();
    	nZ = impm.getNSlices();
    	nT = impm.getNFrames();
    	stackSize = impm.getStackSize();
    	
    	original_dir = impm.getOriginalFileInfo().directory;
    	original_fileName = impm.getOriginalFileInfo().fileName;
    	title = impm.getTitle();
	}
	
	/*RECTANGLE=0, OVAL=1, POLYGON=2, FREEROI=3, TRACED_ROI=4, LINE=5, 
	POLYLINE=6, FREELINE=7, ANGLE=8, COMPOSITE=9, POINT=10; */// Types
	public boolean saveOverlayTSV(Overlay[][][] overlayArray){ //Overlay[c][z][t]
		Overlay[] overlay = new Overlay[imp.getStackSize()];
		for(int cc = 0; cc < nC; cc++){
			for(int ct = 0; ct < nT; ct++){
				for(int cz = 0; cz < nZ; cz++){
					int index = imp.getStackIndex(cc+1, cz+1, ct+1);
					overlay[index-1] = overlayArray[cc-1][cz-1][ct-1];
				}
			}
		}
		
		return saveOverlayTSV(overlay);
	}
	
	
	public boolean saveOverlayTSV(Overlay[] overlayArray){ //Overlay[stack size]
    	File f = new File(path + fileName);
    	FileWriter filewriter = null;
		try {
			filewriter = new FileWriter(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
    	BufferedWriter bw = new BufferedWriter(filewriter);    	
    	PrintWriter pw = new PrintWriter(bw);
		

		String header = "C\tZ\tT\tRoiNum\tType\tSpline\tPointNum\t<x,y>\t*when oval roi this parameter become the centroid, width, height";
		pw.println(header);
		for(int cc = 0; cc < nC; cc++){
			for(int ct = 0; ct < nT; ct++){
				for(int cz = 0; cz < nZ; cz++){
					int index = imp.getStackIndex(cc+1, cz+1, ct+1); 
					if(overlayArray[index -1] != null){
						Roi[] buffRoiArray = overlayArray[index -1].toArray();

						for(int i = 0; i < buffRoiArray.length; i++){
							Roi r = buffRoiArray[i];
							int typeID = r.getType();
							String typeS = r.getTypeAsString();
							int pointNum = 0;

							
							List<Integer> points_x = new ArrayList<Integer>();
							List<Integer> points_y = new ArrayList<Integer>();
							
							boolean fitSpline = false;

							
							if(typeID == 1){
								pointNum = 0;
							}else if(typeID == 10){
								pointNum = 1;
							}else{

								if((typeID == 2)||(typeID == 6)){
									PolygonRoi p = (PolygonRoi)buffRoiArray[i];
									
									//Polygon pp = buffRoiArray[i].getPolygon();
									fitSpline = p.isSplineFit();
									p.removeSplineFit();
									pointNum = p.getNCoordinates();
									System.out.println(p.getXBase());
									int ox = (int)Math.round(p.getXBase());// + 1; //苦肉の策・・・なぜか1pixelずれるため->doubleからのキャスト問題か？とりあえず四捨五入で回避できそう
									int oy = (int)Math.round(p.getYBase());// + 1; //苦肉の策
									
									int[] xps = p.getXCoordinates();
									int[] yps = p.getYCoordinates();
									//int[] xps = pp.xpoints;
									//int[] yps = pp.ypoints;
									
									for(int n = 0; n < pointNum; n++){
										points_x.add(xps[n] + ox);
										points_y.add(yps[n] + oy);
										//points_x.add(xps[n]);
										//points_y.add(yps[n]);
									}
								}else{
								
									Polygon p = r.getPolygon();
									pointNum = p.npoints;
									int[] xps = p.xpoints;
									int[] yps = p.ypoints;
									for(int n = 0; n < pointNum; n++){
										points_x.add(xps[n]);
										points_y.add(yps[n]);
									}
								}
							}
							
							
							pw.print(cc+1 + "\t");
							pw.print(cz+1 + "\t");
							pw.print(ct+1 + "\t");
							pw.print(i+1 + "\t");
							pw.print(typeS + "\t");
							pw.print(fitSpline + "\t");
							pw.print(pointNum);

							if((typeID == 0)||(typeID == 2)||(typeID == 3)||(typeID == 5)||(typeID == 6)||(typeID == 7)||(typeID == 8)){
								for(int n = 0; n < pointNum; n++){
									pw.print("\t" + points_x.get(n) + "\t" + points_y.get(n));
								}
								pw.print("\n");
							}else if(typeID == 1){
								//double[] centroid = r.getContourCentroid();
								double ox = r.getXBase();
								double oy = r.getYBase();
								int width = (int)r.getFloatWidth();
								int height = (int)r.getFloatHeight();
								pw.println("\t" + ox + "\t" + oy + "\t" + width + "\t" + height);
							//}else if((typeID == 8)||(typeID == 6)){
								
								
								
							}else if(typeID == 10){
								int x_p = (int)r.getXBase();
								int y_p = (int)r.getYBase();
								pw.println("\t" + x_p + "\t" + y_p);
							}else{
								pw.print("\n");
							}
	
						}
					}
				}
			}
		}
		pw.close();
		return true;
	}
	
	public Overlay[] loadOverlayTSV(){
		Overlay[] resultArray = null;
		ArrayList<String> stringList = new ArrayList<String>();
		try (Stream<String> lineString_s = Files.lines(Paths.get(path,fileName))){
			lineString_s.skip(1).forEach(stringList::add);

		}catch (IOException e) {
		    e.printStackTrace();
		}
		
		resultArray = getOverlay(stringList);
		
		return resultArray;
	}
	
	
	public Overlay[] getOverlay(ArrayList<String> as){
		Overlay[] overlayArray = new Overlay[stackSize];
		for(int i = 0; i < stackSize; i++){
			overlayArray[i] = new Overlay();
			overlayArray[i].drawLabels(true);
		}
		
		for(String line:as){
			String[] splitS = line.split("\t");
			int c = Integer.valueOf(splitS[0]);
			int z = Integer.valueOf(splitS[1]);
			int t = Integer.valueOf(splitS[2]);
			String roiType = splitS[4];
			String spline = splitS[5];
			int polygonNum = Integer.valueOf(splitS[6]);
			int index = imp.getStackIndex(c, z, t);
			System.out.println(index);
			if(roiType.equals("Oval")){
				double x = Double.valueOf(splitS[7]);
				double y = Double.valueOf(splitS[8]);
				double width = Double.valueOf(splitS[9]);
				double height = Double.valueOf(splitS[10]);
				Roi r = new OvalRoi(x, y, width, height);
				overlayArray[index -1].add(r);
			}else if(roiType.equals("Straight Line")){
				double sx = Double.valueOf(splitS[7]);
				double sy = Double.valueOf(splitS[8]);
				double ex = Double.valueOf(splitS[9]);
				double ey = Double.valueOf(splitS[10]);
				Roi r = new Line(sx, sy, ex, ey);
				overlayArray[index -1].add(r);

			}else if(roiType.equals("Point")){
				double x = Double.valueOf(splitS[7]);
				double y = Double.valueOf(splitS[8]);
				Roi r = new PointRoi(x, y);
				overlayArray[index -1].add(r);
			}else{
				int typeID = Roi.POLYGON;
				float[] x = new float[polygonNum];
				float[] y = new float[polygonNum];
				
				switch(roiType){
					case("Polyline"):typeID = Roi.POLYLINE;break;
					case("Freeline"):typeID = Roi.FREELINE;break;
					case("Angle"):typeID = Roi.ANGLE;break;
					case("Freehand"):typeID = Roi.FREEROI;break;
				}
	
	
				for(int n = 0; n < polygonNum; n++){
					int xp = ((2 * n) + 7);
					int yp = xp + 1;
					x[n] = Integer.valueOf(splitS[xp]);
					y[n] = Integer.valueOf(splitS[yp]);
				}
				PolygonRoi r = new PolygonRoi(x, y, typeID);

				if(roiType.equals("Rectangle")){
					double ox = r.getXBase();
					double oy = r.getYBase();
					double xl = r.getFloatWidth();
					double yl = r.getFloatHeight();
					Roi rr = new Roi(ox, oy, xl, yl);
					overlayArray[index -1].add(rr);

				}else{
					if(spline.equals("true")){
						r.fitSpline();
					}
					overlayArray[index -1].add(r);
				}
			}
		}
		
		return overlayArray;
	
	}
}