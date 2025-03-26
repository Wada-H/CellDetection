package hw.celldetection;

//Edit>Select>Enlargeでできる。

import java.awt.Polygon;
//import java.util.ArrayList;
import java.util.stream.IntStream;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;

public class RoiResizer{
	Roi roi;
	int reduceSize = 1;
	
	public RoiResizer(){
	}
	
	
	public void setRoi(Roi r){
		roi = r;
	}
	
	public void setReducePolygonPoint(int n){
		reduceSize = n;
	}
	
	/*
	public Roi resize(int n){
		double[] centroid = roi.getContourCentroid();
		
		System.out.println("centroid:" + centroid[0] + "," + centroid[1]);
		float[] xpoints = roi.getFloatPolygon().xpoints;
		float[] ypoints = roi.getFloatPolygon().ypoints;
		
		int vertexNum = xpoints.length;
		
		float[] new_xpoints = new float[vertexNum];
		float[] new_ypoints = new float[vertexNum];

		IntStream i_stream = IntStream.range(0, vertexNum);
		i_stream.parallel().forEach(i -> {
			double x = xpoints[i] - centroid[0];
			double y = ypoints[i] - centroid[1];
			
			new_xpoints[i] = xpoints[i];
			if(x < 0){
				new_xpoints[i] = xpoints[i] - n;
			}else if(x > 0){
				new_xpoints[i] = xpoints[i] + n;
			}
			
			new_ypoints[i] = ypoints[i];
			if(y < 0){
				new_ypoints[i] = ypoints[i] - n;
			}else if(y > 0){
				new_ypoints[i] = ypoints[i] + n;
			}
			
		});
		
		PolygonRoi newRoi = new PolygonRoi(new_xpoints, new_ypoints, Roi.FREEROI);
		return newRoi;
	}
	*/
	
	public Roi resize(float n){
		
		double[] centroid_oimg = roi.getContourCentroid();

		float[] xpoints = roi.getFloatPolygon().xpoints;
		float[] ypoints = roi.getFloatPolygon().ypoints;
		
		int vertexNum = xpoints.length;

		
		float[] new_xpoints = new float[vertexNum];
		float[] new_ypoints = new float[vertexNum];
		

		IntStream i_stream = IntStream.range(0, vertexNum);
		i_stream.parallel().forEach(i -> {
			new_xpoints[i] = (xpoints[i] * n);
			new_ypoints[i] = (ypoints[i] * n);
			
		});
		
		PolygonRoi newRoi = new PolygonRoi(new_xpoints, new_ypoints, Roi.FREEROI);
		double[] centroid_nimg = newRoi.getContourCentroid();

		double[] difference_of_centroid = {(centroid_nimg[0]-centroid_oimg[0]), (centroid_nimg[1]-centroid_oimg[1])};

		Roi movedRoi = moveRoi(newRoi, difference_of_centroid);
		
		return movedRoi;
	}
	
	
	public Roi moveRoi(Roi r, double[] difference){
		float[] xpoints = r.getFloatPolygon().xpoints;
		float[] ypoints = r.getFloatPolygon().ypoints;
		
		int vertexNum = xpoints.length;
		float[] new_xpoints = new float[vertexNum];
		float[] new_ypoints = new float[vertexNum];
		
		IntStream i_stream = IntStream.range(0, vertexNum);
		i_stream.parallel().forEach(i -> {
			new_xpoints[i] = (xpoints[i] - (float)difference[0]);
			new_ypoints[i] = (ypoints[i] - (float)difference[1]);
			
		});
		
		PolygonRoi newRoi = new PolygonRoi(new_xpoints, new_ypoints, Roi.FREEROI);
		return newRoi;
		
	}
	
	
	public Roi resize(Roi r, float n){
		roi = r;
		Roi newRoi = resize(n);

		
		return newRoi;
	}
	
	
	public Roi enlarge(double pixels){
		Roi newRoi = RoiEnlarger.enlarge(roi, pixels);
		return newRoi;
	}
	
	
	
	public Roi enlarge(Roi r, double pixels){
		Roi newRoi = RoiEnlarger.enlarge(r, pixels);
		
		if(r.getType() == Roi.POLYGON){
			PolygonRoi newPRoi = new PolygonRoi(newRoi.getFloatPolygon(), Roi.POLYGON);
			newRoi = newPRoi;
			
			if(reduceSize > 1){
				newRoi = reducePolygonPoint(newRoi);
			}
		}
		
		return newRoi;
	}
	
	public float[][] getEnlargePoints(Roi r, double pixels){
		Roi newRoi = RoiEnlarger.enlarge(r, pixels);
		Polygon poly = newRoi.getPolygon();
		int[][] buff = new int[2][poly.npoints];
		float[][] result = new float[poly.npoints][2];
		buff[0] = poly.xpoints;
		buff[1] = poly.ypoints;
		
		for(int i = 0; i < buff[0].length; i++){
			result[i][0] = (float)buff[0][i];
			result[i][1] = (float)buff[1][i];
		}
		
		return result;
	}
	
	public int[][] getEnlargeIntPoints(Roi r, double pixels){// [2][npoints]
		Roi newRoi = RoiEnlarger.enlarge(r, pixels);
		Polygon poly = newRoi.getPolygon();
		int[] buff_x = new int[poly.npoints];
		int[] buff_y = new int[poly.npoints];
		//ArrayList<Integer> buff_x_list = new ArrayList<Integer>();
		//ArrayList<Integer> buff_y_list = new ArrayList<Integer>();

		buff_x = poly.xpoints;
		buff_y = poly.ypoints;

		/*外周の内側にある点については除いておきたいのだが、、、
		for(int i = 0; i < poly.npoints; i++){
			if(poly.contains(buff_x[i], buff_y[i])){
				buff_x_list.add(buff_x[i]);
				buff_y_list.add(buff_y[i]);
			}
		}
		*/
		int[][] result = new int[2][];

		//result[0] = toPrimitiveInt((Integer[])buff_x_list.toArray(new Integer[0]));
		//result[1] = toPrimitiveInt((Integer[])buff_y_list.toArray(new Integer[0]));
		result[0] = buff_x;
		result[1] = buff_y;
		return result;
	}
	
	public int[] toPrimitiveInt(Integer[] ic){
		int[] result = new int[ic.length];
		for(int i = 0; i < ic.length; i++){
			result[i] = ic[i];
		}
		return result;
	}
	
	public Roi reducePolygonPoint(Roi r){
		Polygon p = r.getPolygon();
		int size  = p.npoints / reduceSize;
		float[] xpoint = new float[size];
		float[] ypoint = new float[size];
		
		for(int i = 0; i < size; i++){
			int index = i * reduceSize;
			xpoint[i] = p.xpoints[index];
			ypoint[i] = p.ypoints[index];
		}
		PolygonRoi newPRoi = new PolygonRoi(xpoint, ypoint, r.getType());
		return newPRoi;
	}
	
}