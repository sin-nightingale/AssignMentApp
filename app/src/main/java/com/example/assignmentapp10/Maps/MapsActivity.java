package com.example.assignmentapp10.Maps;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.example.assignmentapp10.MainActivity;
import com.example.assignmentapp10.R;

public class MapsActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static MapView mMapView = null;
    public static BaiduMap mBaiduMap = null;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private LocationClient mLocationClient = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocationClient.setAgreePrivacy(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // 请求定位权限
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        }
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        //普通地图 ,mBaiduMap是地图控制器对象
        mBaiduMap.setMyLocationEnabled(true);
        Button returnButton = (Button) findViewById(R.id.return1);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在此处编写按钮点击事件的处理逻辑
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MapsActivity.this, MainActivity.class);
                startActivities(intents);
                finish();
            }
        });

        //定位初始化
        try {
            mLocationClient = new LocationClient(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();

    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
    }
}
