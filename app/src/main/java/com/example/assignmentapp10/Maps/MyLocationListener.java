package com.example.assignmentapp10.Maps;

import static com.example.assignmentapp10.Maps.MapsActivity.mBaiduMap;
import static com.example.assignmentapp10.Maps.MapsActivity.mMapView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MyLocationListener extends BDAbstractLocationListener{
    @Override
    public void onReceiveLocation(BDLocation location) {
        //mapView 销毁后不在处理新接收的位置
        if (location == null || mMapView == null){
            return;
        }
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(location.getDirection()).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        mBaiduMap.setMyLocationData(locData);
        // 获取定位信息
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // 将定位信息转换为 LatLng 对象
        LatLng latLng = new LatLng(latitude, longitude);
        // 将地图的绘制中心设置为当前位置
        MapStatusUpdate centerMapStatus = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(centerMapStatus);
    }
}