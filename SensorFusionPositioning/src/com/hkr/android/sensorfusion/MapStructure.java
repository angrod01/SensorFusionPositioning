package com.hkr.android.sensorfusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import android.util.Log;



public class MapStructure {
	final static int NUMBER_APS = 4;
	final static int NORTH = 1;
	final static int WEST = 4; //Epsilon 2;
	final static int SOUTH = 3;
	final static int EAST = 2; //Epsilon 4;
	
	public boolean fingerprintsLoaded = false;
	
	private int fingerPrintNumber = 110;
	private float[][][] fingerPrints = new float [fingerPrintNumber][NUMBER_APS][7];
	
	
	
	
	public int loadFingerPrints(){
		//read from file and loadç
		//*Don't* hardcode "/sdcard"

        File root = android.os.Environment.getExternalStorageDirectory(); 

		//Get the text file
		File fileAP1 = new File(root.getAbsolutePath() + "/mapWiFi/PrintsAP1.txt");
		File fileAP2 = new File(root.getAbsolutePath() + "/mapWiFi/PrintsAP2.txt");
		File fileAP3 = new File(root.getAbsolutePath() + "/mapWiFi/PrintsAP3.txt");
		File fileAP4 = new File(root.getAbsolutePath() + "/mapWiFi/PrintsAP4.txt");

		//Read text from file
		int counter = 0;
		try {
		    BufferedReader br1 = new BufferedReader(new FileReader(fileAP1));
		    BufferedReader br2 = new BufferedReader(new FileReader(fileAP2));
		    BufferedReader br3 = new BufferedReader(new FileReader(fileAP3));
		    BufferedReader br4 = new BufferedReader(new FileReader(fileAP4));
		    String line1, line2, line3, line4;
		    
		    while (((line1 = br1.readLine()) != null)&&((line2 = br2.readLine()) != null)&&((line3 = br3.readLine()) != null)&&((line4 = br4.readLine()) != null)) {
		    	String[] values1 = line1.split(" ");
		    	String[] values2 = line2.split(" ");
		    	String[] values3 = line3.split(" ");
		    	String[] values4 = line4.split(" ");
		    	for (int i = 0; i < values1.length; i++) {
					fingerPrints[counter][0][i] = Float.parseFloat(values1[i]);
					fingerPrints[counter][1][i] = Float.parseFloat(values2[i]);
					fingerPrints[counter][2][i] = Float.parseFloat(values3[i]);
					fingerPrints[counter][3][i] = Float.parseFloat(values4[i]);
				}
		    	counter++;
		    }
		}
		catch (IOException e) {
		    //You'll need to add proper error handling here
		}
		fingerPrintNumber = counter;
		if (counter>0) fingerprintsLoaded = true;
		return counter;
	}
	
	public float calculateDistance(float[] rssi, int direction, int node){
		float res = 0;
		for (int i = 0; i < NUMBER_APS; i++) {
			res += Math.pow(fingerPrints[node][i][direction]-rssi[i],2);
		}
		float sqr = (float)Math.sqrt(res);
		return (float)Math.sqrt(sqr);
	}
	
	public float[][] getClosestNodes(float[] rssi, int direction){
		
		Log.d("WIFI", "Direction: "+ direction);
		float[] distances = new float[4];
		int[] nodes = new int[4];
		for (int i = 0; i < 4; i++) {
			distances[i] = Long.MAX_VALUE;
			nodes[i] = 0;
		}
		
		for (int i = 0; i < fingerPrintNumber; i++) {
			float d = calculateDistance(rssi, direction, i);
			//Log.d("WiFi", "Distance "+i+": "+d);
			int index = Arrays.binarySearch(distances, d);
			if (index<0) index = Math.abs(index)-1;
			if (index<4){
				float[] newDistances = new float[4];
				int[] newNodes = new int[4];
				System.arraycopy(distances, 0, newDistances, 0, index);
				System.arraycopy(nodes, 0, newNodes, 0, index);
				newDistances[index] = d;
				newNodes[index] = i;
				System.arraycopy(distances, index, newDistances, index+1, distances.length - index - 1);
				System.arraycopy(nodes, index, newNodes, index+1, nodes.length - index - 1);
				distances = newDistances;
				nodes = newNodes;
			}
			//Log.d("Wifi", "array: "+distances[0]+" "+distances[1]+" "+distances[2]+" "+distances[3]+" ");
			
		}
		float[][] res = new float[4][2];
		for (int i = 0; i < 4; i++) {
			res[i][0] = nodes[i];
			res[i][1] = distances[i];
			//Log.d("WIFI", "Node: "+ nodes[i] +" Distance:"+distances[i]);
		}
		return res;
	}
	
	public float[] estimateCoordenates(float[] rssi, double angleDiff){
		float[] res = new float[6];
		float a = 0;
		float bx = 0;
		float by = 0;
		
		float[][] closest = getClosestNodes(rssi, getDirection(angleDiff));
		for (int i = 0; i < 4; i++) {
			a += 1/closest[i][1];
			bx += fingerPrints[(int)closest[i][0]][0][5]/closest[i][1];
			by += fingerPrints[(int)closest[i][0]][0][6]/closest[i][1];
		}
		a = 1/a;
		res[0] = new Float(a*bx).longValue();
		res[1] = new Float(a*by).longValue();
		for (int i = 2; i < 6; i++) {
			res[i] = closest[i-2][0];
		}
		return res;
	}
	
//	public int getDirection(double angleDiff){
//		if (angleDiff>=0){
//			if (angleDiff<= Math.PI/4)return NORTH;
//			else if (angleDiff<= 3*Math.PI/4) return EAST;//Epsilon WEST;
//			else return SOUTH;
//		}
//		else {
//			if (angleDiff>= (-1*Math.PI/4))return NORTH;
//			else if (angleDiff>= (-3*Math.PI/4)) return WEST; //Epsilon EAST;
//			else return SOUTH;
//		}
//	}
	public int getDirection(double angleDiff){
		if (angleDiff>=0){
			if (angleDiff<= Math.PI/4)return SOUTH;
			else if (angleDiff<= 3*Math.PI/4) return EAST;//Epsilon WEST;
			else return NORTH;
		}
		else {
			if (angleDiff>= (-1*Math.PI/4))return SOUTH;
			else if (angleDiff>= (-3*Math.PI/4)) return WEST; //Epsilon EAST;
			else return NORTH;
		}
	}
}
