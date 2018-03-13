package com.hkr.android.sensorfusion;

import android.widget.EditText;

public class FingerPrints {
    static final int NUMBER_APS = 4;
    static final int NUMBER_FINGERPRINTS = 105;
    static final int NUMBER_DIRECTIONS = 4;
    static final int MINIMUN_MEASURAMENTS = 3;
    static final int INITIAL_X = 100;
    static final int INITIAL_Y = 150;
    static final int DISTANCE_FINGERPRINTS_X = 50;
    static final int DISTANCE_FINGERPRINTS_Y = 50;
    
    static final String NETWORK_SSID = "EmbeddedTEST"; 
    
    static final String AP1_BSSID = "00:16:b6:54:ff:4d";
    static final String AP2_BSSID = "00:16:b6:5a:bc:82";
    static final String AP3_BSSID = "1c:af:f7:7e:fa:98";
    static final String AP4_BSSID = "00:1f:33:2a:ea:42";
    
    public float [] rssis = new float [NUMBER_APS];
    public float [] rssisAverage = new float [NUMBER_APS];
    public int [] rssisCounter = new int [NUMBER_APS];
    public float [][][] fingerprintValues = new float[NUMBER_APS][NUMBER_DIRECTIONS][NUMBER_FINGERPRINTS];
    public boolean recordingRssi = false;
    
  //  private int [][][] fingerprintLevels = new int[NUMBER_APS][NUMBER_DIRECTIONS][NUMBER_FINGERPRINTS];
    public long [][] fingerprintCoordinates = new long[NUMBER_FINGERPRINTS][2];

    public int currentFingerprintX = 100;//epsilon 132
    public int currentFingerprintY = 150;
    
    public int fingerprintCounter = 0;
    public int fingerprintDirection = 0;
	
	public void initializeRssiCountersAndAverages(){
		for (int i = 0; i < NUMBER_APS; i++) {
			rssisCounter[i] = 0;
			rssisAverage[i] = 0;
		}
	}
	
	public void resetRssiArray(){
		for (int i = 0; i < NUMBER_APS; i++) {
			rssis[i] = 0;
		}		
	}
	
	public float[] getAverageRssis(){
		float[] res = new float[NUMBER_APS];
		for (int i = 0; i < NUMBER_APS; i++) {
			if (rssisCounter[i]!=0) res[i] = (float)rssisAverage[i]/rssisCounter[i];
			else res[i] = 0;
		}
		//Log.d("WIFI", "Average: "+res[0]+" "+res[1]+" "+res[2]+" "+res[3]);
		return res;
	}
	
	public boolean checkIfMinimunMeasuraments(){
		boolean minimun = true;
		for (int i = 0; i < NUMBER_APS; i++) {
			if ((rssisCounter[i] != 0)&&(rssisCounter[i] < MINIMUN_MEASURAMENTS)) minimun = false; 
		}
		return minimun;
	}
	
	
    public boolean recordFingerprintHere(EditText printXcoordinate, EditText printYcoordinate){
    	if (checkIfMinimunMeasuraments()){
        	fingerprintCoordinates[fingerprintCounter][0] = Long.parseLong(printXcoordinate.getText().toString());
        	fingerprintCoordinates[fingerprintCounter][1] = Long.parseLong(printYcoordinate.getText().toString());
        	for (int i = 0; i < NUMBER_APS; i++) {
        		if (rssisCounter[i]!=0)
        			fingerprintValues[i][fingerprintDirection][fingerprintCounter] = rssisAverage[i]/rssisCounter[i];
        		else fingerprintValues[i][fingerprintDirection][fingerprintCounter] = 0;
        		//	fingerprintLevels[i][fingerprintDirection][fingerprintCounter] = wifiLevels[i];
    		}
        	fingerprintDirection=(fingerprintDirection + 1)%NUMBER_DIRECTIONS;
        	if (fingerprintDirection == 0)fingerprintCounter++;
    		
    		initializeRssiCountersAndAverages();
        	
    		return true;
    	}
    	else return false;
    	
    }
    public void saveFingerprints(){
    	CharSequence [] c = new CharSequence[fingerprintCounter];
    	for (int i = 0; i < NUMBER_APS; i++) {
			for (int j = 0; j < fingerprintCounter; j++) {
				c[j] = j +" "+fingerprintValues[i][0][j]+" "+
							  fingerprintValues[i][1][j]+" "+
							  fingerprintValues[i][2][j]+" "+
							  fingerprintValues[i][3][j]+" "+
							  fingerprintCoordinates[j][0]+" "+fingerprintCoordinates[j][1]; 
			}
			int n = i+1;
			Utilities.writeToSDFile("PrintsAP"+n+".txt","/sensor_data/wifi", c, fingerprintCounter);
		}
    }

}
