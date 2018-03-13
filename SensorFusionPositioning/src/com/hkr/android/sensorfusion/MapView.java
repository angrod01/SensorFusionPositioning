package com.hkr.android.sensorfusion;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;


public class MapView extends View {
	

	final int OFFSET_X = 5; //Epsilon 10;
	final int OFFSET_Y = 5; //Epsilon 210;
	final int INITIAL_X = 400; //Epsilon 1350;
	final int INITIAL_Y = 3000; //Epsilon 1950;
	final int TRACE_SIZE = 1000;
	final float MAP_PROPORTION = 21.8f; //Epsilon 17.1f;
	final float STEP_PATTERN = 55;//cm
	
	private long mRefreshPeriod = 100;
	//control boleans to use/not use the different channels when calculating the merged
	public boolean ChannelWifi = true;
	public boolean ChannelSteps = true;
	public boolean ChannelAcc = false;
	public boolean ChannelLinAcc = true;
	//control booleans to display/not display the different traces
	public boolean showWifi = false;
	public boolean showSteps = false;
	public boolean showAcc = false;
	public boolean showLinAcc = false;
	public boolean showMerged = true;
	
	public int movingAverage = 2;//is half of this number so 2 is no average used
	
	public boolean longPress = false;
	//counters for the acc updates, to update position only every x readings
	private int counterAccUpdates = 0;
	private int counterLinAccUpdates = 0;
	//traces contain the last TRACE_SIZE positions calculated per channels
	private float[] traceMerged;
	public float[] traceWifi;
	private float[] traceWifiAveraged;
	private float[] traceSteps;
	private float[] traceAcc;
	private float[] traceLinAcc;
	
	private float[] movingAverageWifi;
	//map bounds settings
	private Rect mapbounds;
	private Drawable map;
	
	public boolean moving = false;
	

	public MapView(Context context) {
		super(context);
		//initialaze arrays
		traceMerged = new float[TRACE_SIZE];
		traceSteps = new float[TRACE_SIZE];
		traceWifi  = new float[TRACE_SIZE];
		traceWifiAveraged  = new float[TRACE_SIZE];
		traceAcc = new float[TRACE_SIZE];
		traceLinAcc = new float[TRACE_SIZE];
		
		movingAverageWifi= new float[2];
		//initialaze all traces to be at the start position
		resetAllTraces(INITIAL_X, INITIAL_Y);
		
		Resources r = this.getContext().getResources();
		//place map
		//Epsilon map = r.getDrawable(R.drawable.mapepsilon);
		map = r.getDrawable(R.drawable.mapnorrastation);
		//Epsilon mapbounds = new Rect(0, 200, 480, 583);
		mapbounds = new Rect(0, 0, 480, 800);
		map.setBounds(mapbounds);

	}
	
	private RefreshHandler mRedrawHandler = new RefreshHandler();
    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            //MapView.this.update();
            MapView.this.invalidate();
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };
    
    //converts X coordinate from cm to pixels in the screen
    public float mapXFromCmToPixels(float x){
    	//Add Screen offset & convert
    	
    	return OFFSET_X + x*MAP_PROPORTION/100;
    }

    //converts Y coordinate from cm to pixels in the screen
    public float mapYFromCmToPixels(float y){
    	//Add Screen offset & convert
    	
    	return OFFSET_Y + y*MAP_PROPORTION/100;
    }
    
    //converts X coordinate from pixels to cm
    public float mapXFromPixelsToCm(float x){
    	//Add Screen offset & convert
    	
    	return (x - OFFSET_X)/MAP_PROPORTION*100;
    }

    //converts Y coordinate from pixels to cm 
    public float mapYFromPixelsToCm(float y){
    	//Add Screen offset & convert
    	
    	return (y - OFFSET_Y)/MAP_PROPORTION*100;
    }
    
    //resets all the traces to the given posion
    private void resetAllTraces(float x, float y){
		for (int i = 0; i < TRACE_SIZE; i=i+2) {
			traceMerged[i] = x;
			traceMerged[i+1] = y;
			traceSteps[i] = x;
			traceSteps[i+1] = y;
			traceWifi[i] = x;
			traceWifi[i+1] = y;
			traceWifiAveraged[i] = x;
			traceWifiAveraged[i+1] = y;
			traceAcc[i] = x;
			traceAcc[i+1] = y;
			traceLinAcc[i] = x;
			traceLinAcc[i+1] = y;
		}
    }
    
    //calculate distance of a given poit to the current wifi position
    public int distanceToNewWifiPosition(int x, int y){
    	return (int)Math.sqrt(Math.pow((traceWifi[0]-x),2)+ Math.pow((traceWifi[1]-y),2));
    }
    
    //updates the merged position and moves all the trace one place
    private void updateCurrentPositionMerged(float x, float y){

    	System.arraycopy(traceMerged, 0, traceMerged, 2, traceMerged.length-2);
    	traceMerged[0] = x;//(x + traceMerged[2])/2;
    	traceMerged[1] = y;//(y + traceMerged[3])/2;
  
    }
    
    //updates the trace that contains the moveing average of the wifi positions
    private void updateCurrentPositionWifiAverage(float x, float y){

    	System.arraycopy(traceWifiAveraged, 0, traceWifiAveraged, 2, traceWifiAveraged.length-2);
    	traceWifiAveraged[0] = x;
    	traceWifiAveraged[1] = y;
  
    	//add moving average to the merged
    	if(ChannelWifi){
    		if (moving) updateCurrentPositionMerged((float)(traceMerged[0]*0.99 + x*0.01), (float)(traceMerged[1]*0.99 + y*0.01));
    		else updateCurrentPositionMerged((float)(traceMerged[0]*0.95 + x*0.05), (float)(traceMerged[1]*0.95 + y*0.05));
    	}
    }
    
    //updates the trace with the regular wifi estimated position
    public void updateCurrentPositionWifi(float x, float y){
    	
    	System.arraycopy(traceWifi, 0, traceWifi, 2, traceWifi.length-2);
    	traceWifi[0] = x;
    	traceWifi[1] = y;
    	//update moving average
    	movingAverageWifi = Utilities.calculateMovingAverage(traceWifi, 0, movingAverage);
    	updateCurrentPositionWifiAverage(movingAverageWifi[0] , movingAverageWifi[1] );
   	
    }
    
    //updates the trace that contains the stepss with the last estimated step position
    public void updateCurrentPositionSteps(float x, float y){

    	if(moving){
        	System.arraycopy(traceSteps, 0, traceSteps, 2, traceSteps.length-2);
        	traceSteps[0] = x;
        	traceSteps[1] = y;
    	}
    	//add step to the merged
    	if(moving && ChannelSteps){
    		updateCurrentPositionMerged(x, y);//((traceMerged[0] + x)/2, (traceMerged[1] + y)/2);
    	}
  
    }

    //updates the trace that contains the position estimated with the pure acc on Y (minus G) [for trolley only]
    public void updateCurrentPositionAcc(float x, float y){
    	//only add a new trace point every 50 samples
    	counterAccUpdates++;
    	if(counterAccUpdates==50){
    		counterAccUpdates = 0;
    		
        	System.arraycopy(traceAcc, 0, traceAcc, 2, traceAcc.length-2);
        	traceAcc[0] = x;
        	traceAcc[1] = y;
        	//add acc est to the merged
        	if(moving && ChannelAcc){
        		updateCurrentPositionMerged((traceMerged[0] + x)/2, (traceMerged[1] + y)/2);
        	}
    	}
    	else {
    		//update only the head
        	traceAcc[0] = x;
        	traceAcc[1] = y;
    	}
    }    
    
  //updates the trace that contains the position estimated with the linear acceleration
    public void updateCurrentPositionLinAcc(float x, float y){
    	//only add a new trace point every 50 samples
    	counterLinAccUpdates++;
    	if(counterLinAccUpdates==50){
    		counterLinAccUpdates = 0;
    		
        	System.arraycopy(traceLinAcc, 0, traceLinAcc, 2, traceLinAcc.length-2);
        	traceLinAcc[0] = x;
        	traceLinAcc[1] = y;
        	//add lin acc estimation to merged
        	if(moving && ChannelLinAcc){
        		updateCurrentPositionMerged((float)(traceMerged[0]*0.99 + x*0.01), (float)(traceMerged[1]*0.9 + y*0.1));
        	}
    	}
    	else {
    		//update only the head
        	traceLinAcc[0] = x;
        	traceLinAcc[1] = y;
    	}
    }
    
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		switch (event.getActionMasked()) {
		
		case MotionEvent.ACTION_DOWN:
			float pixX = event.getX();
			float pixY = event.getY();
			//update all traces to this point (convert from pix to cm first)
			resetAllTraces(mapXFromPixelsToCm(pixX), mapYFromPixelsToCm(pixY));
			update();
		break;

		case MotionEvent.ACTION_UP:
			//TODO
		break;


		case MotionEvent.ACTION_MOVE:
			//TODO
			break;

		default:
			break;
		}

		return false;
	}
    
    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        // make the entire canvas white
        paint.setColor(Color.TRANSPARENT);
        canvas.drawPaint(paint);
        
        map.draw(canvas);
        //draw wifi trace
        if (showWifi){
            
            paint.setColor(Color.GREEN);
            for (int i = (TRACE_SIZE -1); i >10; i = i-2) {
            	canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[i-1]), mapYFromCmToPixels(traceWifiAveraged[i]), 1, paint);//tail trace
    		}
            canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[8]), mapYFromCmToPixels(traceWifiAveraged[9]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[6]), mapYFromCmToPixels(traceWifiAveraged[7]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[4]), mapYFromCmToPixels(traceWifiAveraged[5]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[2]), mapYFromCmToPixels(traceWifiAveraged[3]), 2, paint); //body
            canvas.drawCircle(mapXFromCmToPixels(traceWifiAveraged[0]), mapYFromCmToPixels(traceWifiAveraged[1]), 3, paint); //head
        }
        //draw steps trace
        if (showSteps){
        	
            paint.setColor(Color.RED);
            for (int i = (TRACE_SIZE -1); i >10; i = i-2) {
            	canvas.drawCircle(mapXFromCmToPixels(traceSteps[i-1]), mapYFromCmToPixels(traceSteps[i]), 1, paint);//tail trace
    		}
            canvas.drawCircle(mapXFromCmToPixels(traceSteps[8]), mapYFromCmToPixels(traceSteps[9]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceSteps[6]), mapYFromCmToPixels(traceSteps[7]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceSteps[4]), mapYFromCmToPixels(traceSteps[5]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceSteps[2]), mapYFromCmToPixels(traceSteps[3]), 2, paint); //body
            canvas.drawCircle(mapXFromCmToPixels(traceSteps[0]), mapYFromCmToPixels(traceSteps[1]), 3, paint); //head

        }
        //draw acc trace
        if (showAcc){
        	
            paint.setColor(Color.YELLOW);
            for (int i = (TRACE_SIZE -1); i >10; i = i-2) {
            	canvas.drawCircle(mapXFromCmToPixels(traceAcc[i-1]), mapYFromCmToPixels(traceAcc[i]), 1, paint);//tail trace
    		}
            canvas.drawCircle(mapXFromCmToPixels(traceAcc[8]), mapYFromCmToPixels(traceAcc[9]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceAcc[6]), mapYFromCmToPixels(traceAcc[7]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceAcc[4]), mapYFromCmToPixels(traceAcc[5]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceAcc[2]), mapYFromCmToPixels(traceAcc[3]), 2, paint); //body
            canvas.drawCircle(mapXFromCmToPixels(traceAcc[0]), mapYFromCmToPixels(traceAcc[1]), 3, paint); //head

        } 
        //draw lin acc trace
        if (showLinAcc){
        	
            paint.setColor(Color.BLUE);
            for (int i = (TRACE_SIZE -1); i >10; i = i-2) {
            	canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[i-1]), mapYFromCmToPixels(traceLinAcc[i]), 1, paint);//tail trace
    		}
            canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[8]), mapYFromCmToPixels(traceLinAcc[9]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[6]), mapYFromCmToPixels(traceLinAcc[7]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[4]), mapYFromCmToPixels(traceLinAcc[5]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[2]), mapYFromCmToPixels(traceLinAcc[3]), 2, paint); //body
            canvas.drawCircle(mapXFromCmToPixels(traceLinAcc[0]), mapYFromCmToPixels(traceLinAcc[1]), 3, paint); //head

        }
        //draw merged trace
        paint.setColor(Color.WHITE);
        if (showMerged){	
            
            for (int i = (TRACE_SIZE -1); i >10; i = i-2) {
            	canvas.drawCircle(mapXFromCmToPixels(traceMerged[i-1]), mapYFromCmToPixels(traceMerged[i]), 1, paint);//tail trace
    		}
            canvas.drawCircle(mapXFromCmToPixels(traceMerged[8]), mapYFromCmToPixels(traceMerged[9]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceMerged[6]), mapYFromCmToPixels(traceMerged[7]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceMerged[4]), mapYFromCmToPixels(traceMerged[5]), 1, paint); //tail
            canvas.drawCircle(mapXFromCmToPixels(traceMerged[2]), mapYFromCmToPixels(traceMerged[3]), 2, paint); //body

        } 
        //draw merged last position (always)
        canvas.drawCircle(mapXFromCmToPixels(traceMerged[0]), mapYFromCmToPixels(traceMerged[1]), 3, paint); //head

    }


    public float getCurrentMergedX() {
		return traceMerged[0];
	}

	public float getCurrentMergedY() {
		return traceMerged[1];
	}
    
    public float getCurrentWifiX() {
		return traceWifi[0];
	}

	public float getCurrentWifiY() {
		return traceWifi[1];
	}
	
    public float getCurrentStepsX() {
		return traceSteps[0];
	}

	public float getCurrentStepsY() {
		return traceSteps[1];
	}
	
    public float getCurrentAccX() {
		return traceAcc[0];
	}

	public float getCurrentAccY() {
		return traceAcc[1];
	}
	
    public float getCurrentLinAccX() {
		return traceLinAcc[0];
	}

	public float getCurrentLinAccY() {
		return traceLinAcc[1];
	}
	
	public void update() {
             mRedrawHandler.sleep(mRefreshPeriod);
    }
}
