package com.crrepa.sdk.sample.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crrepa.ble.CRPBleClient;
import com.crrepa.ble.conn.CRPBleDevice;
import com.crrepa.ble.conn.listener.CRPBleFirmwareUpgradeListener;
import com.crrepa.ble.scan.bean.CRPScanDevice;
import com.crrepa.ble.scan.callback.CRPScanCallback;
import com.crrepa.sdk.sample.R;
import com.crrepa.sdk.sample.SampleApplication;
import com.crrepa.sdk.sample.device.DeviceActivity;

import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final int SCAN_PERIOD = 10 * 1000;
    @BindView(R.id.scan_toggle_btn)
    Button scanToggleBtn;
    @BindView(R.id.scan_results)
    RecyclerView scanResults;
    @BindView(R.id.tv_firmware_fix_state)
    TextView tvFirmwareFixState;

    private CRPBleClient mBleClient;
    private ScanResultsAdapter mResultsAdapter;
    private boolean mScanState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        mBleClient = SampleApplication.getBleClient(this);

        configureResultList();

        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        Log.i("ConnectedDevices: ", "Processing list");
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice d: pairedDevices) {
                String deviceName = d.getName();
                String macAddress = d.getAddress();
                Log.i("ConnectedDevices: ", "paired device: " + deviceName + " at " + macAddress);
                // do what you need/want this these list items

                /*if ("EA:0D:61:5A:16:B1".equals(macAddress)) {
                    fastEnter(macAddress);
                }*/
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelScan();
    }

    @OnClick({R.id.scan_toggle_btn, R.id.btn_firmware_fix})
    public void onViewClicked(View view) {
        if (!mBleClient.isBluetoothEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }

        switch (view.getId()) {
            case R.id.scan_toggle_btn:
                if (mScanState) {
                    cancelScan();
                } else {
                    startScan();
                }
                break;
            case R.id.btn_firmware_fix:
                mBleClient.fixDeviceOfUpgrade("",
                        "", mFirmwareUpgradeListener);
                break;
        }

    }

    private void startScan() {
        boolean success = mBleClient.scanDevice(new CRPScanCallback() {
            @Override
            public void onScanning(final CRPScanDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mResultsAdapter.addScanResult(device);
                    }
                });
            }

            @Override
            public void onScanComplete(List<CRPScanDevice> results) {
                if (mScanState) {
                    mScanState = false;
                    updateButtonUIState();
                }
            }
        }, SCAN_PERIOD);
        if (success) {
            mScanState = true;
            updateButtonUIState();
            mResultsAdapter.clearScanResults();
        }
    }

    private void cancelScan() {
        mBleClient.cancelScan();
    }



    private void configureResultList() {
        scanResults.setHasFixedSize(true);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        scanResults.setLayoutManager(recyclerLayoutManager);
        mResultsAdapter = new ScanResultsAdapter();
        scanResults.setAdapter(mResultsAdapter);
        mResultsAdapter.setOnAdapterItemClickListener(new ScanResultsAdapter.OnAdapterItemClickListener() {
            @Override
            public void onAdapterViewClick(View view) {
                final int childAdapterPosition = scanResults.getChildAdapterPosition(view);
                final CRPScanDevice itemAtPosition = mResultsAdapter.getItemAtPosition(childAdapterPosition);
                onAdapterItemClick(itemAtPosition);
            }
        });
    }

    private void fastEnter(String macAddress) {
        mBleClient.cancelScan();

        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.DEVICE_MACADDR, macAddress);
        startActivity(intent);
    }

    private void onAdapterItemClick(CRPScanDevice scanResults) {
        final String macAddress = scanResults.getDevice().getAddress();
        mBleClient.cancelScan();

        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.DEVICE_MACADDR, macAddress);
        startActivity(intent);
    }


    private void updateButtonUIState() {
        scanToggleBtn.setText(mScanState ? R.string.stop_scan : R.string.start_scan);
    }

    CRPBleFirmwareUpgradeListener mFirmwareUpgradeListener = new CRPBleFirmwareUpgradeListener() {
        @Override
        public void onFirmwareDownloadStarting() {
            Log.d(TAG, "onFirmwareDownloadStarting");
            updateTextView(tvFirmwareFixState, getString(R.string.dfu_status_download_starting));
        }

        @Override
        public void onFirmwareDownloadComplete() {
            Log.d(TAG, "onFirmwareDownloadComplete");
            updateTextView(tvFirmwareFixState, getString(R.string.dfu_status_download_complete));
        }

        @Override
        public void onUpgradeProgressStarting() {
            Log.d(TAG, "onUpgradeProgressStarting");
            updateTextView(tvFirmwareFixState, getString(R.string.dfu_status_starting));
        }

        @Override
        public void onUpgradeProgressChanged(int percent, float speed) {
            Log.d(TAG, "onUpgradeProgressChanged: " + percent);
            String status = String.format(getString(R.string.dfu_status_uploading_part), percent);
            updateTextView(tvFirmwareFixState, status);
        }

        @Override
        public void onUpgradeCompleted() {
            Log.d(TAG, "onUpgradeCompleted");
            updateTextView(tvFirmwareFixState, getString(R.string.dfu_status_completed));
        }

        @Override
        public void onUpgradeAborted() {
            Log.d(TAG, "onUpgradeAborted");
            updateTextView(tvFirmwareFixState, getString(R.string.dfu_status_aborted));
        }

        @Override
        public void onError(int errorType, String message) {
            Log.d(TAG, "onError: " + errorType);
            updateTextView(tvFirmwareFixState, message);
        }
    };

    void updateTextView(final TextView view, final String con) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(con);
            }
        });
    }


}
