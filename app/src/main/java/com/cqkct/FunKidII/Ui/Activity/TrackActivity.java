//package com.cqkct.FunKidII.Ui;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.view.View;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import com.amap.api.maps.AMap;
//import com.amap.api.maps.AMapUtils;
//import com.amap.api.maps.CameraUpdateFactory;
//import com.amap.api.maps.MapView;
//import com.amap.api.maps.UiSettings;
//import com.amap.api.maps.model.BitmapDescriptor;
//import com.amap.api.maps.model.BitmapDescriptorFactory;
//import com.amap.api.maps.model.CameraPosition;
//import com.amap.api.maps.model.LatLng;
//import com.amap.api.maps.model.Marker;
//import com.amap.api.maps.model.MarkerOptions;
//import com.amap.api.maps.model.Polyline;
//import com.amap.api.maps.model.PolylineOptions;
//import com.amap.api.maps.model.TextOptions;
//import com.amap.api.services.route.RouteSearch;
//import com.cqkct.FunKidII.Bean.LocationPoint;
//import com.cqkct.FunKidII.R;
//import com.cqkct.FunKidII.Utils.GPSUtils;
//import com.cqkct.FunKidII.Utils.GreenUtils;
//import com.cqkct.FunKidII.Utils.L;
//import com.cqkct.FunKidII.Utils.StringUtils;
//import com.cqkct.FunKidII.Utils.UTIL;
//import com.cqkct.FunKidII.db.Dao.LocationEntityDao;
//import com.cqkct.FunKidII.db.Entity.LocationEntity;
//
//import org.greenrobot.greendao.query.Query;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//
//
//public class TrackActivity extends BaseActivity implements
//        AMap.OnCameraChangeListener {
//    private static final int MSG_UPDATE = 1;
//    private static final int AUTO_UPDATE_TIME = 1000;
//    private MapView mapView = null;
//    private AMap aMap = null;
//    private MarkerOptions markerOption = null;
//    private int currentIndex = -1;
//    private Polyline mMarkerPolyLine = null;
//    private Marker mMoveMarker = null;
//    private ArrayList<LatLng> allPoints = null;
//    private ArrayList<LatLng> points = null;
//
//    private RouteSearch routeSearch;
//    private TextView dataCount = null;
//    private int drawColor = -1;
//    private boolean isReview = false;//是否要播放 (停止播放 和 只显示轨迹的时候 不需要播放)
//    private int selectTimeType = -1;
//    private Calendar beginCalendar = null;
//    private Calendar endCalendar = null;
//    private Button getTrackDateTextView = null;
//    private TextView getTrackBeginTimeTextView = null;
//    private TextView getTrackEndTimeTextView = null;
//    private SimpleDateFormat getDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//    //    private String mDatecur; //当前日期   change_date
//    private String change_date;
//    private String curtime_str, select_monthstr, select_daystr;
//    private String uptime_str;
//    private ImageView btn_track, btn_play, btn_stop;
//    private DateBroadcast mBroadcast;
//    private SharedPreferences datePreferences;
//    private final int UPDATEDATE = 2;
//    private String deviceId = null; //当前设备IMEI号
//    private LinearLayout ll_track_select_date;
//    private ArrayList<String> dateList = new ArrayList<>();
//
//    //测试
//    private static final LatLng marker1 = new LatLng(29.541464, 106.324444);
//    private static final LatLng marker2 = new LatLng(29.541325, 106.323451);
//    private static final LatLng marker3 = new LatLng(29.541122, 106.322515);
//    private static final LatLng marker4 = new LatLng(29.540788, 106.320008);
//    private static final LatLng marker5 = new LatLng(29.540547, 106.318702);
//    private static final LatLng marker6 = new LatLng(29.54013, 106.317345);
//    private static final LatLng marker7 = new LatLng(29.539977, 106.316654);
//    private static final LatLng marker8 = new LatLng(29.539578, 106.314725);
//    private static final LatLng marker9 = new LatLng(29.539368, 106.313839);
//    private static final LatLng marker10 = new LatLng(29.539085, 106.312501);
//    private ArrayList<LatLng> latlngList = new ArrayList<LatLng>();
//
//    private FrameLayout frm_track_play;//播放轨迹播放所在FragmeLayout
//    private FrameLayout frm_track_stop;//停止轨迹播放所在FragmeLayout
//
//    private boolean isTrackPlaying = false;//轨迹是否播放中 （播放完毕 和没有播放都为false 播放时候为true）
//
//
//    final Handler handler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MSG_UPDATE://播放
//                    playTrack();
//                    break;
//
//                case UPDATEDATE:
//                    String select_date = (String) msg.obj;
//                    getTrackDateTextView.setText(select_date);
//                    break;
//            }
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_track);
//        initTitleBar(R.string.track_playback_title);
//
//        float f = 0;
//        for (int i = 0; i < latlngList.size() - 1; i++) {
//            f += AMapUtils.calculateLineDistance(latlngList.get(i),
//                    latlngList.get(i + 1));
//        }
//        latlngList.add(marker1);
//        latlngList.add(marker2);
//        latlngList.add(marker3);
//        latlngList.add(marker4);
//        latlngList.add(marker5);
//        latlngList.add(marker6);
//        latlngList.add(marker7);
//        latlngList.add(marker8);
//        latlngList.add(marker9);
//        latlngList.add(marker10);
//        L.i("float", String.valueOf(f / 1000));
//
//        initView(savedInstanceState);
//        registerBroad();
//    }
//
//    /**
//     * 接收日期更新
//     */
//    private void registerBroad() {
//        mBroadcast = new DateBroadcast();
//        IntentFilter runFilter = new IntentFilter();
//        runFilter.addAction("selectDate");
//        registerReceiver(mBroadcast, runFilter);
//    }
//
//    class DateBroadcast extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            dealDateShow();
//        }
//    }
//
//    private void dealDateShow() {
//        datePreferences = this.getSharedPreferences("datepreferences", MODE_PRIVATE);
//        final int select_day = datePreferences.getInt("select_day_track", 0);
//        final int select_month = datePreferences.getInt("select_month_track", 0);
//        final int select_year = datePreferences.getInt("select_year_track", 0);
//        if ((select_day != 0) && (select_month != 0) && (select_year != 0)) {
//
//            if (select_month < 10) {
//                select_monthstr = "0" + select_month;
//            } else {
//                select_monthstr = String.valueOf(select_month);
//            }
//            if (select_day < 10) {
//                select_daystr = "0" + select_day;
//            } else {
//                select_daystr = String.valueOf(select_day);
//            }
//            String select_date = select_year + "-" + select_monthstr + "-" + select_daystr;
//            Message msg = new Message();
//            msg.what = UPDATEDATE;
//            msg.obj = select_date;
//            handler.sendMessage(msg);
//            // 清除缓存。
//            SharedPreferences.Editor editor = datePreferences.edit();
//            editor.remove("select_day_track");
//            editor.remove("select_month_track");
//            editor.remove("select_year_track");
//            editor.commit();
//            L.e("选择的日期：" + select_date);
//        }
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(mBroadcast);
//
//        if (mapView != null) {
//            mapView.onDestroy();
//            mapView = null;
//        }
//
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        if (mapView != null) {
//            mapView.onSaveInstanceState(outState);
//
//        }
//    }
//
//    @Override
//    public void onCameraChange(CameraPosition position) {
//        //showAddMarker();
//    }
//
//    @Override
//    public void onCameraChangeFinish(CameraPosition arg0) {
//        // TODO Auto-generated method stub
//
//    }
//
//    private void initView(Bundle savedInstanceState) {
//        ll_track_select_date = (LinearLayout) findViewById(R.id.track_select_date);
//        ll_track_select_date.getBackground().setAlpha(200);
//        drawColor = getResources().getColor(R.color.location_fence_draw_color);
//
//        //初始化地图
//        initMap(savedInstanceState);
//
////        getTrackDateTextView = (TextView)findViewById(R.id.tv_get_track_date); //轨迹的日期选择
//        getTrackDateTextView = (Button) findViewById(R.id.tv_get_track_date);
//        getTrackBeginTimeTextView = (TextView) findViewById(R.id.tv_get_track_begin_time);  //取消时间段的选择
//        getTrackEndTimeTextView = (TextView) findViewById(R.id.tv_get_track_end_time);
//
//        beginCalendar = Calendar.getInstance();
//        endCalendar = Calendar.getInstance();
//
//        //退出轨迹回放页面，再回到此页面时，是否显示当前系统的日期（还是显示上次退出页面时的日期）---目前是显示当前系统的日期
//        if (StringUtils.isEmpty(change_date)) {
//            getTrackDateTextView.setText(StringUtils.getStrDate(beginCalendar.getTimeInMillis(), StringUtils.TRACK_DATE_FORMAT_STRING)); //轨迹的开始日期
//        } else {
//            getTrackDateTextView.setText(change_date);
//        }
//        //开始日期和结束日期相同，表示是当前这一天
////        getTrackDateTextView.setText(StringUtils.getStrDate(beginCalendar.getTimeInMillis(), StringUtils.TRACK_DATE_FORMAT_STRING)); //轨迹的开始日期
//        getTrackEndTimeTextView.setText(StringUtils.getStrDate(endCalendar.getTimeInMillis(), StringUtils.TRACK_HOUR_FORMAT_STRING)); //轨迹的结束日期
//
//        beginCalendar.set(Calendar.HOUR_OF_DAY, 0);
//        beginCalendar.set(Calendar.MINUTE, 0);
////        mDatecur = getDateFormat.format(beginCalendar.getTime());
//        getTrackBeginTimeTextView.setText(StringUtils.getStrDate(beginCalendar.getTimeInMillis(), StringUtils.TRACK_HOUR_FORMAT_STRING));
//
//        dataCount = (TextView) findViewById(R.id.tv_history_footer_data_number);  //总数
//        ///////////////////////////////////////////////////////////
//
//        btn_track = (ImageView) findViewById(R.id.btn_track);
//        btn_play = (ImageView) findViewById(R.id.btn_play);
//        btn_stop = (ImageView) findViewById(R.id.btn_stop);
//
//        frm_track_play = (FrameLayout) findViewById(R.id.frm_track_play);
//        frm_track_stop = (FrameLayout) findViewById(R.id.frm_track_stop);
//    }
//
//    private void initMap(Bundle savedInstanceState) {
//        mapView = (MapView) findViewById(R.id.map_track_playback);   // 地图
//        mapView.setVisibility(View.VISIBLE);
//        mapView.onCreate(savedInstanceState);
//        aMap = mapView.getMap();
//        aMap.getUiSettings().setMyLocationButtonEnabled(true);
//        aMap.getUiSettings().setScaleControlsEnabled(true); // 显示比例尺
//        aMap.setMyLocationEnabled(false);
//        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
//        UiSettings uiSettings = aMap.getUiSettings();
//        uiSettings.setRotateGesturesEnabled(false);
//        uiSettings.setZoomControlsEnabled(false);
//        uiSettings.setRotateGesturesEnabled(false);
//        aMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));
//    }
//
//    public void onViewClick(View view) {
//        switch (view.getId()) {
//            case R.id.data_bt_left:  //日期减
//                //日期减前判断轨迹有没有在播放
//                if (isTrackPlaying) {
//                    toast(R.string.track_is_autio_playing);//提示轨迹正在播放中
//                    break;
//                }
//                aMap.clear();//清除地图上的覆盖物等
//                change_date = getTrackDateTextView.getText().toString();
//                String downtime_str = UTIL.getSubtractDay(change_date);
//                getTrackDateTextView.setText(downtime_str);
//                break;
//
//            case R.id.data_bt_right: //日期加
//                //日期减前判断轨迹有没有在播放
//                if (isTrackPlaying) {
//                    toast(R.string.track_is_autio_playing);//提示轨迹正在播放中
//                    break;
//                }
//                aMap.clear();//清除地图上的覆盖物等
//                change_date = getTrackDateTextView.getText().toString(); //日期控件上显示的当前日期
//                Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
//                curtime_str = getDateFormat.format(curDate);
//                if (change_date.equals(curtime_str)) {
//                    toast(R.string.tomorrow_no);
//                } else {
//                    change_date = UTIL.getAddDay(change_date);
//                    getTrackDateTextView.setText(change_date);
//                }
//                getInTimeTrackLocation(change_date);
//
//
//                break;
//
//            case R.id.tv_get_track_date:  //日期选择
//                selectTimeType = R.id.tv_get_track_date;
////                showSelectTime();   弹框样式
//                Intent mIntent = new Intent(TrackActivity.this, CalendarActivity.class);
//                mIntent.putExtra("from", "TrackActivity");
//                mIntent.putStringArrayListExtra("dateList", dateList);
//                startActivity(mIntent);
//
//                break;
//
//            case R.id.btn_track:
//                if (!isFastDoubleClick()) {
//
//                    isReview = false;
//                    isTrackPlaying = false;//点击轨迹时候 轨迹播放停止了
//                    frm_track_play.setVisibility(View.VISIBLE);
//                    frm_track_stop.setVisibility(View.GONE);
//                    constructionLocationIcon(latlngList);
//                    btn_play.setVisibility(View.VISIBLE);
//                    btn_stop.setVisibility(View.GONE);
//                    stopAutoPlaying();
//
//                }
//                break;
//            case R.id.btn_play:     //   stopAutoPlaying    startAutoPlaying
//                isReview = true;
//                isTrackPlaying = true;//正在播放中
//                frm_track_play.setVisibility(View.GONE);
//                frm_track_stop.setVisibility(View.VISIBLE);
//
//                displayTrack();
//                break;
//
//            case R.id.btn_stop:     //   stopAutoPlaying    startAutoPlaying
//                isTrackPlaying = false;//停止播放
//                isReview = false;
//                frm_track_play.setVisibility(View.VISIBLE);
//                frm_track_stop.setVisibility(View.GONE);
//                stopAutoPlaying();
//
//
//                break;
//        }
//    }
//
//    /**
//     * Query track data during time from database
//     *
//     * @param change_date
//     */
//    private void getInTimeTrackLocation(String change_date) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        int start_time = 0;
//        int end_time = 0;
//        try {
//            Date data = sdf.parse(change_date);
//            long time_stamp = data.getTime();
//            start_time = (int) (time_stamp / 1000);
//            end_time = start_time + (60 * 60 * 24);
//            L.e("获取的时间戳：" + time_stamp);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        LocationEntityDao locationBeanDao = GreenUtils.getLocationEntityDao();
//        Query query = locationBeanDao.queryBuilder().where(LocationEntityDao.Properties.LocateTime.between(start_time, end_time)).build();
//        List<LocationEntity> list = query.list();
//        ArrayList<LatLng> locationBeanList = new ArrayList<>();
//        if (list.size() > 0) {
//            for (LocationEntity location : list) {
//                locationBeanList.add(new LatLng(location.getLatitude(), location.getLongitude()));
//            }
//        }
//        constructionLocationIcon(locationBeanList);
//    }
//
//    private void startAutoPlaying(long interTimes) {
//        stopAutoPlaying();
//        handler.sendEmptyMessageDelayed(MSG_UPDATE, interTimes);
//    }
//
//    private void stopAutoPlaying() {
//        handler.removeMessages(MSG_UPDATE);
//    }
//
//    private int getIconId(int positionInfo) {
//        int iconId = R.drawable.footer_history_blue;  //如果是基站定位，显示蓝色定位图标
//        if (positionInfo == 1) { //如果是GPS定位，显示绿色定位图标
//            iconId = R.drawable.footer_history_green;
//        }
//        return iconId;
//    }
//
//    private void changeMapState(double latitude, double longitude) {
//        aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(latitude, longitude)));// 方法设置地图的可视区域。
//    }
//
//    private void constructionLocationIcon(ArrayList<LatLng> latlngList) {//显示轨迹
//        if (latlngList.size() > 0) {
//            currentIndex = 1;
//            dataCount.setText((currentIndex) + "/" + latlngList.size());
//            points = new ArrayList<LatLng>();
//            allPoints = new ArrayList<LatLng>();
//            for (LatLng latLng : latlngList) {
//                LocationPoint locationPoint = GPSUtils.wgs2gcj(new LocationPoint(latLng.latitude, latLng.longitude));
//                allPoints.add(new LatLng(locationPoint.lat, locationPoint.lon));
//            }
//            L.e("轨迹显示");
//            allPoints = removeHundredMetersPoint(allPoints);//移除相差一百米范围的点
//            displayTrack();
//
//        } else {
//            //showCheckMessage(R.string.footer_empty);
//        }
//    }
//
//    //显示轨迹
//    private void displayTrack() {
//
//        aMap.clear();
//        changeMapState(allPoints.get(0).latitude, allPoints.get(0).longitude);
//        BitmapDescriptor startBitmap = BitmapDescriptorFactory
//                .fromResource(R.drawable.start);
//        MarkerOptions startmMarkerOptions = new MarkerOptions().position(
//                allPoints.get(0)).icon(startBitmap).anchor(0.49f, 0.89f);
//        aMap.addMarker(startmMarkerOptions);
//        L.e("Track 轨迹播放-------" + isReview);
//        if (isReview) {
//            points.add(allPoints.get(0));
//            if (allPoints.size() > 1) {
//                points.add(allPoints.get(1));
//                // 创建Marker图标
//                BitmapDescriptor descriptor = BitmapDescriptorFactory
//                        .fromResource(R.drawable.mod_mapposition);
//                PolylineOptions polylineOptions = new PolylineOptions();
//                polylineOptions.addAll(points);
//                polylineOptions.color(drawColor).width(
//                        getResources().getDimensionPixelSize(
//                                R.dimen.dimen_3));
//                mMarkerPolyLine = (Polyline) aMap.addPolyline(polylineOptions);
//
//                markerOption = new MarkerOptions();
//                markerOption.position(points.get(1));
//                markerOption.draggable(true);
//                markerOption.icon(descriptor);
//                markerOption.setFlat(true);
//                markerOption.anchor(0.49f, 0.89f);
//                TextOptions text = new TextOptions();
//                text.position(points.get(1));
//                text.fontSize(25);
//                text.text("1");
//                aMap.addText(text);
//                aMap.addMarker(markerOption);
//                startAutoPlaying(AUTO_UPDATE_TIME);
//            }
//        } else {
//
//            PolylineOptions polylineOptions = new PolylineOptions();
//            polylineOptions.addAll(allPoints);
//            polylineOptions.color(drawColor).width(
//                    getResources().getDimensionPixelSize(
//                            R.dimen.dimen_3));
//            mMarkerPolyLine = aMap.addPolyline(polylineOptions);
//
//            BitmapDescriptor endBitmap = BitmapDescriptorFactory
//                    .fromResource(R.drawable.end);
//            MarkerOptions endA = new MarkerOptions().position(
//                    allPoints.get(allPoints.size() - 1)
//            ).icon(endBitmap).anchor(0.49f, 0.89f);
//
//            aMap.addMarker(endA);
//
//            frm_track_play.setVisibility(View.VISIBLE);
//            frm_track_stop.setVisibility(View.GONE);
//
//        }
//
//    }
//
//    //    播放轨迹
//    private void playTrack() {
//
//        LatLng p1 = allPoints.get(currentIndex);
//        L.e("Track allPoints:" + p1.latitude + ":" + p1.longitude);
//        changeMapState(p1.latitude, p1.longitude);
//        points.add(p1);
//        mMarkerPolyLine.setPoints(points);
//
//
//        BitmapDescriptor descriptor = BitmapDescriptorFactory
//                .fromResource(R.drawable.mod_mapposition);
//        MarkerOptions ooA = new MarkerOptions().position(p1).icon(
//                descriptor).anchor(0.49f, 0.89f);
//        mMoveMarker = aMap.addMarker(ooA);
//        TextOptions text = new TextOptions();
//        text.position(allPoints.get(currentIndex));
//        text.fontSize(25);
//        text.text(String.valueOf(currentIndex));
//        aMap.addText(text);
//        if (currentIndex < allPoints.size() - 1) {
//            //mMoveMarker.setRotateAngle((float) getAngle(currentIndex));
//        } else {
//            BitmapDescriptor endBitmap = BitmapDescriptorFactory
//                    .fromResource(R.drawable.end);
//            MarkerOptions endA = new MarkerOptions().position(
//                    allPoints.get(allPoints.size() - 1))
//                    .icon(endBitmap).anchor(0.49f, 0.89f);
//            aMap.addMarker(endA);
//        }
//        if (currentIndex++ < allPoints.size() - 1) {   //不是最后一个点，每隔500ms,打印一个点
//            startAutoPlaying(AUTO_UPDATE_TIME);
//        } else {//是最后一个点 添加到地图上后 置播放完中false
//            BitmapDescriptor endBitmap = BitmapDescriptorFactory
//                    .fromResource(R.drawable.end);
//            MarkerOptions endA = new MarkerOptions().position(
//                    allPoints.get(allPoints.size() - 1)).icon(endBitmap).anchor(0.49f, 0.89f);
//            aMap.addMarker(endA);
//            frm_track_play.setVisibility(View.VISIBLE);
//            frm_track_stop.setVisibility(View.GONE);
////                        btn_play.setVisibility(View.VISIBLE);
////                        btn_stop.setVisibility(View.GONE);
//            isTrackPlaying = false;//播放完毕
//        }
//
//        dataCount.setText((currentIndex) + "/" + allPoints.size());
//
//    }
//
//    //2.0s内不重复点击
//    private long lastClickTime;
//
//    public boolean isFastDoubleClick() {
//        long time = System.currentTimeMillis();
//        long timeD = time - lastClickTime;
//        if (0 < timeD && timeD < 2000) {
//            return true;
//        }
//        lastClickTime = time;
//        return false;
//    }
//
//    //移除相差一百米范围的点
//    private ArrayList<LatLng> removeHundredMetersPoint(ArrayList<LatLng> points) {
//        if (points == null || points.size() < 2) {//最少两个点 才比较
//            return points;
//        }
//
//        int j = 0;// 记录不符合点的下标
//        ArrayList<Integer> indexs = new ArrayList<Integer>();//保存不符合点的下标
//
//        for (int i = 1; i < points.size() - 1; i++) {//不处理最后一个点 可能最后剩下一个点 导致只有起点没有终点
//            LatLng latLng1 = points.get(j);
//            LatLng latLng2 = points.get(i);
//            float distance = AMapUtils.calculateLineDistance(latLng1, latLng2);
//            if (Math.abs(distance) <= 100.0d) {
//                indexs.add(i);
//            } else {
//                j = i;
//            }
//        }
//
//        if (indexs != null && indexs.size() != 0) {//有要移除的点
//            for (int i = indexs.size() - 1; i >= 0; i--) {
//                int index = indexs.get(i);
//                points.remove(index);
//            }
//        }
//
//        return points;
//
//    }
//
//    //将高德地图的经纬度集合 变成 谷歌经纬度集合点
//    private ArrayList<com.google.android.gms.maps.model.LatLng> gaoDePointsToGooglePoints(ArrayList<LatLng> gaoDePoints) {
//        ArrayList<com.google.android.gms.maps.model.LatLng> googlePoints = new ArrayList<>();
//        for (LatLng latLng : gaoDePoints) {
//            com.google.android.gms.maps.model.LatLng googleLatLng = new com.google.android.gms.maps.model.LatLng(latLng.latitude, latLng.longitude);
//            googlePoints.add(googleLatLng);
//        }
//        return googlePoints;
//    }
//
//
////
////    private void insertTrackBean(String bindCode, int time, int locationType, double longitude, double latitude) {
////        LocationEntityDao locationBeanDao= GreenUtils.getLocationEntityDao();
////        Query query=locationBeanDao.queryBuilder().where(LocationEntityDao.Properties.Time_stamp.eq(time)).build();
////        List<LocationEntity> beanList=query.list();
////        LocationEntity location=new LocationEntity();
////        location.setDevice_id(bindCode);
////        location.setTime_stamp(time);
////        location.setLocation_type(locationType);
////        location.setLongitude(longitude);
////        location.setLatitude(latitude);
////        if (beanList.size()<=0){
////            locationBeanDao.insert(location);
////        }
////    }
//
//}
