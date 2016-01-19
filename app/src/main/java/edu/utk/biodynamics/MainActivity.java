/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utk.biodynamics;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import edu.utk.biodynamics.DatabaseUtils.DBUpdateService;
import edu.utk.biodynamics.DatabaseUtils.ECGFileWriter;
import edu.utk.biodynamics.DatabaseUtils.ECGRecord;
import edu.utk.biodynamics.DatabaseUtils.RandomID;

public class MainActivity extends Activity {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "BLE4.0 UART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private boolean connected = false;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private TextView HRView;
    int count = 0;
    int count2 = 0;
    int packetDataCount = 14;
    int ECG_Length = packetDataCount*71;
    short ECG_Data[] = new short[ECG_Length];
    short ECG_HighData[] = new short[ECG_Length];
    short ECG_LowData[] = new short[ECG_Length];
    short QRS_Indexes[] = new short[ECG_Length];
    short ECG_30Sec[] = new short[ECG_Length*2];
    int heartRate = 0;
    int HRCount = 0;
    int HRArray[] = new int[3];
    boolean touching = false;
    int touchingCount = 0;
    ProgressBar touchwaitPB;
    MenuItem connectIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(edu.utk.biodynamics.R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        HRView = (TextView) findViewById(edu.utk.biodynamics.R.id.HR_label);
        touchwaitPB = (ProgressBar) findViewById(R.id.touchwaitPB);
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
         service_init();

        if (findViewById(edu.utk.biodynamics.R.id.ECGgraph) != null) {
            setUpChart();
            findViewById(edu.utk.biodynamics.R.id.ECGgraph).setVisibility(View.VISIBLE);
        }
        for (int i = 0; i <3 ; i++) {
            HRArray[i]=0;
        }
        for (int i = 0; i < ECG_Length; i++) {

            ECG_Data[i] = 138;
            ECG_HighData[i] = 138;
            ECG_LowData[i] = 138;

        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(QrsDetectService.TAG));

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double QRS_IndexData[] = intent.getDoubleArrayExtra("qrsindexes");
            heartRate = (int) intent.getDoubleExtra("heartRate",0);

            if(heartRate<250 && heartRate>40) {
                Log.e("HeartRateCount",String.valueOf(HRCount));
                HRArray[HRCount] = heartRate;
                HRCount++;
                    HRView.setText(String.valueOf(heartRate));
            }

            for (int i = 0; i < QRS_Indexes.length ; i++) {
                QRS_Indexes[i] = (short) QRS_IndexData[i];
            }
            if(HRCount==3){
                save30SecECG();
            }
        }
    };


    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

        }

        public void onServiceDisconnected(ComponentName classname) {
        		mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        
        //Handler events that received from UART service 
        public void handleMessage(Message msg) {
  
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
           //*****����BLE�豸�ɹ��¼�****************// successfully connect bluetooth device 
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                         Log.d(TAG, "UART_CONNECT_MSG");
                         mState = UART_PROFILE_CONNECTED;
                         findViewById(edu.utk.biodynamics.R.id.no_bh_popup).setVisibility(View.GONE);
                         connected=true;
                         connectIcon.setIcon(R.drawable.bt_c);
                     }
            	 });
            }
           
          //*******�Ͽ��豸�����¼�**************// disconnect with the device
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                             Log.d(TAG, "UART_DISCONNECT_MSG");
                             mState = UART_PROFILE_DISCONNECTED;
                             mService.close();
                             findViewById(edu.utk.biodynamics.R.id.no_bh_popup).setVisibility(View.VISIBLE);
                             findViewById(R.id.no_touch_popup).setVisibility(View.GONE);
                            connected=false;
                         connectTask connecttask = new connectTask();
                         connecttask.execute();
                         
                     }
                 });
            }
            
          
          //******�����¼�***************// find available devices
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	 mService.enableTXNotification();
            }
          //******���յ���ݴ���***************//receive data stream
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                findViewById(R.id.no_bh_popup).setVisibility(View.GONE);
                if(touching) {
                    findViewById(edu.utk.biodynamics.R.id.no_touch_popup).setVisibility(View.GONE);
                }else if(!touching){
                    findViewById(edu.utk.biodynamics.R.id.no_touch_popup).setVisibility(View.VISIBLE);
                }
                 final byte[] incomingBytes = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                 final String[] incomingString = new String[14];

                 runOnUiThread(new Runnable() {
                     public void run() {
                         try {

                             if(incomingBytes.length!=18){
                                 for (int i = 0; i < incomingBytes.length ; i++) {
                                     Log.d("IncomingByte",String.valueOf(incomingBytes[i] & 0xff));
                                 }
                                 if(String.valueOf(incomingBytes[1] & 0xff).matches("12")){
                                     if(String.valueOf(incomingBytes[5] & 0xff).matches("64")){
                                         Log.e("SmartECG","Not Touching");
                                         touching = false;
                                         touchingCount=0;
                                         count=0;
                                         count2=0;
                                     }
                                     if(String.valueOf(incomingBytes[5] & 0xff).matches("224")){
                                         Log.e("SmartECG","Touching");
                                         touching = true;
                                         touchwaitPB.setVisibility(View.VISIBLE);
                                     }
                                 }
                             }
                             if(touching){
                                 touchingCount++;
                             }

                             if(incomingBytes.length==18&&touchingCount >=60) {
                                 touchwaitPB.setVisibility(View.GONE);

                                 for (int i = 2; i < 16; i++) {
                                    //incomingString[i-2] = String.valueOf(incomingBytes[i]);
                                    incomingString[i-2] = String.valueOf(incomingBytes[i] & 0xff);
                                 }
                                 setChartData(incomingString);
                             }


                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });
             }
           //******����ʧ��***************// failed to connect
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
            	showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
            }
            
            
        }
    };

    private void setChartData(String[] incomingString) {

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        //Need some function to get byte array text into short array
        if(count>=ECG_Length/incomingString.length) {
            count = 0;
            getHR(ECG_Data);

            if(count2>=ECG_30Sec.length/ECG_Length){
                count2 = 0;

            }

            int n =0;
            for (int i = count2*ECG_Length; i < (count2+1)*ECG_Length ; i++) {

                ECG_30Sec[i] = ECG_Data[n];
                n++;
            }
            count2++;

        }
        int j =0;
        for (int i = count*incomingString.length; i < (count+1)*incomingString.length ; i++) {

            ECG_Data[i] = Short.parseShort(incomingString[j]);
            //Log.e("ChartDataPoint: "+String.valueOf(i),String.valueOf(incomingECG_Data[i]));
            j++;
        }

        count++;


        for (int i = 0; i < ECG_Data.length; i++) {

            yVals1.add(new Entry(ECG_Data[i], i));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals1, "DataSet 1");
        set1.setColor(Color.BLACK);
        set1.setCircleColor(Color.BLACK);
        set1.setLineWidth(1f);
        set1.setDrawCircles(false);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.BLACK);

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < ECG_Data.length; i++) {
            xVals.add((i) + "");
        }

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);

        // set data
        float avgY = data.getYValueSum() / data.getYValCount();
        float maxY = data.getYMax();
        float minY = data.getYMin();
        float maxMinMargin = 0;//(float)((maxY - minY) * 0.07);

        mChart.setData(data);
        mChart.invalidate();
        mChart.getLegend().setEnabled(false);

        //float xLimitLine = 100;//Need limit line value
        //LimitLine limitLine = new LimitLine(xLimitLine);
        //limitLine.setLineWidth(6f);
        //limitLine.setLineColor(0xfff3f3f3);

        YAxis leftAxis = mChart.getAxisLeft();
        if (maxY < 1.2 * avgY && minY > 0.8 * avgY) {
            //leftAxis.setAxisMaxValue(maxY + maxMinMargin);
            //leftAxis.setAxisMinValue(minY);
            leftAxis.setAxisMaxValue(300);
            leftAxis.setAxisMinValue(50);
        }
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawGridLines(false);
        //mChart.getXAxis().removeAllLimitLines();
        //mChart.getXAxis().addLimitLine(limitLine);


    }

    private void save30SecECG() {

        mainTask maintask = new mainTask();
        maintask.execute();

    }

    private class mainTask extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            //toastHandler.sendEmptyMessage(0);
            final ECGRecord newRecord = makeNewRecord();
            sendRecordToDB(newRecord.getId());
            ECGFileWriter.newFile(ECG_30Sec, newRecord.getId(), newRecord.getBasePath());
        }

        @Override
        protected Void doInBackground(Void... params) {


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void success) {

            Intent intent = new Intent(MainActivity.this, ViewRecActivity.class);
            startActivity(intent);

            finish();
        }
    }
    private class connectTask extends AsyncTask<Void, String, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean success = false;
            Log.d("connectTask","Attempting Connection");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String deviceAddress = "C4:99:E6:CF:F4:42";
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

            if(mDevice!=null) {
                success = mService.connect(deviceAddress);
                Log.e("deviceAddress", deviceAddress);
            }else{
                Log.d("connectTask","Device Not Found");
            }


            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

        if(!success){
            connectTask connecttask = new connectTask();
            connecttask.execute();
        }
        }
    }



    private ECGRecord makeNewRecord() {
        final String randomID = RandomID.generateString();
        final String basepath = Environment.getExternalStorageDirectory().getAbsolutePath();

        ECGRecord record = new ECGRecord();
        record.setId(randomID);
        record.setBasePath(basepath);

        return record;

}
    private void sendRecordToDB(String id) {

        int heartRateAVG = (HRArray[0] + HRArray[1] + HRArray[2]) / 3;
        Log.e("HeartRate1",String.valueOf(HRArray[0]));
        Log.e("HeartRate2",String.valueOf(HRArray[1]));
        Log.e("HeartRate3",String.valueOf(HRArray[2]));
        HRView.setText(String.valueOf(heartRateAVG));
        Intent intent = new Intent(MainActivity.this, DBUpdateService.class);
        intent.putExtra("maxHR",heartRateAVG);
        intent.putExtra("recordID", id);
        Log.e("DBUpdate Service","Starting");
        startService(intent);

    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //   THIS IS WHERE DATA GETS SENT TO THE SERVICE
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void getHR(short[] ecg_data) {

        double[] ECG_toHR = new double[ecg_data.length];
        for (int i = 0; i < ecg_data.length; i++) {
            ECG_toHR[i] = ecg_data[i];
        }
        Intent qrsDetIntent = new Intent(MainActivity.this, QrsDetectService.class)
                .putExtra("ECG_Data", ECG_toHR);
        startService(qrsDetIntent);
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
        connectTask connecttask = new connectTask();
        connecttask.execute();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
            unbindService(mServiceConnection);
            mService.stopSelf();
            mService= null;
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 

       
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
               
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                mService.connect(deviceAddress);
                Log.e("deviceAddress", deviceAddress);

            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong request code");
            break;
        }
    }

    
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  
    }

    @Override
    public void onBackPressed() {
            finish();
    }
    
    /*
     * 
     * public static byte[] charToByte(char c) {
        byte[] b = new byte[2];
        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        return b;
    }
byteת��Ϊchar
    public static char byteToChar(byte[] b) {
        char c = (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
        return c;
    }
     */
    
    //�ַ��?��
    public  byte charToNum(char paramChar)
    {
    	byte num=0;
    	switch (paramChar)
    	{
    	case '0':
    		num=0;
    		break;
    	case '1':
    		num=1;
    		break;    		
    	case '2':
    		num=2;
    		break;
    	case '3':
    		num=3;
    		break; 
    	case '4':
    		num=4;
    		break;
    	case '5':
    		num=5;
    		break; 
    	case '6':
    		num=6;
    		break;
    	case '7':
    		num=7;
    		break;    		
    	case '8':
    		num=8;
    		break;
    	case '9':
    		num=9;
    		break; 
    	case 'A':
    		num=10;
    		break;
    	case 'B':
    		num=11;
    		break; 
    	case 'C':
    		num=12;
    		break;
    	case 'D':
    		num=13;
    		break;    		
    	case 'E':
    		num=14;
    		break;
    	case 'F':
    		num=15;
    		break; 
    	case 'a':
    		num=10;
    		break;
    	case 'b':
    		num=11;
    		break; 
    	case 'c':
    		num=12;
    		break;
    	case 'd':
    		num=13;
    		break;    		
    	case 'e':
    		num=14;
    		break;
    	case 'f':
    		num=15;
    		break;     		
    	}

      return num;
    }

    public  char byteToChar(byte paramChar)
    {
    	int adder=(paramChar>>4)&0x0f;
    	String tx=new String("0123456789ABCDEF");
    	return tx.charAt(paramChar);
    }
    
    public void bytesToHex(byte[] txValue)//BLE���յ�HEX��ݴ��� //received HEX data from bluetooth
    {
    	char [] ch={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F',};
    	int len=txValue.length;
    	byte[] Rdata=new byte[len];
        String hexArray[] = new String[len];
        String tx;

    	for(int i=0;i<len;i++)
 			Rdata[i]=txValue[i];
 		
 		for(int i=0;i<len;i++)
 		{
            tx=new String();
 		    tx+=ch[(Rdata[i]>>4)&0x0f];   //��λ //High
 		    tx+=ch[(Rdata[i]&0x0f)]; //��λ	 //Low

            hexArray[i] = tx;
            //Log.e("hexArray["+String.valueOf(i)+"]",hexArray[i]);

 		}

        hexToShort(len, hexArray);

// 		messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
    }

    private void hexToShort(int len, String[] hexArray) {

        short incomingECG_Data[] = new short[14];

        if (hexArray[0].matches("F0") && hexArray[1].matches("0F")){
            //Log.e("hexToShort","Inside If Statement");
            for (int i = 2; i < 16; i++) {
                incomingECG_Data[i-2] = (short)Integer.parseInt(hexArray[i],16);
                //Log.e("Index",String.valueOf(i-2));
              //  Log.e("incomingECG_Data["+String.valueOf(i)+"]", String.valueOf(incomingECG_Data[i]));
            }
        }
        //setChartData(incomingECG_Data);
    }

       public void btSendHex(String message)//�����ʹ���1 //Send command 1
    {
    	String sb_copy = new String(message);
    	int len = sb_copy.length();
    	int j=0;
    	char [] ch=new char [len];
    	for(int i=0;i<len;i++)
    	{
    		if(sb_copy.charAt(i)!=' ')
    		{
    			ch[j]=sb_copy.charAt(i);
    			j++;
    		}
    	}   	
    	
    	byte[] value=new byte[j/2];
    	for(int i=0;i<j/2;i++)
    	{	
    		int k=i*2;
    		value[i]=(byte)((charToNum(ch[k])&0x0f)<<4);
    		value[i]|=(byte)(charToNum(ch[k+1])&0x0f);
    	}
		mService.writeRXCharacteristic(value);	
		
		//ת��������ʾ //Convert the format for display
		
		
    } 
    
    public void hexStringToStr()
    {

    }

    LineChart mChart;

    public void setUpChart() {
        mChart = (LineChart) findViewById(edu.utk.biodynamics.R.id.ECGgraph);
        //mChart.setOnChartGestureListener(this);
        //mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);

        // no description text
        mChart.setDescription("");
        mChart.setNoDataTextDescription("");
        mChart.setNoDataText("");
        // enable touch gestures
        mChart.setTouchEnabled(false);

        // enable scaling and dragging
        mChart.setDragEnabled(false);
        //mChart.setScaleEnabled(true);
        mChart.setScaleXEnabled(false);
        mChart.setScaleYEnabled(false);


        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);
        mChart.getXAxis().setDrawLabels(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getXAxis().setDrawAxisLine(false);
        mChart.getAxisLeft().setEnabled(false);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(edu.utk.biodynamics.R.menu.main, menu);
        connectIcon = menu.findItem(R.id.connectIcon);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int selected = item.getItemId();
        switch(selected) {
            //connect selected
            case edu.utk.biodynamics.R.id.connectIcon:
                    if (!mBtAdapter.isEnabled()) {
                        Log.i(TAG, "onClick - BT not enabled yet");
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    } else {
                        if (!connected) {
                            Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        } else if(connected) {
                            //Disconnect button pressed
                            if (mDevice != null) {
                                mService.disconnect();
                            }
                        }
                    }

                break;
            case R.id.recordsClick:
                Intent intent = new Intent(MainActivity.this, ViewRecActivity.class);
                startActivity(intent);
        }
        return false;
    }

    public void connectToBH(View view){
        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
    }

}
/*
To read heart rate data:
Phone -> Sensor: 0xF0 0xC0 CKSUM
Sensor -> Phone: 0xF0 0xC0 XLH XLL CKSUM
XLH: high byte for heart rate
XLL: low byte for heart rate

To read ECG data:
Phone -> Sensor: 0xF0 0xC7 CKSUM
Sensor -> Phone: 0xF0 0xC0 ZQH ZQL CKSUM
Will send one segment of data every heartbeat.
ZQH: high byte
ZQL: low byte

To terminate data transmission
Phone -> Sensor: 0xF0 0XC1 CKSUM
Sensor -> Phone: 0xF0 0XC1 CKSUM

Read the device ID.
Phone -> sensor:  0xF0 0xC2 CKSUM
Sensor -> Phone: 0xF0 0xC2 SN0 SN1 SN2 SN3 CKSUM
SN0 SN1 SN2 SN3: are the device ID

Read the date of manufactory.
Phone -> Sensor: 0xF0 0xC3 CKSUM
Sensor -> Phone: 0xF0 0xC3 T0 T1 T2 T3 CKSUM
T0, T1: year
T2: Month
T3: date

 */
