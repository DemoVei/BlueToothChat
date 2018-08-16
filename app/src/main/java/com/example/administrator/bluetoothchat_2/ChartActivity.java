package com.example.administrator.bluetoothchat_2;

import android.annotation.TargetApi;
        import android.app.Activity;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothServerSocket;
        import android.bluetooth.BluetoothSocket;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.os.Build;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.util.Log;
        import android.view.View;
        import android.widget.AdapterView;
        import android.widget.AdapterView.OnItemClickListener;
        import android.widget.ArrayAdapter;
        import android.widget.EditText;
        import android.widget.ListView;
        import android.widget.Toast;

        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.Set;
        import java.util.UUID;

public class ChartActivity extends Activity implements OnItemClickListener {

    private static final String TAG = "MainActivity";
    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    // 列表
    private ListView lvDevices;
    // 存储搜索到的蓝牙
    private List<String> bluetoothDevices = new ArrayList<String>();
    // listview的adapter
    private ArrayAdapter<String> arrayAdapter;
    // UUID.randomUUID()随机获取UUID
    private final UUID MY_UUID = UUID
            .fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
    // 连接对象的名称
    private final String NAME = "LGL";

    // 这里本身即是服务端也是客户端，需要如下类
    private BluetoothSocket clientSocket;
    private BluetoothDevice device;
    // 输出流_客户端需要往服务端输出
    private OutputStream os;
    //线程类的实例
    private AcceptThread ac;
    private EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        initView();
    }

    private void initView() {

        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
            // 不做提示，强行打开
//             mBluetoothAdapter.enable();
        }
        et = (EditText) findViewById(R.id.et);
        // 初始化listview
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        lvDevices.setOnItemClickListener(this);

        // 获取已经配对的设备
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();

        // 判断是否有配对过的设备
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // 遍历到列表中
                bluetoothDevices.add("已配对\n" + device.getName() + ":"
                        + device.getAddress());
            }
        }

        // adapter
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);

        //启动服务
        ac = new AcceptThread();
        ac.start();

        /**
         * 异步搜索蓝牙设备——广播接收
         */
        // 找到设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // 注册广播
        registerReceiver(receiver, filter);
        // 搜索完成的广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 注册广播
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public void btnSearch(View v) {
        // 设置进度条
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在搜索...");
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 开始搜索
        mBluetoothAdapter.startDiscovery();
    }

    // 广播接收器
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

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
                    // 添加到列表
                    bluetoothDevices.add("未配对\n" + device.getName() + ":"
                            + device.getAddress());
                    arrayAdapter.notifyDataSetChanged();
                }
                // 搜索完成
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                // 关闭进度条
                setProgressBarIndeterminateVisibility(true);
                setTitle("搜索完成！");
            }
        }
    };

    // 客户端
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        // 先获得蓝牙的地址和设备名
        String s = arrayAdapter.getItem(position);
        // 单独解析地址
        String address = s.substring(s.indexOf(":") + 1).trim();

        // 主动连接蓝牙
        try {
            // 判断是否在搜索,如果在搜索，就取消搜索
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            try {
                // 获得远程设备
                device = mBluetoothAdapter.getRemoteDevice(address);
                Log.e(TAG, "device:" + device);

                clientSocket = device
                        .createRfcommSocketToServiceRecord(MY_UUID);
                // 连接
                clientSocket.connect();
                // 获得输出流
                os = clientSocket.getOutputStream();
               /* // 判断是否可以获得
                if (device == null) {
                    // 获得远程设备
                    device = mBluetoothAdapter.getRemoteDevice(address);
                    Log.e(TAG, "device:" + device);
                }
                // 开始连接
                if (clientSocket == null) {
                    clientSocket = device
                            .createRfcommSocketToServiceRecord(MY_UUID);
                    // 连接
                    clientSocket.connectDevice();
                    // 获得输出流
                    os = clientSocket.getOutputStream();
                }*/
            } catch (Exception e) {
                // TODO: handle exception
            }
            // 如果成功获得输出流
            if (os != null) {
//                os.write("Hello Bluetooth!".getBytes());
                os.write(et.getText().toString().getBytes("GBK"));
                Log.e(TAG, "write");
                et.setText("");
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    // 服务端，需要监听客户端的线程类
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(ChartActivity.this, String.valueOf(msg.obj),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, msg.obj + ":服务端");
            super.handleMessage(msg);
        }
    };

    // 线程服务类
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        // 输入 输出流
        private OutputStream os;
        private InputStream is;

        public AcceptThread() {
            try {
                serverSocket = mBluetoothAdapter
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // 截获客户端的蓝牙消息
            try {
                socket = serverSocket.accept(); // 如果阻塞了，就会一直停留在这里
                is = socket.getInputStream();
                os = socket.getOutputStream();
                // 不断接收请求,如果客户端没有发送的话还是会阻塞
               /* while (true) {
                    // 每次只发送128个字节
                    byte[] buffer = new byte[128];
                    // 读取
                    int count = is.read();
                    Log.e(TAG, "count:" + count);
                    // 如果读取到了，我们就发送刚才的那个Toast
                    Message msg = new Message();
                    msg.obj = String.valueOf((char)count);   //new String(buffer, 0, count, "GBK");
                    Log.e(TAG, msg.obj + ":客户端");
                    handler.sendMessage(msg);
                }*/
                while (true) {
                    synchronized (ChartActivity.this) {
                        byte[] tt = new byte[is.available()];
                        if (tt.length > 0) {
                            is.read(tt, 0, tt.length);
                            Message msg = new Message();
                            msg.obj = new String(tt, "GBK");
                            Log.e(TAG, msg.obj + ":客户端");
                            handler.sendMessage(msg);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

}
