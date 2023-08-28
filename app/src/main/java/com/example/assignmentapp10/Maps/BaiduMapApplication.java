package com.example.assignmentapp10.Maps;

import android.app.Application;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;

public class BaiduMapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // java
        // 是否同意隐私政策，默认为false
        // 获取ApplicationContext
        SDKInitializer.setAgreePrivacy(this,true);
        try {
            // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
            SDKInitializer.initialize(this);
        } catch (BaiduMapSDKException e) {
            System.out.println("ERROR");
        }
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);
    }
}
