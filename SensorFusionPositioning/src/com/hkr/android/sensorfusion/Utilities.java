package com.hkr.android.sensorfusion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Utilities {
/* stdDev: Calculates the standard deviation:
 * - Take the absolute value of each measured value minus the mean value
 * - Calculate the square root of the mean of the above
 * Input: measured values
 * Output: standard deviation */
	public static float stdDev(float[] x){
		int n = x.length;
		float[] dev = new float[n];
		float sum = 0;
		float sumDev = 0;
		float mean = 0;
		float stdDev = 0;

		//calc mean value
    	for(int i=0; i<n; i++){
    	    sum = sum + x[i];
    	}
    	mean = sum/n;  

    	//Absolute value of each measured value minus the mean value    	
    	for (int i=0; i<n; i++) {
    		dev[i] = (float)Math.pow(x[i] - mean, 2);
    		sumDev = sumDev + dev[i];
    	}
    	//Standard deviation
    	stdDev = (float)Math.sqrt((sumDev)/n);

    	return stdDev;
    }	
	
	public static float calculateAverage(float[] values){
		double total = 0;
		
		for (int i = 0; i < values.length; i++) {
			total += values[i];
		}
		return (float) total/values.length;
		
	}
	
	
    public static void writeToSDFile(String fileName, String folder, CharSequence s[],int number){
        
        // Find the root of the external storage.
        File root = android.os.Environment.getExternalStorageDirectory(); 
                
        File dir = new File (root.getAbsolutePath() + folder);
        dir.mkdirs();
        File file = new File(dir, fileName);
    
        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for (int i=0; i<number; i++)
              pw.println(s[i]);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }	
    }
    
    public static float[] calculateMovingAverage(float[] array, int start, int end){
    	float[] res = new float[2];
    		float x = 0;
    		float y = 0;
    		
    		for (int i = start; i < end; i=i+2) {
				x += array[i];
				y += array[i+1];
			}
    		
    		res[0] = x / (end/2);
    		res[1] = y / (end/2);
    	
    	return res;
    }
}
