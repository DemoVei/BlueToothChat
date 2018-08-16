package com.example.administrator.bluetoothchat_2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "ChartActivity";
    private Button mOpenBtn;
    private Button mCloseBtn;
    private Button mSearchBtn;
    private Button mSendBtn;
    private EditText mContentEt;
    private TextView mDeviceTv;
    private ListView mList;
    private List<BluetoothDevice> mDevices;
    private DeviceListAdapter mAdapter;

    private LoadingAlertDialog mLoadingDialog;
    //线程类的实例
    private BlueToothUtils.AcceptThread ac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews(){
        mOpenBtn =  findViewById(R.id.openBtn);
        mCloseBtn =  findViewById(R.id.closeBtn);
        mSearchBtn =  findViewById(R.id.searchBtn);
        mSendBtn =  findViewById(R.id.sendBtn);
        mContentEt =  findViewById(R.id.contentET);
        mDeviceTv =  findViewById(R.id.deviceTV);
        mList =  findViewById(R.id.deviceList);


        mOpenBtn.setOnClickListener(this);
        mCloseBtn.setOnClickListener(this);
        mSearchBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);

        BlueToothUtils.getInstance().setContext(this);

        mDevices = BlueToothUtils.getInstance().getBondedDevices();
        mAdapter = new DeviceListAdapter(this, mDevices);
        mList.setAdapter(mAdapter);

        mLoadingDialog = new LoadingAlertDialog(this);

        //启动服务
        ac = BlueToothUtils.getInstance().getAc();
        ac.start();

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = mDevices.get(i);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    showToast("去配对");
                    BlueToothUtils.getInstance().createBond(device);
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    BlueToothUtils.getInstance().connectDevice(device);
                    mDeviceTv.setText("当前设备:" + device.getName());
                }
            }
        });

        if (!BlueToothUtils.getInstance().isEnabled()) {
            BlueToothUtils.getInstance().openBlueTooth();
        }

        /**
         * 异步搜索蓝牙设备——广播接收
         */
        IntentFilter filter = new IntentFilter();
        // 找到设备的广播
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        // 搜索完成的广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 注册广播
        registerReceiver(receiver, filter);


    }

    // 广播接收器
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            // 收到的广播类型
            String action = intent.getAction();
            // 发现设备的广播
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 从intent中获取设备
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 判断是否配对过
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (!isExistDevice(device)) {
                        mDevices.add(device);
                    }
                }
                Log.e(TAG, "add:" + device.getName());
                // 搜索完成
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mAdapter.setItems(mDevices);
                mLoadingDialog.dismiss();
                showToast("搜索完成！");
            }
        }
    };

    private boolean isExistDevice(BluetoothDevice device) {
        for (BluetoothDevice d : mDevices) {
            if (d.getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.openBtn:
                toOpen();
                break;
            case R.id.closeBtn:
                toClose();
                break;
            case R.id.searchBtn:
                toSearch();
                break;
            case R.id.sendBtn:
                toSend();
                break;
        }
    }

    private void toOpen() {
        BlueToothUtils.getInstance().openBlueTooth();
    }

    private void toClose() {
        BlueToothUtils.getInstance().closeBlueTooth();
    }

    private void toSend() {
        String message = mContentEt.getText().toString();
        if (!BlueToothUtils.getInstance().isEnabled()) {
            showToast("蓝牙未打开!");
            return;
        }
        if (BlueToothUtils.getInstance().getCurDevice() != null) {
            if (!TextUtils.isEmpty(message)) {
                BlueToothUtils.getInstance().write(message);
                mContentEt.setText("");
            } else {
                showToast("输入你要发送的内容!");
            }
        } else {
            showToast("选择设备!");
        }
    }

    private void toSearch() {
        mDevices = BlueToothUtils.getInstance().getBondedDevices();
        if (BlueToothUtils.getInstance().isEnabled()) {
            BlueToothUtils.getInstance().searchDevices();
            mLoadingDialog.show("正在搜索...");
        } else {
            showToast("蓝牙未打开!");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
