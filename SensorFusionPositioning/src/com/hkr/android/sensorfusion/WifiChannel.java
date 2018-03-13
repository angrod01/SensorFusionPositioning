package com.hkr.android.sensorfusion;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WifiChannel implements Runnable {

	public Handler handler;
	public Context myContext;
	public WifiManager wifi;
	public WifiLock lock;
	public BroadcastReceiver receiver;		

	public WifiChannel(Context context, Handler handler) {
		super();
		this.myContext = context;
		this.handler = handler;
		this.wifi = (WifiManager) myContext.getSystemService(Context.WIFI_SERVICE);
		this.lock = this.wifi.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "scan");
		this.lock.acquire();
	}

    class WifiReceiver extends BroadcastReceiver {
    	public WifiReceiver(WifiManager wifi) {
			super();
			this.wifi = wifi;
		}

		public WifiManager wifi;
    	
        @Override
		public void onReceive(Context c, Intent intent) {
			Message msg = handler.obtainMessage();
    	    msg.what = 1;

        	List <ScanResult> resultList = wifi.getScanResults();
        	
    	    Bundle bundle = new Bundle();
    	    bundle.putInt("size", resultList.size());
    	    String[] bssids = new String[resultList.size()];
    	    String[] ssids = new String[resultList.size()];
    	    int[] levels = new int[resultList.size()];
            for(int i = 0; i < resultList.size(); i++){
            	bssids[i] = resultList.get(i).BSSID.toString();
            	ssids[i] = resultList.get(i).SSID.toString();
            	levels[i] = resultList.get(i).level;
            }
            bundle.putStringArray("bssids", bssids);
            bundle.putStringArray("ssids", ssids);
            bundle.putIntArray("levels", levels);
            msg.setData(bundle);
            handler.sendMessage(msg);
            this.wifi.startScan();
        }
    }

	@Override
	public void run() {
        try {
        	WifiReceiver receiverWifi = new WifiReceiver(this.wifi);
            myContext.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        		wifi.startScan();
        		Thread.sleep(1000);
        } catch (Exception e) {
        	Log.e("WiFi", "Error", e);
            e.printStackTrace();
        }
	}
}
