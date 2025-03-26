package hw.celldetection;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.IntStream;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class CellDetector{
	
	ImagePlus imp;
	int[] startPosition = {0,0};
	int[] breakPosition = {-1,-1};
	
	
	int width;
	int height;
	int[][] negativePosition;
	
	double limitIntensity;
	int limitNum = 1;
	int radius = 2;
	int expansion = 0;
	int roughness = 1;
	
	boolean fitSpline = true;
	
	int strokeWidth = 0;
	Color strokeColor = Color.WHITE;
	
	ArrayList<String> margin_fill_types = new ArrayList<String>(Arrays.asList("Zero","Reflect","Repeat"));
	String fill_type = "Zero";
	
	int momo = 0;

	
	public CellDetector(){

	}
	
	public void setImage(ImagePlus img){
		imp = img;
		width = imp.getWidth();
		height = imp.getHeight();
		negativePosition = new int[width][height];
		limitIntensity = imp.getProcessor().getMax();

		for(int x = 0; x < width; x++){ //negativePositionの初期値
			for(int y = 0; y < height; y++){
				negativePosition[x][y] = 0; 
			}
		}
	}
	
	public void setStartPosition(int x, int y){
		startPosition[0] = x;
		startPosition[1] = y;
	}
	
	public void setIntensityLimit(double value){
		limitIntensity = value;
	}
	
	public void setRadius(int r){
		radius = r;
	}
	
	public void setCountLimit(int value){
		limitNum = value;
	}
	
	public void setRoughness(int value){
		roughness = value;
	}
	
	public void setFitSpline(boolean b){
		fitSpline = b;
	}

	public void setStrokeWidth(int n){
		strokeWidth = n;
	}
	
	public void setStrokeColor(Color c){
		strokeColor = c;
	}
	
	public void setExpansion(int n){
		expansion = n;
	}
	
	// 肝部分 // negative -1, positive 1;
	LinkedList<int[]> branchingPoint;

	public boolean detectNegativePosition(){
		boolean b = false;
		branchingPoint = new LinkedList<int[]>();
		b = checkAround3(startPosition[0], startPosition[1]);

		while(b == false){
			
			int x_p = breakPosition[0];
			int y_p = breakPosition[1];

			//System.out.println("break:" + x_p + "," + y_p);

			b = checkAround3(x_p, y_p);
			if(b == true){

				int[] bp = branchingPoint.pollLast();
				if(bp != null){
					b = false;
					breakPosition[0] = bp[0];
					breakPosition[1] = bp[1];
				}
			}


		}
		
		//b = true; //このtrueが帰らないと次の処理にいかない
		return b;
	}
	

	
	public boolean checkAround3(int x, int y){
		boolean b = false;

		if((x > 0)&&(x < width-1)&&(y > 0)&&(y < height-1) == true){

			int checkX = x;
			int checkY = y;
			int[] bp = {x,y};
			
			branchingPoint.add(bp);
			if(checkA(x, y, 0)){
				if(checkA(x, y, 1)){
					if(checkA(x, y, 2)){
						if(checkA(x, y, 3)){
							branchingPoint.pollLast();
							b = true;
						}else{
							checkX = x - 1;
							checkY = y;
						}
					}else{
						checkX = x;
						checkY = y + 1;	
					}
				}else{
					checkX = x + 1;
					checkY = y;
				}
				
			}else{
				checkX = x;
				checkY = y - 1;				
			}
			
			
			breakPosition[0] = checkX;
			breakPosition[1] = checkY;
		}else{
			b = true;
		}
		
		return b;
	}
	
	public boolean checkA(int x, int y, int d){ //d = 0,1,2,3 = up, right, down, left
		boolean b = false;
		int checkX = x;
		int checkY = y;
		if(d == 0){
			checkY = y - 1;
		}else if(d == 1){
			checkX = x + 1;
		}else if(d == 2){
			checkY = y + 1;
		}else if(d == 3){
			checkX = x - 1;
		}
		
		if(negativePosition[checkX][checkY] == 0){
			ImageProcessor ip = imp.getProcessor();
			ArrayList<Integer> aroundArray = this.getAround(ip, checkX, checkY);
			int count = 0;
			for(int i = 0; i < aroundArray.size(); i++){
				if(aroundArray.get(i) >= limitIntensity){
					count = count + 1;
				}
			}
			negativePosition[checkX][checkY] = -1;
	
			if(count > limitNum){
				negativePosition[checkX][checkY] = 1;
				b = true;
			}
		}else{
			b = true;
		}
		return b;
	}

	
	public ArrayList<Integer> getAround(ImageProcessor ip, int x, int y){
		ArrayList<Integer> int_array = new ArrayList<Integer>();
		
		int start_position_x = x - radius;
		int start_position_y = y - radius;
		int end_position_x = x + radius;
		int end_position_y = y + radius;
		
		for(int cx = start_position_x; cx < end_position_x + 1; cx++){
			for(int cy = start_position_y; cy < end_position_y + 1; cy++){
				
				if((cx == x)&&(cy == y)){
					//とばす
				}else{
					int value = 0;
					if((cx < 0) | (cy < 0) | (cx > width) | (cy > height)){
						value = this.getMarginValue(ip, cx, cy);
					}else{
						value = ip.getPixel(cx, cy);
					}
					int_array.add(value);
				}
			}
			
		}

		
		return int_array;
	}
	
	
	public void drawNegativeRegion(){
		
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				int check = negativePosition[x][y];
				if(check == -1){
					imp.getProcessor().set(x, y, (int)imp.getProcessor().getMax());
				}
			}
		}
	}
	
	public Roi detectRoi(){
		ArrayList<Float> positivePosition_x = new ArrayList<Float>();
		ArrayList<Float> positivePosition_y = new ArrayList<Float>();
		PolygonRoi resultRoi;
		
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				int check = negativePosition[x][y];
				if(check == 1){
					positivePosition_x.add((float)x);
					positivePosition_y.add((float)y);
				}
			}
		}
		
		int detectedSize = positivePosition_x.size();
		float[][] ppxy_f = new float[detectedSize][2];
		System.out.println("detectedSize :" + detectedSize);

		
		if(detectedSize > 5000){//大きすぎるサイズでsortに時間がかかりすぎるため
			IJ.showMessage("Over 5000pt");
			resultRoi = null;
			return resultRoi;
		}
		

		for(int i = 0; i < positivePosition_x.size(); i++){
			ppxy_f[i][0] = positivePosition_x.get(i);
			ppxy_f[i][1] = positivePosition_y.get(i);
			//System.out.println("ppxy:" + ppxy_f[i][0] + "," + ppxy_f[i][1]);

		}
		
		float[][] sorted_xy = sortCurtateDistanceC(ppxy_f, 2, 5);//minLimit, maxLimit of length
		
		for(int i = 0; i < sorted_xy[0].length; i++){

			//System.out.println("sortedxy:" + sorted_xy[0][i] + "," + sorted_xy[1][i]);
		}
		
		
		//System.out.println("sorted_xy[0].length:" + sorted_xy[0].length);
		resultRoi = new PolygonRoi(sorted_xy[0], sorted_xy[1], Roi.POLYGON);//Roiのtypeは変更できるようにするか
		//System.out.println("n:" + resultRoi.getNCoordinates());
		RoiResizer roiR = new RoiResizer();
		roiR.setReducePolygonPoint(roughness);
		
		if(expansion > 0){
			if(detectedSize > 4){
	

				//resultRoi = roiR.resize(resultRoi, 1.2f);// 1.05倍の座標に変換して重心を元の位置に戻す
				//resultRoi = (PolygonRoi)roiR.enlarge(resultRoi,(radius + expansion));// radius + expansion 分拡張したROIにする。
				int[][] ex_npoints = roiR.getEnlargeIntPoints(resultRoi, (radius + (expansion -1)));
				
				//System.out.println("ex_npoints:" + ex_npoints[0].length);
	
				
				if(ex_npoints[0].length > 4){
	
					//int[][] second_sorted = sortCurtateDistance(tranceDimension(ex_npoints),8, 20); //enlarge後もう一度してみるもいまいち。
					//Roi newResultRoi = new PolygonRoi(second_sorted[0], second_sorted[1], second_sorted[0].length, Roi.POLYGON);
					PolygonRoi newResultRoi = new PolygonRoi(ex_npoints[0],  ex_npoints[1], ex_npoints[0].length, Roi.POLYGON);
	
					resultRoi = (PolygonRoi)roiR.reducePolygonPoint(newResultRoi);

				}else{
					IJ.showMessage("Under 4 points(include calc error)");
					resultRoi = null;
				}		
				
			}else{
				resultRoi = null;
			}
		}else{
			resultRoi = (PolygonRoi)roiR.reducePolygonPoint(resultRoi);
		}
		
		if(resultRoi != null){
			if(fitSpline) resultRoi.fitSpline();
			resultRoi.setStrokeWidth(strokeWidth);
			resultRoi.setStrokeColor(strokeColor);
		}
		
		return resultRoi;

	}

	public int[][] tranceDimension(int[][] souceArray){
		int[][] result = new int[souceArray[0].length][souceArray.length];
		for(int i = 0; i < souceArray.length; i++){
			for(int n = 0; n < souceArray[0].length; n++){
				result[n][i] = souceArray[i][n];
				result[n][i] = souceArray[i][n];	
			}

		}
		return result;
	}
	
	public int[][] sortCurtateDistance(int[][] xy_array, int min, int max){
		float[][] f_array = new float[xy_array.length][xy_array[0].length];
		for(int i = 0; i < xy_array.length; i++){
			f_array[i][0] = (float)xy_array[i][0];
			f_array[i][1] = (float)xy_array[i][1];
		}
		float[][] result_f_array = sortCurtateDistance(f_array, min, max);
		int[][] result_i_array = new int[result_f_array.length][result_f_array[0].length];
		for(int i = 0; i < result_f_array[0].length; i++){
			result_i_array[0][i] = (int)result_f_array[0][i];
			result_i_array[1][i] = (int)result_f_array[1][i];
		}
		return result_i_array;
	}
	
	public float[][] sortCurtateDistance(float[][] xy_array,int minLimit, int maxLimit){ //[position][x or y]近い点を検出？同じ場合は角度？ 戻り値は[x or y][position]
		LinkedList<ArrayList<Float>> xy_list = new LinkedList<ArrayList<Float>>();
		
		for(int i = 0; i < xy_array.length; i++){
			ArrayList<Float> buff_list = new ArrayList<Float>();
			buff_list.add(xy_array[i][0]);
			buff_list.add(xy_array[i][1]);
			xy_list.add(buff_list);
		}
		ArrayList<Float> firstPosition = xy_list.peekFirst();
		ArrayList<Float> currentPosition = xy_list.pollFirst();
		
		ArrayList<Float> sorted_x = new ArrayList<Float>();
		ArrayList<Float> sorted_y = new ArrayList<Float>();
		sorted_x.add(currentPosition.get(0));
		sorted_y.add(currentPosition.get(1));

		double[] index_with_length = {0.0, 0.0};
		while(xy_list.size() > 0){
			index_with_length = checkDistance(currentPosition, xy_list);
			//System.out.println(index_with_length[1]);
			if((index_with_length[1] < minLimit)&&(index_with_length[1]>0)){ //length
				//sorted_x.add(xy_list.get((int)index_with_length[0]).get(0));
				//sorted_y.add(xy_list.get((int)index_with_length[0]).get(1));
				sorted_x.add(currentPosition.get(0));
				sorted_y.add(currentPosition.get(1));
				currentPosition = xy_list.get((int)index_with_length[0]);
				xy_list.remove((int)index_with_length[0]);
			}else if(index_with_length[1] > maxLimit){
				xy_list.clear();
			}else{
				currentPosition = xy_list.get((int)index_with_length[0]);
				xy_list.remove((int)index_with_length[0]);
			}
		}
		sorted_x.add(firstPosition.get(0));
		sorted_y.add(firstPosition.get(1));
		float[][] result = new float[2][sorted_x.size()];

		for(int i = 0; i < sorted_x.size(); i++){
			int index = i;
			result[0][i] = sorted_x.get(index);
			result[1][i] = sorted_y.get(index);
			//System.out.println("x,y = " + result[0][i] + "," + result[1][i] );
		}
		//System.out.println(result[0].length);
		return result;
	}

	public float[][] sortCurtateDistanceC(float[][] xy_array,int minLimit, int maxLimit){
		// 注目点を中心としたある範囲の円（決め打ちもしくはどんどん小さくなるような）に含まれる点のみを用いて距離を測り、そのうち最短を解とする
		//下記順番にやっちゃダメやん。近い点としてとったものを次の中心点としいかないと、、、
		LinkedList<ArrayList<Float>> xy_list = new LinkedList<ArrayList<Float>>();

		for(int i = 0; i < xy_array.length; i++){

			ArrayList<Float> buff_list = new ArrayList<Float>();
			buff_list.add(xy_array[i][0]);
			buff_list.add(xy_array[i][1]);
			xy_list.add(buff_list);
		}
		
		ArrayList<Float> firstPosition = xy_list.peekFirst();
		ArrayList<Float> currentPosition = xy_list.pollFirst();
		
		float first_x_position = firstPosition.get(0);
		float first_y_position = firstPosition.get(1);

		double circle_radius = maxLimit;

		ArrayList<Float> sorted_x = new ArrayList<Float>();
		ArrayList<Float> sorted_y = new ArrayList<Float>();

		//プログレスを出したい
		while(xy_list.size() > 0){

			
			double[] index_with_length = {0.0, maxLimit};

			float p1_x = currentPosition.get(0);
			float p1_y = currentPosition.get(1);

			sorted_x.add(p1_x);
			sorted_y.add(p1_y);

			Roi r = new OvalRoi((p1_x - circle_radius), (p1_y - circle_radius), (circle_radius * 2), (circle_radius *2));

			ArrayList<Boolean> check = new ArrayList<Boolean>();

			
			IntStream i_stream = IntStream.range(0, xy_list.size());
			i_stream.parallel().forEach(i -> {

				float p2_x = xy_list.get(i).get(0);
				float p2_y = xy_list.get(i).get(1);
				
				if(r.contains((int)p2_x, (int)p2_y) == true){ //注目点の円内に入っているかのcheck
					check.add(true);
					double l = this.getDistance(p1_x, p1_y, p2_x, p2_y);
					if(l < index_with_length[1]){
						index_with_length[0] = i;
						index_with_length[1] = l;
					}
				}	
				
			});
			
			
			if(check.contains(true)){ //隣接する点がなければ終わり(残っているのは中に含まれる要らない点のはず)
				currentPosition = xy_list.get((int)index_with_length[0]);
				xy_list.remove((int)index_with_length[0]);
			}else{
				xy_list.clear();
			}

		}


		sorted_x.add(first_x_position);
		sorted_y.add(first_y_position);
		

		float[][] result = new float[2][sorted_x.size()];

		for(int i = 0; i < sorted_x.size(); i++){
			int index = i;
			result[0][i] = sorted_x.get(index);
			result[1][i] = sorted_y.get(index);
			//System.out.println("x,y = " + result[0][i] + "," + result[1][i] );
		}

		return result;
	}

	
	public double getDistance(float sx, float sy, float ex, float ey){
		double l = Math.sqrt((Math.pow((ex - sx), 2) + Math.pow((ey - sy), 2)));		
		return l;
	}
	
	public double[] checkDistance(ArrayList<Float> c_xy, LinkedList<ArrayList<Float>> other_xy){
		double[] result = {0.0, 0.0};
		double buff_length = 0.0;
		for(int i = 0; i < other_xy.size(); i++){
			double s_x = c_xy.get(0);
			double s_y = c_xy.get(1);
			double e_x = other_xy.get(i).get(0);
			double e_y = other_xy.get(i).get(1);
			if((s_x != e_x)||(s_y != e_y)){
				double length = Math.sqrt((Math.pow((e_x - s_x), 2) + Math.pow((e_y - s_y), 2)));
				if(buff_length == 0.0){
					buff_length = length;
					result[0] = i;
					result[1] = length;
				}else if(length < buff_length){
					buff_length = length;
					result[0] = i;
					result[1] = length;
				}
			}else{
			}
		}
		
		return result;
	}
	
	
	
	//public float[][] detectConcaveHulls(){ //ConvaveHull?
	//
	//}
	
	
	public int getMarginValue(ImageProcessor ip, int x, int y){
		int result = 0;
		int fill_type_index = margin_fill_types.indexOf(fill_type);
		
		switch(fill_type_index){
			case 0: //Zero
				result = 0;
				break;
				
			case 1: //Reflect
				int reflect_x = x;
				int reflect_y = y;
				if(x < 0){
					reflect_x = - x;
				}
				if(y < 0){
					reflect_y = - y;
				}
				
				if(x > (width-1)){

					reflect_x = width - (x - (width-1));
				}
				
				if(y > (height-1)){
					reflect_y = height - (y - (height-1));
				}
				result  = ip.get(reflect_x, reflect_y);
				break;
			case 2: //Repeat
				int repeat_x = x;
				int repeat_y = y;
				if(x < 0){
					repeat_x = 0;
				}
				if(y < 0){
					repeat_y = 0;
				}
				
				if(x > (width-1)){
					repeat_x = (width-1);
				}
				
				if(y > (height-1)){
					repeat_y = (height-1);
				}
				
				result  = ip.get(repeat_x, repeat_y);
				
				
				break;
		}
		
		return result;
	}
	
	
}