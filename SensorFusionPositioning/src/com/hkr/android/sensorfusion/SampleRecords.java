package com.hkr.android.sensorfusion;

import android.hardware.Sensor;
import android.util.Log;

public class SampleRecords {
	
    static final int ARRAY_SIZE = 1000;
    static final int GYRO_ARRAY_SIZE = 100000;
    
    final boolean SAVE_ALL_DATA = true;
    final boolean SAVE_SENSORS_DATA = true;
    final boolean SAVE_FREQUENCIES = false;
    final boolean SAVE_WIFI_OVERTIME = true;
    final boolean SAVE_WIFI_PRINTS = true;
    
    int counterAcc = 1;
    int counterMag = 1;
    int counterGyro = 1;
    int counterLinear = 1;
    int counterRotation = 1;
    int counterOri = 1;
    int counterWiFi = 0;
    int index = 1;
 
    CharSequence [] allDataArr;
    CharSequence [] accDataArr;
    CharSequence [] magDataArr;
    CharSequence [] gyroDataArr;
    CharSequence [] linearAccDataArr;
    CharSequence [] oriDataArr;
    

    
    CharSequence [] accFrequencyData;
    CharSequence [] magFrequencyData;
    CharSequence [] gyroFrequencyData;
    CharSequence [] linearFrequencyData;
    CharSequence [] rotationFrequencyData;
    
    CharSequence [] wifiValuesOverTime;
    
    public void initSampling(){
    	
    	if (SAVE_ALL_DATA){
    		allDataArr = new CharSequence[ARRAY_SIZE];
    		
    		allDataArr[0] = "index stamp period magX magY magZ" +
  		  " accX accY accZ ori0 ori1 ori2 angle0 angle1 angle2" +
  		  " linearAcc[0] linearAcc[1] linearAcc[2] linearVel[0] linearVel[1] linearVel[2]" +
  		  " DistanceInc[0] DistanceInc[1] DistanceInc[2] linearAcc[0]' linearAcc[1]' linearAcc[2]'" + 
  		  " linearVel[0]' linearVel[1]' linearVel[2]' DistanceInc[0]' DistanceInc[1]' DistanceInc[2]'";
    	}
    	if (SAVE_SENSORS_DATA){
    		Log.d("INIT", "sensors");
    		    		
    	    accDataArr = new CharSequence[ARRAY_SIZE];
    	    magDataArr = new CharSequence[ARRAY_SIZE];
    	    gyroDataArr = new CharSequence[GYRO_ARRAY_SIZE];
    	    linearAccDataArr = new CharSequence[ARRAY_SIZE];
    	    oriDataArr = new CharSequence[ARRAY_SIZE];
    	    
            accDataArr[0] = "index stamp period Acc[0] Acc[1] Acc[2]"; 
            magDataArr[0] = "index stamp period Mag[0] Mag[1] Mag[2]"; 
            gyroDataArr[0] = "index stamp period Gyro[0] Gyro[1] Gyro[2] Angle[0] Angle[1] Angle[2]"; 
            linearAccDataArr[0] = "index stamp period LinearAcc[0] LinearAcc[1] LinearAcc[2]"; 
            oriDataArr[0] = "index stamp period Ori[0] Ori[1] Ori[2]"; 
    	}
    	if (SAVE_FREQUENCIES){
    	    accFrequencyData = new CharSequence[ARRAY_SIZE];
    	    magFrequencyData = new CharSequence[ARRAY_SIZE];
    	    gyroFrequencyData = new CharSequence[GYRO_ARRAY_SIZE];
    	    linearFrequencyData = new CharSequence[ARRAY_SIZE];
    	    rotationFrequencyData = new CharSequence[ARRAY_SIZE];
    	}
    	if (SAVE_WIFI_OVERTIME){
    		wifiValuesOverTime = new CharSequence[ARRAY_SIZE];
    	}

    }
    
    public void recordEverything(float[]currentAcc, float[] currentMag, float[] currentOrientation, double[] currentGyroAngles, float[] currentLinearAcc, double[] currentLinearVelocity, double[] currentDistanceInc, float[] currentLinearAcc2, double[] currentLinearVelocity2, double[] currentDistanceInc2, long stamp, long period){
   		if (SAVE_ALL_DATA){
   	    	allDataArr[index] = index +" " + stamp +" "+ period/1000 + " " + currentMag[0] + " " + currentMag[1] + " " + currentMag[2] + " " +
  		  currentAcc[0] + " " + currentAcc[1] + " " + currentAcc[2] + " " +
  		  currentOrientation[0] + " " + currentOrientation[1] + " " + currentOrientation[2] + " " + 
  		  currentGyroAngles[0] + " " + currentGyroAngles[1] + " " + currentGyroAngles[2] + " " + 
  		  currentLinearAcc[0] + " " + currentLinearAcc[1] + " " + currentLinearAcc[2] + " " + 
  		  currentLinearVelocity[0] + " " + currentLinearVelocity[1] + " " + currentLinearVelocity[2] + " " + 
  		  currentDistanceInc[0] + " " + currentDistanceInc[1] + " " + currentDistanceInc[2] + " " + 
  		  currentLinearAcc2[0] + " " + currentLinearAcc2[1] + " " + currentLinearAcc2[2] + " " + 
  		  currentLinearVelocity2[0] + " " + currentLinearVelocity2[1] + " " + currentLinearVelocity2[2] + " " + 
  		  currentDistanceInc2[0] + " " + currentDistanceInc2[1] + " " + currentDistanceInc2[2];

            index = (index+1)%ARRAY_SIZE;
   		}
    }
    public void recordValues(int sensor, float[] values,  double[] values2, long stamp, long period){
    	if (sensor == Sensor.TYPE_MAGNETIC_FIELD){
            if (SAVE_SENSORS_DATA) magDataArr[counterMag] = counterMag + " " + stamp/1000 + " " + period/1000 + " " + values[0] + " " + values[1]+ " " + values[2];  
            if (SAVE_FREQUENCIES) magFrequencyData[counterMag] = counterMag + " " + period/1000;
            counterMag = (counterMag + 1)%ARRAY_SIZE;
    	}
    	else if (sensor == Sensor.TYPE_ACCELEROMETER){
    		if (SAVE_SENSORS_DATA) accDataArr[counterAcc] = counterAcc + " " + stamp/1000 + " " + period/1000 + " " + values[0] + " " + values[1]+ " " + values[2];
    		if (SAVE_FREQUENCIES) accFrequencyData[counterAcc]=counterAcc + " " + period/1000;
    		counterAcc = (counterAcc + 1)%ARRAY_SIZE;
    	}
    	else if (sensor == Sensor.TYPE_GYROSCOPE){
    		if (SAVE_SENSORS_DATA) gyroDataArr[counterGyro] = counterGyro + " " + stamp/1000 + " " + period/1000 + " " + values[0] + " " + values[1]+ " " + values[2]+ " " + values2[0] + " " + values2[1]+ " " + values2[2];  
    		if (SAVE_FREQUENCIES) gyroFrequencyData[counterGyro] = counterGyro + " " + period/1000;
            counterGyro = (counterGyro + 1)%GYRO_ARRAY_SIZE;

    	}
    	else if (sensor == Sensor.TYPE_LINEAR_ACCELERATION){
    		if (SAVE_SENSORS_DATA) linearAccDataArr[counterLinear] = counterLinear + " " + stamp/1000 + " " + period/1000 + " " + values[0] + " " + values[1]+ " " + values[2];  
    		if (SAVE_FREQUENCIES) linearFrequencyData[counterLinear] = counterLinear + " " + period/1000;
            counterLinear = (counterLinear + 1)%ARRAY_SIZE;
    	}
    	else if (sensor == Sensor.TYPE_ORIENTATION){
    		if (SAVE_SENSORS_DATA) oriDataArr[counterOri] = counterOri + " " + stamp/1000 + " " + period/1000 + " " + values[0] + " " + values[1]+ " " + values[2];  
    		
            counterOri = (counterOri + 1)%ARRAY_SIZE;
    	}
    }
    public void recordWiFiOverTime(float ap1, float ap2, float ap3, float ap4, long period){
    	if(SAVE_WIFI_OVERTIME){
    	    wifiValuesOverTime[counterWiFi] = counterWiFi + " " + period/1000 + " " + ap1 + " " + ap2 + " " + ap3+ " " + ap4;
    	    counterWiFi = (counterWiFi + 1)%ARRAY_SIZE;
    	}
    	
    }
    
    
    public void saveRecordsToSD(){
    	
    	if (SAVE_ALL_DATA){
    		Utilities.writeToSDFile("allData.txt", "/sensor_data/raw", allDataArr,index);
    	}
    	if (SAVE_FREQUENCIES){
        	Utilities.writeToSDFile("accFreq.txt", "/sensor_data/frequencies", accFrequencyData,ARRAY_SIZE);
        	Utilities.writeToSDFile("magFreq.txt", "/sensor_data/frequencies", magFrequencyData,ARRAY_SIZE);
        	Utilities.writeToSDFile("gyroFreq.txt", "/sensor_data/frequencies", gyroFrequencyData,ARRAY_SIZE);
        	Utilities.writeToSDFile("linearFreq.txt", "/sensor_data/frequencies", linearFrequencyData,ARRAY_SIZE);
        	Utilities.writeToSDFile("rotationFreq.txt", "/sensor_data/frequencies", rotationFrequencyData,ARRAY_SIZE);
    	}
    	if (SAVE_SENSORS_DATA){
        	Utilities.writeToSDFile("accRawData.txt", "/sensor_data/raw", accDataArr,counterAcc);
        	Utilities.writeToSDFile("magRawData.txt", "/sensor_data/raw", magDataArr,counterMag);
        	Utilities.writeToSDFile("gyroRawData.txt", "/sensor_data/raw", gyroDataArr,counterGyro);
        	Utilities.writeToSDFile("linearAccRawData.txt", "/sensor_data/raw", linearAccDataArr,counterLinear);
        	Utilities.writeToSDFile("oriRawData.txt", "/sensor_data/raw", oriDataArr,counterOri);
    	}
    	if (SAVE_WIFI_OVERTIME){
    		Utilities.writeToSDFile("wifiOverTime.txt", "/sensor_data/wifi", wifiValuesOverTime, counterWiFi);
    	}

    }


}
