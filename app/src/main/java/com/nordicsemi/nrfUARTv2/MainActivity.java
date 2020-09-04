
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//http://blog.naver.com/PostView.nhn?blogId=skyvvv624&logNo=221062100445#

package com.nordicsemi.nrfUARTv2;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.nordicsemi.nrfUARTv2.UartService;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.*;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import java.util.HashMap;


public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    //BLE 통신 변수
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "BLE Test";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    //화면 구성 변수
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect; //btnSend
    private Button btnSend;
    private EditText setValue;
    private LineChart chart;

    //경고음 재생 변수
    private SoundPool mSoundPool;
    int mStreamId;
    int soundValue=1000;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        chart = (LineChart) findViewById(R.id.chart); // chart 연결
        chart.getDescription().setEnabled(false);

        // 차트의 아래 Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);    // xAxis의 위치는 아래쪽
        xAxis.setTextSize(10);                            // xAxis에 표출되는 텍스트의 크기는 10
        xAxis.setDrawGridLines(false);                    // xAxis의 그리드 라인을 없앰

        // 차트의 왼쪽 Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);                // leftAxis의 그리드 라인을 없앰

        leftAxis.setLabelCount(5); //화면에 표시될 y축 숫자 개수 설정
        leftAxis.setAxisMaxValue(1000); //y축 최댓값 설정
        leftAxis.setAxisMinValue(-1000); //y축 최솟값 설정

       // 차트의 오른쪽 Axis
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);                    // rightAxis를 비활성화 함

        LineData data = new LineData();
        chart.setData(data);                            // LineData를 셋팅함

        messageListView = (ListView) findViewById(R.id.listMessage); //ListView 연결
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);

        btnConnectDisconnect = (Button) findViewById(R.id.btn_select); //button 연결
        btnSend = (Button) findViewById(R.id.btn_send);
        setValue = (EditText) findViewById(R.id.inputValue);

        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
        //경고 값 설정 후 키보드 및 커서 숨기기
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundValue = Integer.parseInt(setValue.getText().toString());
                hideKeyboard();
                setValue.setCursorVisible(false);
            }
        });
        setValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setValue.setCursorVisible(true);
            }
        });
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(setValue.getWindowToken(), 0);
    }
    private ILineDataSet createSet() { //graph chart 생성
        LineDataSet set = new LineDataSet(null, mDevice.getName());    // 데이터셋의 이름 설정(기본 데이터는 null)
        set.setLineWidth(2);                                        // 라인의 두께 설정
        set.setCircleRadius(4);                                    // 데이터 점의 반지름을 설정
        set.setCircleColor(Color.parseColor("#FFA1B4DC"));  // 데이터의 점 설정
        set.setDrawCircleHole(true);
        set.setDrawCircles(true);
        set.setDrawHorizontalHighlightIndicator(false);
        set.setDrawHighlightIndicators(false);
        set.setDrawValues(false);
        set.setColor(Color.parseColor("#3F51B5"));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);            // Axis를 YAxis의 LEFT를 기본으로 설정
        return set;
    }

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
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };
    //핸들러 생성
    private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    //경고음 재생에 필요한 soundpool 생성 및 설정
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void playMusic() {
        final SoundPool mSoundPool = new SoundPool.Builder().build(); // 생성
        mSoundPool.load(this, R.raw.beep, 1); // 로딩 //음악 파일을 변경하고 싶으면 --> R.raw.음악파일명 으로 변경
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    mStreamId = mSoundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f); // 실행
            }
        });
    }
    public void stopMusic () {
        // stop 메소드 사용시 streamId가 필요
        // streamId는 play 메소드 호출 시 반환
        mSoundPool.stop(mStreamId);
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //장치와 연결이 되면 실행 ACTION_GATT_CONNECTED
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");

                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName());
                        Toast.makeText(getApplicationContext(), "Connected to: " + mDevice.getName(), Toast.LENGTH_LONG).show();
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //장치와 연결이 끊어지면 실행 ACTION_GATT_CONNECTED
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");

                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        Toast.makeText(getApplicationContext(), "Disconnected to: " + mDevice.getName(), Toast.LENGTH_LONG).show();
                        //listAdapter.add(currentDateTimeString + " Disconnected to: " + mDevice.getName());
                        //listAdapter.notifyDataSetChanged();
                        //messageListView.setSelection(listAdapter.getCount()-1);
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();

                    }
                });
            }

            //장치의 Service가 발견되면 실행 ACTION_GATT_SERVICES_DISCOVERED
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //Read 할 데이터가 발견되면 실행 ACTION_DATA_AVAILABLE
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);

               for (int i = 0; i < txValue.length / 2; i++) { // 바이트 배열 순서 변경 big endian - little endian
                    byte temp = txValue[i];
                    txValue[i] = txValue[txValue.length - i - 1];
                    txValue[txValue.length - i - 1] = temp;
                }

                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    public void run() {
                        try {
                            int intValue = ((txValue[0] & 0xff) << 24) | ((txValue[1] & 0xff) << 16) |
                                    ((txValue[2] & 0xff) << 8)  | (txValue[3] & 0xff); //음수를 읽기 위해 배열 변경

                            //화면에 출력될 형식 지정 및 출력
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            listAdapter.add("   " + currentDateTimeString+"           "+String.format("%6d", intValue)+"V"+"           0x"+String.format("%06X",intValue));
                            //스크롤바를 맨 아래로 설정
                            listAdapter.notifyDataSetChanged(); //listadapter의 변동을 감지함
                            messageListView.setSelection(listAdapter.getCount()-1);

                            LineData data = chart.getData();    // onCreate에서 생성한 LineData를 가져옴
                            if (data != null)                    // 데이터가 비어있지 않으면
                            {
                                ILineDataSet set = data.getDataSetByIndex(0);    // 0번째 위치의 데이터셋을 가져옴

                                if (set == null)                    // 0번에 위치한 값이 없으면
                                {
                                    set = createSet();            // createSet을 함
                                    data.addDataSet(set);        // createSet을 한 set을 데이터셋에 추가함
                                }

                                data.addEntry(new Entry(set.getEntryCount(), intValue), 0);
                                data.notifyDataChanged();        // data의 값 변동을 감지함

                                chart.notifyDataSetChanged();                // chart의 값 변동을 감지함
                                chart.setVisibleXRangeMaximum(20);            // chart에서 최대 X좌표기준으로 몇개의 데이터를 보여줄지 설정함
                                chart.moveViewToX(data.getEntryCount());    // 가장 최근에 추가한 데이터의 위치로 chart를 이동함
                            }

                            if ( Math.abs(intValue) >= soundValue) { // 설정 값 보다 측정 값이 높으면 경고음 실행
                                playMusic();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            //UART를 지원하지 않으면 실행 DEVICE_DOES_NOT_SUPPORT_UART
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init() { //서비스 초기화
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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        stopMusic();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
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
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);

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

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("BLE Test is running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Quit BLE Test")
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}

