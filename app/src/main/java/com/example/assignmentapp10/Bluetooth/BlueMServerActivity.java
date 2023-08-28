package com.example.assignmentapp10.Bluetooth;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assignmentapp10.MainActivity;
import com.example.assignmentapp10.R;

public class BlueMServerActivity extends Activity  {
    String TAG = "HPBTSERVERMA";
    // 获取到蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    // 用来保存搜索到的设备信息
    private List<String> bluetoothDevices = new ArrayList<String>();
    // ListView组件
    private ListView lvDevices;
    // ListView的字符串数组适配器
    private ArrayAdapter<String> arrayAdapter;
    // UUID，蓝牙建立链接需要的
    private final UUID MY_UUID = UUID
            .fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
    // 为其链接创建一个名称
    private final String NAME = "Bluetooth_Socket";
    // 选中发送数据的蓝牙设备，全局变量，否则连接在方法执行完就结束了
    private BluetoothDevice selectDevice;
    // 获取到选中设备的客户端串口，全局变量，否则连接在方法执行完就结束了
    private BluetoothSocket clientSocket = null;
    // 获取到向设备写的输出流，全局变量，否则连接在方法执行完就结束了

    private InputStream is;// 获取到输入流
    private OutputStream os;// 获取到输出流

    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 1;
    // 服务端利用线程不断接受客户端信息
    private AcceptThread thread;
    private EditText m_send_data;
    private TextView m_connect_status;
    public static final int BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE = 1000;
    public static final int BLUETOOTH_SERVER_RECEIVED_DATA = 1001;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluemas);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        m_send_data = (EditText) findViewById(R.id.send_data);;
        m_connect_status = (TextView) findViewById(R.id.connect_status);

        // 获取到蓝牙默认的适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取到ListView组件
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        // 为listview设置字符换数组适配器
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        // 为listView绑定适配器
        lvDevices.setAdapter(arrayAdapter);
        // 为listView设置item点击事件侦听
        Log.e(TAG,"hello");
        // 用Set集合保持已绑定的设备
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                // 保存到arrayList集合中
                bluetoothDevices.add(bluetoothDevice.getName() + ":"
                        + bluetoothDevice.getAddress() + "\n");
            }
        }
        // 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的
        // 这里注册找到设备和完成搜索广播
        IntentFilter filter = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        thread = new AcceptThread();
        thread.start();
        //receiveFile();

    }

    public void onClick_Return(View view) throws IOException {
        // 在此处编写按钮点击事件的处理逻辑
        Intent[] intents = new Intent[1];
        intents[0] = new Intent(BlueMServerActivity.this, MainActivity.class);
        startActivities(intents);
        if (clientSocket!=null) {
            clientSocket.close();
            finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            File file = new File(uri.getPath());
            try {
                receiveFile(file,is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void onClick_Search(View view) {
        // 点击搜索周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    public void onClick_send(View view)  {
        String text = m_send_data.getText().toString();
        Log.d(TAG,"onClick_send ready to send " + text);
        if (os != null) {
            try {
                os.write(text.getBytes("UTF-8"));
                Log.d(TAG,"successfully onClick_send " + text);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG,"onClick_send Failed "+e.toString());
            }
        }else
            Log.d(TAG,"OutputStream os is null");

    }

    // 注册广播接收者
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // 获取到广播的action
            String action = intent.getAction();
            // 判断广播是搜索到设备还是搜索完成
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // 找到设备后获取其设备
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 设备没有绑定过，则将其保持到arrayList集合中
                    bluetoothDevices.add(device.getName() + ":"
                            + device.getAddress() + "\n");
                    // 更新字符串数组适配器，将内容显示在listView中
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Toast.makeText(BlueMServerActivity.this, "搜索完成", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            // 通过msg传递过来的信息，吐司一下收到的信息
            Log.i(TAG,"recevied message:"+msg.obj);
            switch (msg.what){

                case BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE:
                    int status = (int) msg.arg1;
                    if(status == 0){
                        m_connect_status.setText("客户端已断开");
                        m_connect_status.setTextColor(Color.RED);}
                    else if(status == 1){
                        m_connect_status.setText("等待客户端连接");
                        m_connect_status.setTextColor(Color.GRAY);
                    }
                    else if(status == 2) {
                        m_connect_status.setText("客户端已连接");
                        m_connect_status.setTextColor(Color.GREEN);
                    }

                    Toast.makeText(BlueMServerActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();

                    break;
                case BLUETOOTH_SERVER_RECEIVED_DATA:
                    Toast.makeText(BlueMServerActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    // 服务端接收信息线程
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;// 服务端接口
        private BluetoothSocket socket;// 获取到客户端的接口
        //private InputStream is;// 获取到输入流
        //private OutputStream os;// 获取到输出流
        Message msg = new Message();
        @SuppressLint("MissingPermission")
        public AcceptThread() {
            try {
                // 通过UUID监听请求，然后获取到对应的服务端接口
                serverSocket = mBluetoothAdapter
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                Log.i(TAG,"listenUsingRfcommWithServiceRecord\n");

            } catch (Exception e) {

                // TODO: handle exception
            }
        }

        public void run() {
            while (true) {
                try {
                    Log.i(TAG, "accept,waiting a client... \n");

                    msg = handler.obtainMessage();
                    msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                    msg.obj = "there is a client connected";
                    msg.arg1 = 1;
                    handler.sendMessage(msg);

                    socket = serverSocket.accept();
                    msg = handler.obtainMessage();
                    msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                    msg.obj = "there is a client connected";
                    msg.arg1 = 2;
                    handler.sendMessage(msg);
                    Log.i(TAG, "there is a client connected \n");

                    // 获取到输入流
                    is = socket.getInputStream();
                    // 获取到输出流
                    os = socket.getOutputStream();
                    Log.i(TAG, "accept getInputStream getOutputStream \n");

                    while (true) {
                        byte[] buffer = new byte[128];
                        Log.i(TAG, "reading... ");
                        int count = is.read(buffer);
                        Log.i(TAG, "read count " + count);
                        os.write("OK".getBytes("UTF-8"));
                        // 创建Message类，向handler发送数据
                        //Message msg = new Message();
                        msg = handler.obtainMessage();
                        msg.what = BLUETOOTH_SERVER_RECEIVED_DATA;
                        msg.obj = new String(buffer, 0, count, "utf-8");
                        handler.sendMessage(msg);
                    }
                } catch(Exception e){
                    // TODO: handle exception
                    e.printStackTrace();
                    Log.d(TAG, "read  Thread " + e.toString());
                }
                msg = handler.obtainMessage();
                msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                msg.obj = "the client is disconnected";
                msg.arg1 = 0;
                handler.sendMessage(msg);
            }
        }
    }

    public void receiveFile(File file, InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        FileOutputStream fos = new FileOutputStream(file);
        while ((length = is.read(buffer)) != -1) {
            fos.write(buffer, 0, length);
        }
        fos.close();
    }
}