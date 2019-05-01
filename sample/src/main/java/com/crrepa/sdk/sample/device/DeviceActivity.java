package com.crrepa.sdk.sample.device;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crrepa.ble.CRPBleClient;
import com.crrepa.ble.conn.CRPBleConnection;
import com.crrepa.ble.conn.CRPBleDevice;
import com.crrepa.ble.conn.bean.CRPAlarmClockInfo;
import com.crrepa.ble.conn.bean.CRPFirmwareVersionInfo;
import com.crrepa.ble.conn.bean.CRPFunctionInfo;
import com.crrepa.ble.conn.bean.CRPFutureWeatherInfo;
import com.crrepa.ble.conn.bean.CRPHeartRateInfo;
import com.crrepa.ble.conn.bean.CRPMovementHeartRateInfo;
import com.crrepa.ble.conn.bean.CRPSleepInfo;
import com.crrepa.ble.conn.bean.CRPStepInfo;
import com.crrepa.ble.conn.bean.CRPTodayWeatherInfo;
import com.crrepa.ble.conn.bean.CRPUserInfo;
import com.crrepa.ble.conn.bean.CRPWatchFaceLayoutInfo;
import com.crrepa.ble.conn.callback.CRPDeviceAlarmClockCallback;
import com.crrepa.ble.conn.callback.CRPDeviceBatteryCallback;
import com.crrepa.ble.conn.callback.CRPDeviceBreathingLightCallback;
import com.crrepa.ble.conn.callback.CRPDeviceDfuStatusCallback;
import com.crrepa.ble.conn.callback.CRPDeviceDominantHandCallback;
import com.crrepa.ble.conn.callback.CRPDeviceFirmwareVersionCallback;
import com.crrepa.ble.conn.callback.CRPDeviceGoalStepCallback;
import com.crrepa.ble.conn.callback.CRPDeviceLanguageCallback;
import com.crrepa.ble.conn.callback.CRPDeviceMetricSystemCallback;
import com.crrepa.ble.conn.callback.CRPDeviceNewFirmwareVersionCallback;
import com.crrepa.ble.conn.callback.CRPDeviceOtherMessageCallback;
import com.crrepa.ble.conn.callback.CRPDeviceQuickViewCallback;
import com.crrepa.ble.conn.callback.CRPDeviceSedentaryReminderCallback;
import com.crrepa.ble.conn.callback.CRPDeviceTimeSystemCallback;
import com.crrepa.ble.conn.callback.CRPDeviceVersionCallback;
import com.crrepa.ble.conn.callback.CRPDeviceWatchFaceLayoutCallback;
import com.crrepa.ble.conn.callback.CRPDeviceWatchFacesCallback;
import com.crrepa.ble.conn.listener.CRPBleConnectionStateListener;
import com.crrepa.ble.conn.listener.CRPBleFirmwareUpgradeListener;
import com.crrepa.ble.conn.listener.CRPBloodOxygenChangeListener;
import com.crrepa.ble.conn.listener.CRPBloodPressureChangeListener;
import com.crrepa.ble.conn.listener.CRPHeartRateChangeListener;
import com.crrepa.ble.conn.listener.CRPPhoneOperationListener;
import com.crrepa.ble.conn.listener.CRPSleepChangeListener;
import com.crrepa.ble.conn.listener.CRPStepChangeListener;
import com.crrepa.ble.conn.listener.CRPWatchFaceSwitchListener;
import com.crrepa.ble.conn.type.CRPBleMessageType;
import com.crrepa.ble.conn.type.CRPDeviceLanguageType;
import com.crrepa.ble.conn.type.CRPDeviceVersionType;
import com.crrepa.ble.conn.type.CRPDominantHandType;
import com.crrepa.ble.conn.type.CRPFirmwareUpgradeType;
import com.crrepa.ble.conn.type.CRPHeartRateType;
import com.crrepa.ble.conn.type.CRPMetricSystemType;
import com.crrepa.ble.conn.type.CRPPastTimeType;
import com.crrepa.ble.conn.type.CRPTimeSystemType;
import com.crrepa.ble.conn.type.CRPWatchFaceLayoutType;
import com.crrepa.ble.conn.type.CRPWatchFacesType;
import com.crrepa.ble.conn.type.CRPWeatherId;
import com.crrepa.sdk.sample.R;
import com.crrepa.sdk.sample.SampleApplication;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by bill on 2017/5/15.
 */

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";
    public static final String DEVICE_MACADDR = "device_macaddr";

    ProgressDialog mProgressDialog;
    CRPBleClient mBleClient;
    CRPBleDevice mBleDevice;
    CRPBleConnection mBleConnection;
    boolean isUpgrade = false;
    @BindView(R.id.tv_connect_state)
    TextView tvConnectState;
    @BindView(R.id.tv_heart_rate)
    TextView tvHeartRate;
    @BindView(R.id.tv_blood_pressure)
    TextView tvBloodPressure;
    @BindView(R.id.btn_ble_connect_state)
    Button btnBleDisconnect;
    @BindView(R.id.tv_blood_oxygen)
    TextView tvBloodOxygen;

    public long tiempo;
    public String medidaActual = "";

    private final int MODO_COMPLETO = 0;
    private final int MODO_SENCILLO = 1;
    private int modoEjecucion = MODO_COMPLETO;

    private String bandFirmwareVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        initView();
        mProgressDialog = new ProgressDialog(this);
        String macAddr = getIntent().getStringExtra(DEVICE_MACADDR);
        if (TextUtils.isEmpty(macAddr)) {
            finish();
            return;
        }

        mBleClient = SampleApplication.getBleClient(this);
        mBleDevice = mBleClient.getBleDevice(macAddr);
        if (mBleDevice != null && !mBleDevice.isConnected()) {
            connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
    }

    void initView() {

    }

    void connect() {
        mProgressDialog.show();
        mBleConnection = mBleDevice.connect();
        mBleConnection.setConnectionStateListener(new CRPBleConnectionStateListener() {
            @Override
            public void onConnectionStateChange(int newState) {
                Log.d(TAG, "onConnectionStateChange: " + newState);
                int state = -1;
                switch (newState) {
                    case CRPBleConnectionStateListener.STATE_CONNECTED:
                        state = R.string.state_connected;
                        mProgressDialog.dismiss();
                        updateTextView(btnBleDisconnect, getString(R.string.disconnect));
                        testSet();
                        break;
                    case CRPBleConnectionStateListener.STATE_CONNECTING:
                        state = R.string.state_connecting;
                        break;
                    case CRPBleConnectionStateListener.STATE_DISCONNECTED:
                        state = R.string.state_disconnected;
                        mProgressDialog.dismiss();
                        updateTextView(btnBleDisconnect, getString(R.string.connect));
                        break;
                }
                updateConnectState(state);
            }
        });


        mBleConnection.setHeartRateChangeListener(mHeartRateChangListener);
        mBleConnection.setBloodPressureChangeListener(mBloodPressureChangeListener);
        mBleConnection.setBloodOxygenChangeListener(mBloodOxygenChangeListener);
    }

    private void testSet() {
        mBleConnection.syncTime();
        mBleConnection.queryPastHeartRate();
        mBleConnection.syncSleep();
    }


    @OnClick(R.id.btn_ble_connect_state)
    public void onConnectStateClick() {
        if (mBleDevice.isConnected()) {
            mBleDevice.disconnect();
        } else {
            mBleDevice.connect();
        }
    }


    @OnClick({
            R.id.btn_start_measure_heartrate,
            R.id.btn_start_measure_pressure,
            R.id.btn_start_measure_bloodoxygen,
            R.id.btn_start_measure_all,
            R.id.btn_stop_measure_all})
    public void onViewClicked(View view) {
        if (!mBleDevice.isConnected()) {
            return;
        }

        tiempo = 0;
        medidaActual="";

        switch (view.getId()) {

            case R.id.btn_start_measure_heartrate:
                this.modoEjecucion = MODO_SENCILLO;
                medidaActual="RATE";
                Log.d(TAG, "PRESS: " + medidaActual);
                mBleConnection.startMeasureOnceHeartRate();
                break;

            case R.id.btn_start_measure_pressure:
                this.modoEjecucion = MODO_SENCILLO;
                medidaActual="PRESSURE";
                Log.d(TAG, "PRESS: " + medidaActual);
                mBleConnection.startMeasureBloodPressure();
                break;

            case R.id.btn_start_measure_bloodoxygen:
                this.modoEjecucion = MODO_SENCILLO;
                medidaActual="OXYGEN";
                Log.d(TAG, "PRESS: " + medidaActual);
                mBleConnection.startMeasureBloodOxygen();
                break;

            case R.id.btn_start_measure_all:
                this.modoEjecucion = MODO_COMPLETO;
                tiempo = System.currentTimeMillis();
                medidaActual="RATE";
                mBleConnection.startMeasureOnceHeartRate();
                break;

            case R.id.btn_stop_measure_all:
                switch (medidaActual){
                    case "RATE":
                        mBleConnection.stopMeasureOnceHeartRate();
                        break;
                    case "PRESSURE":
                        mBleConnection.stopMeasureBloodPressure();
                        break;
                    case "OXYGEN":
                        mBleConnection.stopMeasureBloodOxygen();
                        break;
                }
                break;
        }
    }


    CRPHeartRateChangeListener mHeartRateChangListener = new CRPHeartRateChangeListener() {

        @Override
        public void onMeasuring(int rate) {
            Log.d("PULSERA", "onMeasuring: " + rate);
            updateTextView(tvHeartRate, String.format(getString(R.string.heart_rate), rate));
            Log.d(TAG, "onConnectionStateChange: " + (System.currentTimeMillis() - tiempo )/1000 +" S");
        }

        @Override
        public void onOnceMeasureComplete(int rate) {
            Log.d("PULSERA", "onOnceMeasureComplete: " + rate);
            Log.d(TAG, "onConnectionStateChange: " + (System.currentTimeMillis() - tiempo )/1000 +" S");

            if (modoEjecucion == MODO_COMPLETO) {
                /*int TIME = 3000; // 3000 ms (3 Seconds)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        medidaActual="PRESSURE";
                        Log.d(TAG, "RUNNING: " + medidaActual);
                        mBleConnection.startMeasureBloodPressure();
                    }
                }, TIME);*/

                mBleConnection.stopMeasureOnceHeartRate();
                medidaActual="PRESSURE";
                Log.d(TAG, "RUNNING: " + medidaActual);
                mBleConnection.startMeasureBloodPressure();
            }
        }


        @Override
        public void onMeasureComplete(CRPHeartRateInfo info) {
            Log.d("PULSERA", "onMeasureComplete");
            if (info != null && info.getMeasureData() != null) {
                for (Integer integer : info.getMeasureData()) {
                    Log.d("PULSERA", "onMeasureComplete: " + integer);

                }
            }

        }

        @Override
        public void on24HourMeasureResult(CRPHeartRateInfo info) {
            List<Integer> data = info.getMeasureData();
            Log.d("PULSERA", "on24HourMeasureResult: " + data.size());
        }

        @Override
        public void onMovementMeasureResult(List<CRPMovementHeartRateInfo> list) {
            for (CRPMovementHeartRateInfo info : list) {
                if (info != null) {
                    Log.d("PULSERA", "onMovementMeasureResult: " + info.getStartTime());
                }
            }
        }

    };

    CRPBloodPressureChangeListener mBloodPressureChangeListener = new CRPBloodPressureChangeListener() {
        @Override
        public void onBloodPressureChange(int sbp, int dbp) {
            Log.d("PULSERA", "sbp: " + sbp + ",dbp: " + dbp);
            updateTextView(tvBloodPressure,
                    String.format(getString(R.string.blood_pressure), sbp, dbp));

            if (modoEjecucion == MODO_COMPLETO) {
                medidaActual="OXYGEN";
                mBleConnection.startMeasureBloodOxygen();
            }

        }

    };

    CRPBloodOxygenChangeListener mBloodOxygenChangeListener = new CRPBloodOxygenChangeListener() {
        @Override
        public void onBloodOxygenChange(int bloodOxygen) {
            updateTextView(tvBloodOxygen,
                    String.format(getString(R.string.blood_oxygen), bloodOxygen));
        }
    };






    void updateConnectState(final int state) {
        if (state < 0) {
            return;
        }
        updateTextView(tvConnectState, getString(state));
    }

    void updateTextView(final TextView view, final String con) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(con);
            }
        });
    }

}
