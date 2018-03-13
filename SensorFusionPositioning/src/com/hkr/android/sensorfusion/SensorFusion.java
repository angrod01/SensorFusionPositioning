package com.hkr.android.sensorfusion;

import java.util.Calendar;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class SensorFusion extends Activity {
	
	private static final float NS2S = 1.0f / 1000000000.0f;
    
	private static final int MODE_INIT = -1;
	private static final int MODE_READY = 0;
	private static final int MODE_RAW = 1;
	private static final int MODE_MAP = 2;
	private static final int MODE_REC = 3;
	private static final int MODE_CAL =4;
	private static final int MODE_SAMPL =5;
	
	private static final int CALIBRATION_SAMPLES = 100;
	private static final int CALIBRATION_SAMPLES_GYRO = 2000;
	private static final int BIAS_CORRECTION_SAMPLES = 2000;

	private static final long MAP_PERIOD = 100000000; //us  = 100ms

	private static final float ACC_STEP_THRESHOLD = 11;
	private static final long STEP_GAP_NSECS = 400000000;
	
	//CALIBRATION
	
	private double GRAVITY = 9.65;
	private double calibrationX = 0;
	private double calibrationY = 0;
	private double calibrationZ = 0;
	private double calibrationLinearX = 0;
	private double calibrationLinearY = 0;
	private double calibrationLinearZ = 0;
	private double calibrationCompas = 0;

	
	static double GYRO_BIAS_X = 0;	
	static double GYRO_BIAS_Y = 0;
	static double GYRO_BIAS_Z = 0;
	
	static double GYRO_CORRECTION_Z = 0;
	
	//UI
	
	private int mode = MODE_INIT;
	private MapView mMapView;
	private SensorObserver mSensorObserver;
    private SensorManager mSensorManager;
    private TextView magValues;
    private TextView gyroValues;
    private TextView accValues;
    private TextView totalAcc;
    private TextView linearAccValues;
    private TextView totalAccLinear;
    private TextView oriValues;
    private TextView wifiValues;
    private TextView titleP, titleM, titleA, titleTA, titleG, titleL, titleTL, titleO, titleW;
    private EditText printXcoordinate;
    private EditText printYcoordinate;
    private TextView estimatedPositions;
    private ImageView initImage;
    private ImageView calibrationImage;
    private ImageView calibratedImage;
    private Button plusX265Button;
    private Button minusX265Button;
    private Button plusY265Button;
    private Button minusY265Button;
    private Button recordRSSIButton;
    

    //SENSOR VALUES
    
    private float [] currentAcc = new float[3];
    private float [] currentMag = new float[3];
    private float [] currentGyro = new float[3];
    private float [] currentOrientation = new float[3];
    private float [] currentR = new float[9];
    private float [] currentLinearAcc = new float[3];

    //INTEGRATED VALUES
    
    private double currentTotalLinearAcc;
    private double [] currentAnglesGyro = new double[3];
    
    private double [] currentLinearVelocity = new double[3];
    private double [] currentDistanceIncrement = new double[3];

    private float [] currentCalcLinearAcc = new float[3];
    private double [] currentCalcLinearVelocity = new double[3];
    private double [] currentCalcDistanceIncrement = new double[3];
    
    //COUNTERS

    private int calibrationCounter = 0;
    private int calibrationCounterLinear = 0;
    private int calibrationCounterGyro = 0;
    private int updateCounter = 0;
    
    //GYRO BIAS CORRECTION 
    
    private int biasCorrectionCounter = 0;
    private double accumulatedGyro = 0;
     
    //COMPASS-GYRO FUSION
    
    static final int AZIMUTH_VALUES = 250;
    private float[] azimuthLastValues = new float[AZIMUTH_VALUES];
    private int azimuthCounter = 0;
    
    //ORIENTATION DRIFT 
    
    static final int ORIENTATION_VALUES = 100;
    private float[] orientationLastValues = new float[ORIENTATION_VALUES];
    private int orientationCounter = 0;
    private int gyroCounter = 0;
    
    //MOTION DETECTION
    
    private boolean moving = false;
    private boolean peak = false;
    private int counterMoving = 0;

    //STEP DETECTION
    
    private long accPeakEndStamp;
    private float accPeakStart;
    private float accPeakTop;
    
    private boolean accPeakStarted = false;
    private boolean accPeakGrowing = false;
    
    private int stepCounter = 0;
    private int apCounter = 0;
    
    //TIMESTAMPS
    
    long timeStamp = 0;
    long mapTimeStamp = 0;
    long wifiTimeStamp = 0;
    long accTimestamp = System.nanoTime();
    long magTimeStamp = System.nanoTime();
    long gyroTimeStamp = System.nanoTime();
    long linearTimeStamp = System.nanoTime();
    long rotationTimeStamp = System.nanoTime();
    long initTimeStamp = System.nanoTime();
    
    //OTHER
    
    private FingerPrints fingerprints;
    private SampleRecords sampleRecords = new SampleRecords();
    private MapStructure mapWiFi = new MapStructure();

    
    //In this handler the measured rssi Wi-Fi values are received
    
    public Handler scanHandler = new Handler(){
    	  @Override
    	  public void handleMessage(Message msg) {
    	    if(msg.what==1){
    	     
    	      long t = Calendar.getInstance().getTimeInMillis();
    	      long period = t - wifiTimeStamp;
    	      wifiTimeStamp = t;
    	    	
    	      Bundle bundle = msg.getData();
    	      
    	      int size = bundle.getInt("size");
    	      String[] bssids = bundle.getStringArray("bssids");
    	      String[] ssids = bundle.getStringArray("ssids");
    	      int[] levels = bundle.getIntArray("levels");
    	      String label = "";
    	      
    	      float ap1 = 0;
    	      float ap2 = 0;
    	      float ap3 = 0;
    	      float ap4 = 0;
    	      apCounter = 0;
    	      
    	      for (int i = 0; i < size; i++) {
    	    	  if(ssids[i].equals(FingerPrints.NETWORK_SSID)){
    	    		  
    	    		  if (bssids[i].equals(FingerPrints.AP1_BSSID)){
    	    			  fingerprints.rssis[0] = levels[i];
    	    			  if ((fingerprints.recordingRssi)||(!moving)) {
    	    				  fingerprints.rssisAverage[0]+=  levels[i];
    	    				  fingerprints.rssisCounter[0]++;
    	    			  }
    	    			  ap1 = levels[i];
    	    			  label+="AP 1: "+" "+levels[i]+ "dBi\n";
    	    			  apCounter++;
    	    		  }
    	    		  else if (bssids[i].equals(FingerPrints.AP2_BSSID)){
    	    			  fingerprints.rssis[1] = levels[i];
    	    			  if ((fingerprints.recordingRssi)||(!moving)) {
    	    				  fingerprints.rssisAverage[1]+=  levels[i];
    	    				  fingerprints.rssisCounter[1]++;
    	    			  }
    	    			  ap2 = levels[i];
    	    			  label+="AP 2: "+" "+levels[i]+ "dBi\n";
    	    			  apCounter++;
    	    		  }
    	    		  else if (bssids[i].equals(FingerPrints.AP3_BSSID)){
    	    			  fingerprints.rssis[2] = levels[i];
    	    			  if ((fingerprints.recordingRssi)||(!moving)) {
    	    				  fingerprints.rssisAverage[2]+=  levels[i];
    	    				  fingerprints.rssisCounter[2]++;
    	    			  }
    	    			  ap3 = levels[i];
    	    			  label+="AP 3: "+" "+levels[i]+ "dBi\n";
    	    			  apCounter++;
    	    		  }
    	    		  else if (bssids[i].equals(FingerPrints.AP4_BSSID)){
    	    			  fingerprints.rssis[3] = levels[i];
    	    			  if ((fingerprints.recordingRssi)||(!moving)) {
    	    				  fingerprints.rssisAverage[3]+=  levels[i];
    	    				  fingerprints.rssisCounter[3]++;
    	    			  }
    	    			  ap4 = levels[i];
    	    			  label+="AP 4: "+" "+levels[i]+ "dBi\n";
    	    			  apCounter++;
    	    		  }

    	    	  }

			}
    	    //Depending on the current mode different actions are taken
    	      
    	    if (mode == MODE_RAW){
    	    	//display values
        	    wifiValues.setText(label);
        	    
    	    }
    	    else if (mode == MODE_MAP){
    	    	
    	    	if ((mapWiFi.fingerprintsLoaded)&&(apCounter>=3)){ //at least 3 APs are measured
    	    		
    	    		float[] res = null;
    	    		if (moving && accPeakStarted){
        	    		res = mapWiFi.estimateCoordenates(fingerprints.rssis, currentAnglesGyro[2] - calibrationCompas);
        	    		fingerprints.resetRssiArray();
        	    		fingerprints.initializeRssiCountersAndAverages();
        	    	}
        	    	else {
        	    		res = mapWiFi.estimateCoordenates(fingerprints.getAverageRssis(), currentAnglesGyro[2] - calibrationCompas);
        	    		fingerprints.resetRssiArray();
        	    	}
        			int distance = mMapView.distanceToNewWifiPosition((int)res[0], (int)res[1]);
        			if (distance<1000){
        				
        			}
        	    	mMapView.updateCurrentPositionWifi(res[0], res[1]);
        			mMapView.update();
    	    	}
    	    }
    	    if (mode == MODE_SAMPL){
    	    	//save samples
    	    	sampleRecords.recordWiFiOverTime(ap1, ap2, ap3, ap4, period);
    	    }	    

    	    }
    	    super.handleMessage(msg);
    	  }
    	};
    	
    	
    	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initImage = (ImageView) findViewById(R.id.logoinit);
        calibrationImage = (ImageView) findViewById(R.id.logocalibration);
        calibratedImage = (ImageView) findViewById(R.id.logocalibrated);
        
        magValues = (TextView) findViewById(R.id.magValues);
        magValues.setVisibility(TextView.GONE);
        gyroValues = (TextView) findViewById(R.id.gyroValues);
        gyroValues.setVisibility(TextView.GONE);
        accValues = (TextView) findViewById(R.id.accValues);
        accValues.setVisibility(TextView.GONE);
        totalAcc = (TextView) findViewById(R.id.totalAcc);
        totalAcc.setVisibility(TextView.GONE);
        linearAccValues = (TextView) findViewById(R.id.linearAccValues);
        linearAccValues.setVisibility(TextView.GONE);
        totalAccLinear = (TextView) findViewById(R.id.totalLinearAcc);
        totalAccLinear.setVisibility(TextView.GONE);
        oriValues = (TextView) findViewById(R.id.oriValues);
        oriValues.setVisibility(TextView.GONE);
        wifiValues = (TextView) findViewById(R.id.wifi);
        wifiValues.setVisibility(TextView.GONE);

        titleP = (TextView) findViewById(R.id.titleEstimatedPos); 
        titleM = (TextView) findViewById(R.id.titleMagneticValues);
        titleA = (TextView) findViewById(R.id.titleAccValues);
        titleTA = (TextView) findViewById(R.id.titleTotalAcc);
        titleG = (TextView) findViewById(R.id.titleGyroValues);
        titleL = (TextView) findViewById(R.id.titleLinearAcc);
        titleTL = (TextView) findViewById(R.id.titleTotalLinearAcc);
        titleO = (TextView) findViewById(R.id.titleOrientValues); 
        titleW = (TextView) findViewById(R.id.titleWiFiValues);
        
        printXcoordinate = (EditText) findViewById(R.id.printXcoordinate);
        printXcoordinate.setVisibility(EditText.GONE); 
        printXcoordinate.setText(""+FingerPrints.INITIAL_X);
        printYcoordinate = (EditText) findViewById(R.id.printYcoordinate);
        printYcoordinate.setVisibility(EditText.GONE);
        printYcoordinate.setText(""+FingerPrints.INITIAL_Y);
        estimatedPositions = (TextView) findViewById(R.id.estimated_position);
        estimatedPositions.setVisibility(TextView.GONE);
        plusX265Button = (Button) findViewById(R.id.Add265toX);
        plusX265Button.setText("+ "+FingerPrints.DISTANCE_FINGERPRINTS_X);
        plusX265Button.setVisibility(Button.GONE);
        minusX265Button = (Button) findViewById(R.id.substrac265toX);
        minusX265Button.setText("- "+FingerPrints.DISTANCE_FINGERPRINTS_X);
        minusX265Button.setVisibility(Button.GONE);
        plusY265Button = (Button) findViewById(R.id.add265toY);
        plusY265Button.setText("+ "+FingerPrints.DISTANCE_FINGERPRINTS_Y);
        plusY265Button.setVisibility(Button.GONE);
        minusY265Button = (Button) findViewById(R.id.substract265toY);
        minusY265Button.setText("- "+FingerPrints.DISTANCE_FINGERPRINTS_Y);
        minusY265Button.setVisibility(Button.GONE);
        recordRSSIButton = (Button) findViewById(R.id.record_fingerprint_button);
        recordRSSIButton.setVisibility(Button.GONE);
        
        fingerprints = new FingerPrints();
        
        plusX265Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	fingerprints.currentFingerprintX += FingerPrints.DISTANCE_FINGERPRINTS_X;
            	printXcoordinate.setText(""+fingerprints.currentFingerprintX);
            }
        });
        minusX265Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	fingerprints.currentFingerprintX -= FingerPrints.DISTANCE_FINGERPRINTS_X;
            	printXcoordinate.setText(""+fingerprints.currentFingerprintX);
            }
        });
        plusY265Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	fingerprints.currentFingerprintY += FingerPrints.DISTANCE_FINGERPRINTS_Y;
            	printYcoordinate.setText(""+fingerprints.currentFingerprintY);
            }
        });
        minusY265Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	fingerprints.currentFingerprintY -= FingerPrints.DISTANCE_FINGERPRINTS_Y;
            	printYcoordinate.setText(""+fingerprints.currentFingerprintY);
            }
        });
        recordRSSIButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if(fingerprints.recordingRssi){
            		if(fingerprints.recordFingerprintHere(printXcoordinate, printYcoordinate)){
                        String d = "";
                        if(fingerprints.fingerprintDirection==0) d = " N";
                        else if(fingerprints.fingerprintDirection==1) d = " W";
                        else if(fingerprints.fingerprintDirection==2) d = " S";
                        else if(fingerprints.fingerprintDirection==3) d = " E";
                    	recordRSSIButton.setText("Record "+fingerprints.fingerprintCounter+d);
                    	fingerprints.recordingRssi = false;
            		}
            	}
            	else{
            		fingerprints.recordingRssi = true;
            		String d = "";
                    if(fingerprints.fingerprintDirection==0) d = " N";
                    else if(fingerprints.fingerprintDirection==1) d = " W";
                    else if(fingerprints.fingerprintDirection==2) d = " S";
                    else if(fingerprints.fingerprintDirection==3) d = " E";
                	recordRSSIButton.setText("Save "+fingerprints.fingerprintCounter+d);
            	}
            }
        });
        
        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        //start wifi scanning thread
        wifiTimeStamp = Calendar.getInstance().getTimeInMillis();
        WifiChannel wifi = new WifiChannel(this,scanHandler);
        
        new Thread(wifi).start();
       
        // instantiate our simulation view and set it as the activity's content
        mSensorObserver = new SensorObserver(this);
        mode = MODE_INIT;
        currentAnglesGyro[0] = 0;
        currentAnglesGyro[1] = 0;
        currentAnglesGyro[2] = 0;
        currentLinearVelocity[0] = 0;
        currentLinearVelocity[1] = 0;
        currentLinearVelocity[2] = 0;
        currentDistanceIncrement[0] = 0;
        currentDistanceIncrement[1] = 0;
        currentDistanceIncrement[2] = 0;
        
        currentCalcLinearVelocity[0] = 0;
        currentCalcLinearVelocity[1] = 0;
        currentCalcLinearVelocity[2] = 0;
        currentCalcDistanceIncrement[0] = 0;
        currentCalcDistanceIncrement[1] = 0;
        currentCalcDistanceIncrement[2] = 0;
        
        sampleRecords.initSampling();
        
    }
    
    //MENU
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mode==MODE_MAP){
			menu.clear();
			if (mMapView.ChannelAcc) menu.add(0, 1001, Menu.NONE, "Disable Acc");
			else menu.add(0, 1001, Menu.NONE, "Enable Acc");
			if (mMapView.ChannelLinAcc) menu.add(0, 1002, Menu.NONE, "Disable LinAcc");
			else menu.add(0, 1002, Menu.NONE, "Enable LinAcc");
			if (mMapView.ChannelSteps) menu.add(0, 1003, Menu.NONE, "Disable Steps");
			else menu.add(0, 1003, Menu.NONE, "Enable Steps");
			if (mMapView.ChannelWifi) menu.add(0, 1004, Menu.NONE, "Disable WiFi");
			else menu.add(0, 1004, Menu.NONE, "Enable WiFi");
			if (mMapView.showMerged) menu.add(0, 1007, Menu.NONE, "Hide Merged");
			else menu.add(0, 1007, Menu.NONE, "Show Merged");
			if (mMapView.showSteps) menu.add(0, 1008, Menu.NONE, "Hide Steps");
			else menu.add(0, 1008, Menu.NONE, "Show Steps");
			if (mMapView.showLinAcc) menu.add(0, 1009, Menu.NONE, "Hide LinAcc");
			else menu.add(0, 1009, Menu.NONE, "Show LinAcc");
			if (mMapView.showWifi) menu.add(0, 1010, Menu.NONE, "Hide WiFi");
			else menu.add(0, 1010, Menu.NONE, "Show WiFi");
			if (mMapView.showAcc) menu.add(0, 1011, Menu.NONE, "Hide Acc");
			else menu.add(0, 1011, Menu.NONE, "Show Acc");
			menu.add(0, 1005, Menu.NONE, "++ WiFi MovingAverage ("+mMapView.movingAverage/2+")");
			menu.add(0, 1006, Menu.NONE, " --  WiFi MovingAverage ("+mMapView.movingAverage/2+")");
		}
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.calibrate:
            changeMode(MODE_CAL);
            return true;        
        case R.id.show_map:
            changeMode(MODE_MAP);
            return true;
        case R.id.sampling:
        	changeMode(MODE_SAMPL);
        	return true;
        case R.id.raw_data:
        	changeMode(MODE_RAW);
        	return true;
        case R.id.record_fp:
        	changeMode(MODE_REC);
        	return true;
        case R.id.load_fingerprints:
        		int n = mapWiFi.loadFingerPrints();
        		item.setTitle("Reload (loaded "+n+")");
        	return true;
        //Map menu
        case 1001:
        	mMapView.ChannelAcc = !mMapView.ChannelAcc;
        	return true;
        case 1002:
        	mMapView.ChannelLinAcc = !mMapView.ChannelLinAcc;
        	return true;
        case 1003:
        	mMapView.ChannelSteps = !mMapView.ChannelSteps;
        	return true;
        case 1004:
        	mMapView.ChannelWifi = !mMapView.ChannelWifi;
        	return true;	
        case 1005:
        	mMapView.movingAverage = mMapView.movingAverage + 2;
        	return true;
        case 1006:
        	mMapView.movingAverage = mMapView.movingAverage - 2;
        	return true;
        case 1007:
        	mMapView.showMerged = !mMapView.showMerged;
        	return true;
        case 1008:
        	mMapView.showSteps = !mMapView.showSteps;
        	return true;
        case 1009:
            	mMapView.showLinAcc = !mMapView.showLinAcc;
            	return true;
        case 1010:
            	mMapView.showWifi = !mMapView.showWifi;
            	return true; 
        case 1011:
            	mMapView.showAcc = !mMapView.showAcc;
            	return true;    	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    private void ShowHideEntries(int m, boolean show){
    	int visibility;
    	if (show) visibility = TextView.VISIBLE;
    	else visibility = TextView.GONE;
    	switch (m) {
		case MODE_RAW:
            titleP.setVisibility(visibility);
            titleM.setVisibility(visibility);
            titleA.setVisibility(visibility);
            titleTA.setVisibility(visibility);
            titleG.setVisibility(visibility);
            titleL.setVisibility(visibility);
            titleTL.setVisibility(visibility);
            titleO.setVisibility(visibility); 
            titleW.setVisibility(visibility);
            magValues.setVisibility(visibility);
            gyroValues.setVisibility(visibility);
            accValues.setVisibility(visibility);
            totalAcc.setVisibility(visibility);
            linearAccValues.setVisibility(visibility);
            totalAccLinear.setVisibility(visibility);
            oriValues.setVisibility(visibility);
            wifiValues.setVisibility(visibility);
			break;
		case MODE_INIT:
			initImage.setVisibility(visibility);
			break;
		case MODE_READY:
			calibratedImage.setVisibility(visibility);
			break;	
		case MODE_REC:
    		printXcoordinate.setVisibility(visibility);
    		printYcoordinate.setVisibility(visibility);
    		plusX265Button.setVisibility(visibility);
    		plusY265Button.setVisibility(visibility);
    		minusX265Button.setVisibility(visibility);
    		minusY265Button.setVisibility(visibility);
    		recordRSSIButton.setVisibility(visibility);  
			break;
		case MODE_MAP:
			
			break;	
		case MODE_CAL:
			calibrationImage.setVisibility(visibility);
			break;
		default:
			break;
		}
    }

    private void changeMode(int newMode){
    	switch (newMode) {
		case MODE_INIT:
			ShowHideEntries(MODE_RAW, false);
			ShowHideEntries(MODE_REC, false);
			ShowHideEntries(MODE_INIT, true);
			ShowHideEntries(MODE_CAL, false);
			ShowHideEntries(MODE_READY, false);
			mode = MODE_INIT;
			break;
		case MODE_READY:
			ShowHideEntries(MODE_RAW, false);
			ShowHideEntries(MODE_REC, false);
			ShowHideEntries(MODE_INIT, false);
			ShowHideEntries(MODE_CAL, false);
			ShowHideEntries(MODE_READY, true);
			mode = MODE_INIT;
			break;	
		case MODE_RAW:
			ShowHideEntries(MODE_RAW, true);
			ShowHideEntries(MODE_REC, false);
			ShowHideEntries(MODE_INIT, false);
			ShowHideEntries(MODE_CAL, false);
			ShowHideEntries(MODE_READY, false);
			mode = MODE_RAW;
			break;
		case MODE_SAMPL:
			ShowHideEntries(MODE_RAW, false);
			ShowHideEntries(MODE_REC, false);
			ShowHideEntries(MODE_INIT, false);
			ShowHideEntries(MODE_CAL, false);
			ShowHideEntries(MODE_READY, false);
			mode = MODE_SAMPL;
			break;	
		case MODE_REC:
			
			ShowHideEntries(MODE_RAW, false);
			ShowHideEntries(MODE_REC, true);
			ShowHideEntries(MODE_INIT, false);
			ShowHideEntries(MODE_CAL, false);
			ShowHideEntries(MODE_READY, false);
			mode = MODE_REC;
			break;
		case MODE_MAP:
			mMapView = new MapView(this);
			mMapView.setVisibility(View.VISIBLE);
			setContentView(mMapView);
			mode = MODE_MAP;
			break;	
		case MODE_CAL:
			ShowHideEntries(MODE_RAW, false);
			ShowHideEntries(MODE_REC, false);
			ShowHideEntries(MODE_INIT, false);
			ShowHideEntries(MODE_CAL, true);
			ShowHideEntries(MODE_READY, false);
			mode = MODE_CAL;
			break;
		default:
			break;
			
		}
    }
    
	@Override
    protected void onResume() {
        super.onResume();
        // Start the simulation
        mSensorObserver.startSampling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the simulation
        mSensorObserver.stopSampling();
        if (mode == MODE_SAMPL)
        {
        	sampleRecords.saveRecordsToSD();
        }
        else if (mode == MODE_REC){
            fingerprints.saveFingerprints();
        }
    }

    //here the sersors are sampled and treated
    class SensorObserver extends View implements SensorEventListener {
        
    	private Sensor accelerometer, magField, gyroscope, linearAcc, rotationVec;
        double gravity = SensorManager.STANDARD_GRAVITY;
        float currAcc = 0;
        float maxAcc = 0;
        
        public SensorObserver(Context context) {
            super(context);
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            linearAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            rotationVec = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        
        //starts listening sensor values
        public void startSampling() {
        	mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);        	
            mSensorManager.registerListener(this, magField, SensorManager.SENSOR_DELAY_FASTEST); 
            mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST); 
            mSensorManager.registerListener(this, linearAcc, SensorManager.SENSOR_DELAY_FASTEST); 
            mSensorManager.registerListener(this, rotationVec, SensorManager.SENSOR_DELAY_FASTEST); 
        }
        
        //stops listening sensor values
        public void stopSampling() {
            mSensorManager.unregisterListener(this);
        }
        
        //step detection algorithm
        public boolean detectSteps(float [] accValues, long timeStamp){
        	float total = (float)Math.sqrt(Math.pow(accValues[0], 2)+Math.pow(accValues[1], 2)+Math.pow(accValues[2], 2));
        	if(!accPeakStarted){
        		if (total >= ACC_STEP_THRESHOLD){
        			if ((timeStamp - accPeakEndStamp)>STEP_GAP_NSECS){
            			accPeakStart = total;
            			accPeakTop = total;
            			accPeakStarted = true;
            			accPeakGrowing = true;
        			}
        		}
        		return false;
        	}
        	else{
        		if (accPeakGrowing){
        			if (total>accPeakTop){
        				accPeakTop = total;
        			}
        			else{
        				accPeakGrowing = false;
        			}
        			return false;
        		}
        		else{
        			if (total < accPeakStart){
        				accPeakEndStamp = timeStamp;
        				stepCounter++;
        				accPeakStarted = false;
        				return true;
        			}
        			else return false;
        			
        		}
        	}
        }
        
        //update step position in map
        public void updateCurrentPositionSteps(){
        	double angle;

        	angle = (currentAnglesGyro[2] - calibrationCompas) + Math.PI;  //Epsilon + Math.PI/2;
        	
			float x = (float) (mMapView.getCurrentMergedX() + mMapView.STEP_PATTERN*Math.sin(angle));
			float y = (float) (mMapView.getCurrentMergedY() + mMapView.STEP_PATTERN*Math.cos(angle));
			mMapView.updateCurrentPositionSteps(x, y);
			
			mMapView.update(); 
        }
        
        //update acceleration position in map
        public void updateCurrentPositionAcc(){
        	double angle;

        	angle = (currentAnglesGyro[2] - calibrationCompas) + Math.PI;  //Epsilon + Math.PI/2;
        	
			float x = (float) (mMapView.getCurrentAccX() + 100*currentCalcDistanceIncrement[1]*Math.sin(angle));
			float y = (float) (mMapView.getCurrentAccY() + 100*currentCalcDistanceIncrement[1]*Math.cos(angle));
			mMapView.updateCurrentPositionAcc(x, y);
			updateCounter++;
			if(updateCounter==50){
				//refresh view
				mMapView.update();
				updateCounter = 0;
			}
        }
        
        //update linear acceleration position in the map
        public void updateCurrentPositionLinAcc(){
        	
        	if(currentDistanceIncrement[1]>0){
            	double angle;
            	currentDistanceIncrement[1] *= 3;
            	angle = (currentAnglesGyro[2] - calibrationCompas) + Math.PI;  //Epsilon + Math.PI/2;
            	
    			float x = (float) (mMapView.getCurrentMergedX() + 100*currentDistanceIncrement[1]*Math.sin(angle));
    			float y = (float) (mMapView.getCurrentMergedY() + 100*currentDistanceIncrement[1]*Math.cos(angle));
    			mMapView.updateCurrentPositionLinAcc(x, y);
    			updateCounter++;

    			if(updateCounter==50){
    				//refresh view
    				mMapView.update();
    				updateCounter = 0;
    			}        		
        	}
        }
        
        //display raw values on the screen
        public void showRawValues(int sensor,float[] data){
        	switch (sensor) {
			case Sensor.TYPE_MAGNETIC_FIELD:

	        	CharSequence strMagX = "" + data[0];
	        	CharSequence strMagY = "" + data[1];
	        	CharSequence strMagZ = "" + data[2];   

	        	magValues.setText(String.format("X: " + strMagX + "\n" +
	        			  						  "Y: " + strMagY + "\n" +
	        			  						  "Z: " + strMagZ )); 
				break;
			case Sensor.TYPE_ACCELEROMETER:
				
				double acc;
        		acc = Math.sqrt(Math.pow(data[0], 2) +
                                Math.pow(data[1], 2) +
                                Math.pow(data[2], 2));

        		currAcc = Math.abs((float)(acc - gravity));
        		if (currAcc > maxAcc)
        			maxAcc = currAcc;
        		CharSequence strVal0 = "" + data[0];
        		CharSequence strVal1 = "" + data[1];
        		CharSequence strVal2 = "" + data[2];
        		
        		accValues.setText(String.format("X: " + strVal0 + "\n" +
              		                            "Y: " + strVal1 + "\n" +
              		                            "Z: " + strVal2 ));
        		totalAcc.setText(String.format("" + currAcc+ "  "+stepCounter));
        		break;
			case 
			Sensor.TYPE_ORIENTATION:
        		
				CharSequence strOri0 = "" + data[0];
        		CharSequence strOri1 = "" + data[1];
        		CharSequence strOri2 = "" + data[2];
        		oriValues.setText(String.format("X: " + strOri0 + " °\n" +
                                                "Y: " + strOri1 + " °\n" +
                                                "Z: " + strOri2 + " °"));
        		break;
			case Sensor.TYPE_GYROSCOPE:
	        	 
				 CharSequence strGyroX = "" + data[0];
	        	 CharSequence strGyroY = "" + data[1];
	        	 CharSequence strGyroZ = "" + data[2];   
	              
	        	  gyroValues.setText(String.format("X: " + strGyroX + "\n" +
	        			  						   "Y: " + strGyroY + "\n" +
	        			  						   "Z: " + strGyroZ )); 
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:

	        	  CharSequence strLinX = "" + data[0];
	        	  CharSequence strLinY = "" + data[1];
	        	  CharSequence strLinZ = "" + data[2];
	              currentTotalLinearAcc = Math.sqrt(Math.pow(data[0], 2) +
	                                          Math.pow(data[1], 2) +
	                                          Math.pow(data[2], 2));
	        	  linearAccValues.setText(String.format("X: " + strLinX + "\n" +
	        			  						  		"Y: " + strLinY + "\n" +
	        			  						  		"Z: " + strLinZ ));
	        	  totalAccLinear.setText("" + currentTotalLinearAcc);
				break;
			default:
				break;
			}
        	
        }
        
        //sums 2 angles giving the result between -PI and PI
        public double addAngles(double alfa, double beta){
        	
        	double sum = alfa + beta;
        	
        	if (sum > Math.PI){
        		sum = sum - (Math.PI*2);
        	}
        	else if (sum < (-Math.PI)){
        		sum = sum + (Math.PI*2);
        	}
        	
        	return sum;
        }
        
        
        @Override
        public void onSensorChanged(SensorEvent event) {    
          //MAGNETOMETER EVENT
         if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
          { 
             long period = event.timestamp - magTimeStamp;
             
             magTimeStamp = event.timestamp;
             currentMag = event.values.clone();
             
             if (mode == MODE_SAMPL) sampleRecords.recordValues(Sensor.TYPE_MAGNETIC_FIELD, currentMag, null, magTimeStamp, period);
             else if (mode == MODE_RAW) showRawValues(Sensor.TYPE_MAGNETIC_FIELD, currentMag);

          }
          //GYROSCOPE EVENT
          else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
          { 
        	 if (mode != MODE_CAL){
        	  long period = event.timestamp - gyroTimeStamp;
           	  gyroTimeStamp = event.timestamp;
              if ((period >0)&&(period<3000000)){
            	  currentGyro = event.values.clone();

            	  accumulatedGyro += currentGyro[2];
            	  biasCorrectionCounter ++;
            	  
            	  if (biasCorrectionCounter == BIAS_CORRECTION_SAMPLES){
            		  double average = (double)accumulatedGyro/BIAS_CORRECTION_SAMPLES;
            		  //update bias
            		  if((average<=0)&&(average>=-0.01f)){
            			  GYRO_BIAS_Z = average;
            		  }
            		  accumulatedGyro = 0;
            		  biasCorrectionCounter = 0;
            	  }
            	  //compensate bias
            	  currentGyro[0] = (float)(currentGyro[0] - GYRO_BIAS_X);
            	  currentGyro[1] = (float)(currentGyro[1] - GYRO_BIAS_Y);
            	  currentGyro[2] = (float)(currentGyro[2] - GYRO_BIAS_Z);
            	  //integrate angular speed
               	  currentAnglesGyro[0] = addAngles(currentAnglesGyro[0],(double)(currentGyro[0])*period/1000000000);
               	  currentAnglesGyro[1] = addAngles(currentAnglesGyro[1],(double)(currentGyro[1])*period/1000000000);
               	  if (Math.abs(currentGyro[2])>0.01) currentAnglesGyro[2] = addAngles(currentAnglesGyro[2],(double)(currentGyro[2])*period/1000000000);
               	  
            	  gyroCounter++;
            	  
            	  if (gyroCounter == 50){
            		  gyroCounter = 0;
            		  orientationLastValues[orientationCounter] = (float)currentAnglesGyro[2];
            		  orientationCounter++;
            		  //make angles straight
            		  if (orientationCounter == ORIENTATION_VALUES){
            			  orientationCounter = 0;
            			  float dev = Utilities.stdDev(orientationLastValues);
            			  if (dev < 0.02){
            				  float diff = (float)addAngles(currentAnglesGyro[2], (-1)*calibrationCompas);
            				  if (diff>0){
            					  if (diff<Math.PI/8) currentAnglesGyro[2] = calibrationCompas;
            					  else if ((diff>3*Math.PI/8)&&(diff<5*Math.PI/8)) currentAnglesGyro[2] = addAngles(calibrationCompas, Math.PI/2);
            					  else if (diff>7*Math.PI/8) currentAnglesGyro[2] = addAngles(calibrationCompas, Math.PI);
            				  }
            				  else{
            					  if (diff>Math.PI/8*(-1)) currentAnglesGyro[2] = calibrationCompas;
            					  else if ((diff<(-3)*Math.PI/8)&&(diff>(-5)*Math.PI/8)) currentAnglesGyro[2] = addAngles(calibrationCompas, (-1)*Math.PI/2);
            					  else if (diff<(-7)*Math.PI/8) currentAnglesGyro[2] = addAngles(calibrationCompas, (-1)*Math.PI);
            				  }
            			  }
            		  }
            	  }
            	  if (mode == MODE_SAMPL) sampleRecords.recordValues(Sensor.TYPE_GYROSCOPE, currentGyro, currentAnglesGyro, gyroTimeStamp, period);
                  if(mode == MODE_RAW) showRawValues(Sensor.TYPE_GYROSCOPE, currentGyro);
              }
           	  
        	 }
        	 if (mode == MODE_CAL){
        		 if (calibrationCounterGyro < CALIBRATION_SAMPLES_GYRO){
        			 GYRO_BIAS_X += (double)event.values[0];
        			 GYRO_BIAS_Y += (double)event.values[1];
        			 GYRO_BIAS_Z += (double)event.values[2];
        		 }
        		 else if (calibrationCounterGyro == CALIBRATION_SAMPLES_GYRO){
        			 //calculate bias
        			 GYRO_BIAS_X = (double)GYRO_BIAS_X/CALIBRATION_SAMPLES_GYRO;
        			 GYRO_BIAS_Y = (double)GYRO_BIAS_Y/CALIBRATION_SAMPLES_GYRO;
        			 GYRO_BIAS_Z = (double)GYRO_BIAS_Z/CALIBRATION_SAMPLES_GYRO;
    		    	
    		    	changeMode(MODE_READY);
        		 }
        		 calibrationCounterGyro++;
        	 }
          }    
         
         //LINEAR ACCELERATION EVENT
          else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
          {
        	  if (mode != MODE_CAL){
        		  
              long period = event.timestamp - linearTimeStamp; 
              
              linearTimeStamp = event.timestamp;
              
              if ((period >0)&&(period<30000000)){
                  double dT = (double)period*NS2S;
            	  
                  currentLinearAcc = event.values.clone();
                  currentLinearAcc[1] *= (-1);
                  if (moving){
                	  //integrate lin acceleration
                      currentLinearVelocity[0] += (currentLinearAcc[0] - calibrationLinearX)*dT; // m/s
                      currentLinearVelocity[1] += (currentLinearAcc[1] - calibrationLinearY)*dT;
                      currentLinearVelocity[2] += (currentLinearAcc[2] - calibrationLinearZ)*dT;
                      //integrate velocity
                      currentDistanceIncrement[0] = currentLinearVelocity[0]*dT + currentLinearAcc[0]*dT*dT/2;
                      currentDistanceIncrement[1] = currentLinearVelocity[1]*dT + currentLinearAcc[1]*dT*dT/2;
                      currentDistanceIncrement[2] = currentLinearVelocity[2]*dT + currentLinearAcc[2]*dT*dT/2;
                      
                  }
                  else{
                	  //reset velocity
                	  currentLinearVelocity[0] = 0;
                	  currentLinearVelocity[1] = 0;
                	  currentLinearVelocity[2] = 0;
                  }
  
              }
              if (mode == MODE_SAMPL) {
            	  	sampleRecords.recordValues(Sensor.TYPE_LINEAR_ACCELERATION, currentLinearAcc, null, linearTimeStamp, period);
            	  	sampleRecords.recordEverything(currentAcc, currentMag, currentOrientation, currentAnglesGyro, currentLinearAcc, currentLinearVelocity, currentDistanceIncrement, currentCalcLinearAcc, currentCalcLinearVelocity, currentCalcDistanceIncrement, event.timestamp, period);
              }
              if(mode == MODE_RAW) showRawValues(Sensor.TYPE_LINEAR_ACCELERATION, currentLinearAcc);
              
             }
        	 else if (mode == MODE_CAL){
        			if (calibrationCounterLinear < CALIBRATION_SAMPLES){
        				
    					calibrationLinearX += (double)event.values[0];
    					calibrationLinearY += (double)event.values[1];
    					calibrationLinearZ += (double)event.values[2];
        			}
        			else if (calibrationCounterLinear == CALIBRATION_SAMPLES){
        				//calculate linear acc bias
        				calibrationLinearX = 0;//(double)calibrationLinearX/CALIBRATION_SAMPLES;
        				calibrationLinearY = 0;//(double)calibrationLinearY/CALIBRATION_SAMPLES;
        				calibrationLinearZ = 0;//(double)calibrationLinearZ/CALIBRATION_SAMPLES;
        			}

        			calibrationCounterLinear++;
        		} 
          }

          //ACCELEROMETER EVENT
          else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
          { 
        	  
        	  long period = 0;
        	  if (mode != MODE_CAL){
        	    period = event.timestamp - accTimestamp;
           		
           		accTimestamp = event.timestamp;

           		if ((period >0)&&(period<30000000)){
           			
           			double dT = (double)period*NS2S;
            		currentAcc = event.values.clone();  
            		
            		//Checking if there is a peak in every 10 measurments
            		double totalAccWithoutG = Math.sqrt(Math.pow(currentAcc[0], 2) +
                            Math.pow(currentAcc[1], 2) +
                            Math.pow(currentAcc[2], 2)) - GRAVITY;
            		//double linearAccTrolley = currentAcc[1] - calibrationY;
            		
            		
            		if ((totalAccWithoutG>1)||(totalAccWithoutG<-1)) {
            			peak = true;
            			moving = true;
            			if (mMapView!=null) mMapView.moving = true;
            		}
            		counterMoving++;
            		if(counterMoving >= 15){
            		//	float dev = Utilities.stdDev(totalAccLast25);
            		//	Log.d("STDEV",""+dev);
            			moving = peak;
            			if (mMapView!=null) mMapView.moving = peak;
            			peak = false;
            			counterMoving = 0;
            			if (!moving) {
            				currentLinearVelocity[1] = 0;
            			}
            		}
            		//compensate gravity and integrate
                	currentCalcLinearVelocity[1] += (double)((currentAcc[1] - calibrationY)*dT);
                	//integrate velocity
                	currentCalcDistanceIncrement[1] = (double)(currentCalcLinearVelocity[1]*dT + (currentAcc[1] - calibrationY)*dT*dT/2);
                	//currentCalcDistanceIncrement[2] += (double)(currentCalcLinearVelocity[2]*dT + currentCalcLinearAcc[2]*dT*dT/2);

                	//COMPASS FUNCTION 
            		SensorManager.getRotationMatrix(currentR, null, currentAcc, currentMag);
            		SensorManager.getOrientation(currentR, currentOrientation);
            		
            		azimuthLastValues[azimuthCounter] = currentOrientation[0];
            		azimuthCounter++;
            		
            		if (azimuthCounter == AZIMUTH_VALUES){
            			Log.d("COMPASS", "Average: " + Utilities.calculateAverage(azimuthLastValues) + " Std dev: "+ Utilities.stdDev(azimuthLastValues));
            			azimuthCounter = 0;
            		}            		
           		}
        	  }
        		if ((mode == MODE_MAP)&&(event.timestamp - mapTimeStamp > MAP_PERIOD)){
        		
        			if (detectSteps(event.values, event.timestamp)){
        				//if there was an step update position
        				updateCurrentPositionSteps();
        			}
    				if (moving){
    					//if moving update lin acc
    					updateCurrentPositionLinAcc();
    				}
    				updateCurrentPositionAcc();
        		}
        		else if (mode == MODE_SAMPL){
        			sampleRecords.recordValues(Sensor.TYPE_ACCELEROMETER, currentAcc, null, accTimestamp, period);
        			sampleRecords.recordValues(Sensor.TYPE_ORIENTATION, currentOrientation, null, accTimestamp, period);
        		}
        		else if (mode == MODE_RAW){
        			showRawValues(Sensor.TYPE_ACCELEROMETER, currentAcc);
        			showRawValues(Sensor.TYPE_ORIENTATION, currentOrientation);
        			float[] angles = new float [3];
        			angles[0] = (float)currentAnglesGyro[0];  angles[1] = (float)currentAnglesGyro[1]; angles[2] = (float)currentAnglesGyro[2];  
        			showRawValues(Sensor.TYPE_GYROSCOPE, angles);
        		}
        		else if (mode == MODE_CAL){
        			if (calibrationCounter < CALIBRATION_SAMPLES){
    		    		double t = Math.sqrt(Math.pow(event.values[0], 2)+Math.pow(event.values[1], 2)+Math.pow(event.values[2], 2));
    					GRAVITY += (double)t;
    					calibrationX += (double)event.values[0];
    					calibrationY += (double)event.values[1];
    					calibrationZ += (double)event.values[2];
    					
                		SensorManager.getRotationMatrix(currentR, null, event.values, currentMag);
                		SensorManager.getOrientation(currentR, currentOrientation);
                		
                		calibrationCompas += currentOrientation[0]/CALIBRATION_SAMPLES;
        			}
        			else if (calibrationCounter == CALIBRATION_SAMPLES){
        				//calculate gravity components
        		    	GRAVITY = (double)GRAVITY/CALIBRATION_SAMPLES;
        		    	calibrationX = (double)calibrationX/CALIBRATION_SAMPLES;
        		    	calibrationY = (double)calibrationY/CALIBRATION_SAMPLES;
        		    	calibrationZ = (double)calibrationZ/CALIBRATION_SAMPLES;
        		    	//reset velocity
        		    	currentCalcLinearVelocity[0] = 0;
        		    	currentCalcLinearVelocity[1] = 0;
        		    	currentCalcLinearVelocity[2] = 0;
        		    	
        		    	//initialize gyroscope angle around z axis with the compass average
        		    	currentAnglesGyro[2] = calibrationCompas;
        		    	
        			}

        			calibrationCounter++;
        		}            
          }
          else return;
        }

        
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
    }
}
