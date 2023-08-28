package com.example.assignmentapp10.Bluetooth;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assignmentapp10.MainActivity;
import com.example.assignmentapp10.Maps.MapsActivity;
import com.example.assignmentapp10.R;

public class BlueMActivity extends Activity implements OnItemClickListener {
    String TAG = "HPBMainActivity";
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
    private BluetoothDevice selectDevice = null;
    // 获取到选中设备的客户端串口，全局变量，否则连接在方法执行完就结束了
    private BluetoothSocket clientSocket = null;
    // 获取到向设备写的输出流，全局变量，否则连接在方法执行完就结束了
    private OutputStream os;
    // 服务端利用线程不断接受客户端信息
    private EditText m_send_data;
    private ReadThread mReadThread;
    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 1;//请求码

    public static final int BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE = 1000;
    public static final int BLUETOOTH_CLIENT_RECEIVED_DATA = 1001;
    public int received_data_line = 0;

    private TextView m_connect_status , m_received_data;
    public StringBuffer sb = new StringBuffer();
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluemac);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        m_send_data = (EditText) findViewById(R.id.send_data);
        m_connect_status = (TextView) findViewById(R.id.connect_status);
        m_connect_status.setText("当前状态:"+"断开");
        m_received_data = (TextView) findViewById(R.id.received_data);
        m_received_data.setMovementMethod(ScrollingMovementMethod.getInstance());
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
        lvDevices.setOnItemClickListener(this);
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
        Log.e(TAG, "Client onCreate");
    }

    @SuppressLint("MissingPermission")
    public void onClick_Search(View view) {
        setTitle("正在扫描...");
        // 点击搜索周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    public void onClick_send(View view) throws IOException {
        if (os != null) {
            try {
                String text = m_send_data.getText().toString();
                os.write(text.getBytes("UTF-8"));
                Toast.makeText(BlueMActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                clientSocket.close();
                clientSocket = null;
                os.close();
                os = null;
                Log.e(TAG, "onClick_send "+ e.toString());
            }
        }
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
                // 判断这个设备是否是之前已经绑定过了，如果是则不需要添加，在程序初始化的时候已经添加了

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 设备没有绑定过，则将其保持到arrayList集合中
                    bluetoothDevices.add(device.getName() + ":"
                            + device.getAddress() + "\n");
                    // 更新字符串数组适配器，将内容显示在listView中
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                //setTitle("搜索完成");
                Toast.makeText(BlueMActivity.this, "搜索完成", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream;
            Log.d(TAG, "start ReadThread");

            while (true) {
                try {
                    inputStream = clientSocket.getInputStream();
                    Log.d(TAG, "waiting to reading....");
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.what = BLUETOOTH_CLIENT_RECEIVED_DATA;
                        msg.obj = s;
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    Message msg = new Message();
                    msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                    msg.arg1 = 0;
                    msg.obj = "Failed to connect the server," + e.toString();
                    handler.sendMessage(msg);
                    Log.d(TAG, e.toString());
                    break;
                }
            }

            if (clientSocket != null) {
                try {
                    clientSocket.close();
                    clientSocket = null;
                } catch (IOException e) {
                    Log.d("TAG", e.toString());
                }
            }
        }
    }

    // 点击listView中的设备，传送数据
    @SuppressLint("MissingPermission")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        // 获取到这个设备的信息
        String s = arrayAdapter.getItem(position);
        // 对其进行分割，获取到这个设备的地址
        String address = s.substring(s.indexOf(":") + 1).trim();
        Message msg = new Message();
        Log.d(TAG, "address:" + address);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 如果选择设备为空则代表还没有选择设备
        if (selectDevice == null) {
            //通过地址获取到该设备
            selectDevice = mBluetoothAdapter.getRemoteDevice(address);
        }
        try {

            if (clientSocket == null) {
                // 获取到客户端接口
                clientSocket = selectDevice
                        .createRfcommSocketToServiceRecord(MY_UUID);
                // 向服务端发送连接
                //Toast.makeText(this, "connecting to the server,mac address " + address, Toast.LENGTH_SHORT).show();

                msg = handler.obtainMessage();
                msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                msg.obj = "connecting to the server,mac address " + address;
                msg.arg1 = 1;
                handler.sendMessage(msg);

                clientSocket.connect();
                os = clientSocket.getOutputStream();
                if (os != null) {
                    String text = "hello server,i am a client.";
                    os.write(text.getBytes("UTF-8"));
                }

                mReadThread = new ReadThread();
                mReadThread.start();
                Log.d(TAG, "connected to server!");
                // Toast.makeText(this, "connected to server!", Toast.LENGTH_SHORT).show();

                msg = handler.obtainMessage();
                msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
                msg.obj = "connected to the server successfully!";
                msg.arg1 = 3;
                handler.sendMessage(msg);

            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            clientSocket = null;
            e.printStackTrace();
            Log.d(TAG, "failed to connect server," + e.toString());

            msg = handler.obtainMessage();
            msg.what = BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE;
            msg.arg1 = 0;
            msg.obj = "Failed to connect the server," + e.toString();
            handler.sendMessage(msg);
        }
    }

    void refresh_received_data(String msg)
    {
        m_received_data.append(msg);
        int offset = m_received_data.getLineCount() * m_received_data.getLineHeight();
        if(offset > m_received_data.getHeight()){
            m_received_data.scrollTo(0,offset- m_received_data.getHeight());
        }
    }

    // 创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            switch (msg.what){
                case BLUETOOTH_CLIENT_CONNECT_INFO_MESSAGE:
                    int sts = (int) msg.arg1;
                    if(sts == 0){
                        m_connect_status.setText("当前状态:" + "断开");
                        m_connect_status.setTextColor(Color.GRAY);
                        Toast.makeText(BlueMActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    }else if(sts == 1){
                        m_connect_status.setText("当前状态:" + "正在连接...");
                    }else if(sts == 3){
                        m_connect_status.setText("当前状态:"+"已经连接到服务端");
                        m_connect_status.setTextColor(Color.GREEN);
                        //sb.delete(0,sb.length());
                        received_data_line = 0;
                        m_received_data.scrollTo(0,0);
                        m_received_data.setText("\n");
                        refresh_received_data("接收到的数据：");
                    }
                    break;
                case BLUETOOTH_CLIENT_RECEIVED_DATA:
                    String str = String.format("%05d",received_data_line);
                    //   sb.append( str +":" + msg.obj+ "\n");
                    received_data_line++;
                    refresh_received_data("\n" + str +":" + msg.obj );
                    break;
                default:
                    //Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }

            Log.i(TAG, "recevied message:" + msg.obj);
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothAdapter.cancelDiscovery();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // mBluetoothAdapter.disable();
    }
    public void onClick_File(View view){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_BLUETOOTH);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            File file = new File(uri.getPath());
            try {
                sendFile(file);
            } catch (IOException e) {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        }
    }
    public void sendFile(File file) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        FileInputStream fis = new FileInputStream(file);
        while ((length = fis.read(buffer)) != -1) {
            os.write(buffer, 0, length);
        }
        fis.close();
    }
    public void onClick_Return(View view) throws IOException {
        // 在此处编写按钮点击事件的处理逻辑
        Intent[] intents = new Intent[1];
        intents[0] = new Intent(BlueMActivity.this, MainActivity.class);
        startActivities(intents);
        if (clientSocket!=null) {
            clientSocket.close();
            finish();
        }
    }
}