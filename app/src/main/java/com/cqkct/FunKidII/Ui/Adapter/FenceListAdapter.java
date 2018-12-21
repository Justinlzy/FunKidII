package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import protocol.Message;


/**
 * Created by Kct on 2016/8/12.
 */
public class FenceListAdapter extends RecyclerView.Adapter {  //围栏数据

    public static final int TYPE_ITEM_SCHOOL_GUARD = 0;
    public static final int TYPE_ITEM_FENCE = 1;

    private List<FenceDataType> dataList;
    private Context context;
    private boolean mHasEditPermission;
    private List<TextureMapView> mapViewLists = new ArrayList<>();
    private OnItemOnClickListener listener;
    private int mapType;


    public static class FenceDataType implements Serializable {
        public Message.SchoolGuard schoolGuard;
        public FenceEntity entity;


        public FenceDataType(Message.SchoolGuard schoolGuard) {
            this.schoolGuard = schoolGuard;
        }

        public FenceDataType(FenceEntity entity) {
            this.entity = entity;
        }
    }


    public FenceListAdapter(List<FenceDataType> list, Context context, boolean hasEditPermission, int mapType) {
        super();
        this.context = context;
        this.dataList = list;
        this.mHasEditPermission = hasEditPermission;
        this.mapType = mapType;
    }

    public void setMapOnClickListener(OnItemOnClickListener mapOnClickListener) {
        this.listener = mapOnClickListener;
    }

    public interface OnItemOnClickListener {
        void onFenceItemClick(int position);

        void onGuardianItemClick();

        void onGuardianAddClick();

        void onCompoundButtonClick(int position, CompoundButton cb);
    }

    @Override
    public int getItemViewType(int position) {
        if (dataList.get(position).schoolGuard != null)
            return TYPE_ITEM_SCHOOL_GUARD;
        else if (dataList.get(position).entity != null)
            return TYPE_ITEM_FENCE;
        return -1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FencesViewHolder) {
            FencesViewHolder fencesViewHolder = (FencesViewHolder) holder;

            Message.Fence fence = dataList.get(position).entity.getFence();
            String title = fence.getName();
            if (title.contains("-")) {
                int index = title.indexOf("-");
                if (index < title.length() - 1) {
                    title = title.substring(title.indexOf("-") + 1);
                }
            }
            fencesViewHolder.fenceName.setText(title);

            List<Message.Fence.Period> periods = fence.getPeriodList();
            Message.Fence.Period period = periods.get(0);
            long startTime = period.getStartTime().getTime();
            long endTime = period.getEndTime().getTime();
            int repeat = period.getRepeat();

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(startTime * 1000L);
            String startTimeStr = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            calendar.setTimeInMillis(endTime * 1000L);
            String endTimeStr = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

            fencesViewHolder.fence_time.setText(String.valueOf(startTimeStr + " | " + endTimeStr));
            fencesViewHolder.fence_repeat.setText(PublicTools.getDecoderWeak(repeat, context));
            fencesViewHolder.fence_address.setText(dataList.get(position).entity.getFenceAddress());
            switch (fence.getIconType()) {
                //0: 默认；1: 私人； 2: 公共
                case 0:
                    fencesViewHolder.fenceType.setImageResource(R.drawable.fence_default_icon);
                    break;
                case 1:
                    fencesViewHolder.fenceType.setImageResource(R.drawable.fence_private_icon);
                    break;
                case 2:
                    fencesViewHolder.fenceType.setImageResource(R.drawable.fence_public_icon);
                    break;
            }
            fencesViewHolder.fenceItemLayout.setOnClickListener(v -> listener.onFenceItemClick(holder.getAdapterPosition()));
            if (mapType == Constants.MAP_TYPE_AMAP) {
                fencesViewHolder.amapMap.setVisibility(View.VISIBLE);
                fencesViewHolder.gmapView.setVisibility(View.GONE);

                fencesViewHolder.amapMap.getMap().setOnMapClickListener(latLng -> listener.onFenceItemClick(holder.getAdapterPosition()));

                addElementOnMap(fencesViewHolder.amapMap.getMap(), fence);
            } else if (mapType == Constants.MAP_TYPE_GOOGLE) {
                fencesViewHolder.amapMap.setVisibility(View.GONE);
                fencesViewHolder.gmapView.setVisibility(View.VISIBLE);
                fencesViewHolder.bindView(fence);
                synchronized (fencesViewHolder) {
                    fencesViewHolder.mOnMapClickListener = latLng -> listener.onFenceItemClick(holder.getAdapterPosition());
                    if (fencesViewHolder.gmap != null) {
                        fencesViewHolder.gmap.setOnMapClickListener(fencesViewHolder.mOnMapClickListener);
                    }
                }
            }
        } else if (holder instanceof FenceSchoolViewHolder) {
            FenceSchoolViewHolder viewHolder = (FenceSchoolViewHolder) holder;
            Message.SchoolGuard schoolGuard = dataList.get(position).schoolGuard;

            if (TextUtils.isEmpty(schoolGuard.toString())) {
                viewHolder.addGuardianImage.setVisibility(View.VISIBLE);
                viewHolder.addGuardianInfo.setVisibility(View.VISIBLE);
                viewHolder.addGuardian.setOnClickListener(v -> {
                    listener.onGuardianAddClick();
//                    Intent intent = new Intent(this, GuardianActivity.class);
//                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE);
                });
                viewHolder.amapView.setVisibility(View.GONE);
                viewHolder.guardianInfo.setVisibility(View.GONE);
                return;
            } else {
                viewHolder.addGuardianImage.setVisibility(View.GONE);
                viewHolder.addGuardianInfo.setVisibility(View.GONE);
                viewHolder.amapView.setVisibility(View.VISIBLE);
                viewHolder.guardianInfo.setVisibility(View.VISIBLE);
            }

            viewHolder.scGuardian.setOnCheckedChangeListener(null);
            viewHolder.scGuardian.setChecked(schoolGuard.getEnable());

            if (listener != null && mHasEditPermission) {
                viewHolder.scGuardian.setOnCheckedChangeListener(modifyGuardianListener);
                viewHolder.scGuardian.setEnabled(true);
            } else {
                viewHolder.scGuardian.setEnabled(false);
            }
            viewHolder.scGuardian.setTag(position);


            Message.Fence.Shape shapeSchool = schoolGuard.getSchool().getFence();
            Message.Fence.Shape shapeHome = schoolGuard.getHome().getFence();

            switch (shapeSchool.getShapeCase()) {
                case ROUND:
                    Message.Fence.Shape.Round round = shapeSchool.getRound();
                    viewHolder.latLonSchool = round.getLatlon();
                    break;
                case POLYGON:
                    break;
            }
            switch (shapeHome.getShapeCase()) {
                case ROUND:
                    Message.Fence.Shape.Round round = shapeHome.getRound();
                    viewHolder.latLonHome = round.getLatlon();
                    break;
                case POLYGON:
                    break;
            }
            viewHolder.guardianSchoolAddress.setText(schoolGuard.getSchool().getAddr());
            viewHolder.guardianHomeAddress.setText(schoolGuard.getHome().getAddr());



            long startFornoonTime = schoolGuard.getForenoon().getStartTime().getTime();
            long endFornoonTime = schoolGuard.getForenoon().getEndTime().getTime();
            long startAfternoonTime = schoolGuard.getAfternoon().getStartTime().getTime();
            long endAfternoonTime = schoolGuard.getAfternoon().getEndTime().getTime();

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(startFornoonTime * 1000L);
            int startFornoonHour = calendar.get(Calendar.HOUR_OF_DAY);
            int startFornoonMin = calendar.get(Calendar.MINUTE);
            String startFornoon = String.format("%02d:%02d", startFornoonHour, startFornoonMin);

            calendar.setTimeInMillis(endFornoonTime * 1000L);
            int endFornoonHour = calendar.get(Calendar.HOUR_OF_DAY);
            int endFornoonMin = calendar.get(Calendar.MINUTE);
            String endFornoon = String.format("%02d:%02d", endFornoonHour, endFornoonMin);

            calendar.setTimeInMillis(startAfternoonTime * 1000L);
            int startAfternoonHour = calendar.get(Calendar.HOUR_OF_DAY);
            int startAfternoonMin = calendar.get(Calendar.MINUTE);
            String startAfternoon = String.format("%02d:%02d", startAfternoonHour, startAfternoonMin);


            calendar.setTimeInMillis(endAfternoonTime * 1000L);
            int endAfternoonHour = calendar.get(Calendar.HOUR_OF_DAY);
            int endAfternoonMin = calendar.get(Calendar.MINUTE);
            String endAfternoon = String.format("%02d:%02d", endAfternoonHour, endAfternoonMin);
            viewHolder.guardianTime.setText(startFornoon + ":" + endFornoon + " | " + startAfternoon + ":" + endAfternoon);

            viewHolder.fenceSchoolLayout.setOnClickListener(v -> listener.onGuardianItemClick());
            if (mapType == Constants.MAP_TYPE_AMAP) {
                viewHolder.amapView.setVisibility(View.VISIBLE);
                viewHolder.gmapView.setVisibility(View.GONE);

                viewHolder.amapView.getMap().setOnMapClickListener(latLng -> listener.onGuardianItemClick());

                viewHolder.addElementOnGuardianMap(viewHolder.amapView.getMap(), viewHolder.latLonSchool, viewHolder.latLonHome, viewHolder);
            } else if (mapType == Constants.MAP_TYPE_GOOGLE) {
                viewHolder.amapView.setVisibility(View.GONE);
                viewHolder.gmapView.setVisibility(View.VISIBLE);
                viewHolder.bindGuardianView(schoolGuard);
                synchronized (viewHolder) {
                    viewHolder.mOnGuardianMapClickListener = latLng -> listener.onGuardianItemClick();
                    if (viewHolder.gMap != null) {
                        viewHolder.gMap.setOnMapClickListener(viewHolder.mOnGuardianMapClickListener);
                    }
                }
            }
        }
    }

    private void addElementOnMap(AMap amapMap, Message.Fence fence) {
        amapMap.clear();
        amapMap.getUiSettings().setZoomControlsEnabled(false); // 不显示缩放按钮
        amapMap.getUiSettings().setLogoBottomMargin(-50);// 隐藏logo
        Message.LatLon latLon = fence.getShape().getRound().getLatlon();

        LatLng amapLatlng = new LatLng(latLon.getLatitude(), latLon.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(amapLatlng)
                .anchor(63f/125f, 97f/139f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon));
        Marker locationMarker = amapMap.addMarker(markerOptions);
        locationMarker.setVisible(true);
        amapMap.moveCamera(CameraUpdateFactory.newLatLngZoom(amapLatlng, 13.5f));

        //画圆
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(amapLatlng);
        circleOptions.radius(fence.getShape().getRound().getRadius());
        circleOptions.strokeWidth(3.0f);
        circleOptions.strokeColor(Constants.FENCE_STROKE_COLOR);
        circleOptions.fillColor(Constants.FENCE_FILL_COLOR);
        amapMap.addCircle(circleOptions);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM_FENCE) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_fence, parent, false);
            if (mHasEditPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                itemView.setForeground(context.getDrawable(R.drawable.foreground_shadow));
            FencesViewHolder holder = new FencesViewHolder(itemView, mapType, context);
            holder.amapMap.onCreate(null);
            mapViewLists.add(holder.amapMap);
            return holder;

        } else if (viewType == TYPE_ITEM_SCHOOL_GUARD) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_school_fence, parent, false);
            FenceSchoolViewHolder holder = new FenceSchoolViewHolder(itemView, mapType, context);
            holder.amapView.onCreate(null);
            mapViewLists.add(holder.amapView);
            return holder;
        }
        return null;
    }


    public final class FencesViewHolder extends RecyclerView.ViewHolder {

        TextureMapView amapMap;
        MapView gmapView;
        GoogleMap gmap;

        TextView fenceName, fence_time, fence_repeat, fence_address;
        ImageView fenceType;
        int mapType;

        Context context;
        View itemView;

        ConstraintLayout fenceItemLayout;

        GoogleMap.OnMapClickListener mOnMapClickListener;

        FencesViewHolder(View itemView, int mapType, Context context) {
            super(itemView);
            this.itemView = itemView;
            this.mapType = mapType;
            this.context = context;

            fenceName = itemView.findViewById(R.id.fence_name); //围栏标题
            fence_time = itemView.findViewById(R.id.fence_time);
            fence_repeat = itemView.findViewById(R.id.fence_time_repeat);
            fence_address = itemView.findViewById(R.id.fence_addreas);
            amapMap = itemView.findViewById(R.id.map_amap);
            gmapView = itemView.findViewById(R.id.map_gmap);
            fenceType = itemView.findViewById(R.id.fence_type);
            fenceItemLayout = itemView.findViewById(R.id.cl_fence_item);

            if (gmapView != null) {
                gmapView.onCreate(null);
                gmapView.getMapAsync(new MyOnMapReadyCallback(this));
            }
        }

        class MyOnMapReadyCallback implements OnMapReadyCallback {
            @NonNull
            final FencesViewHolder mFencesViewHolder;

            MyOnMapReadyCallback(@NonNull FencesViewHolder fencesViewHolder) {
                mFencesViewHolder = fencesViewHolder;
            }

            @Override
            public void onMapReady(GoogleMap googleMap) {
                MapsInitializer.initialize(context);
                synchronized (mFencesViewHolder) {
                    gmap = googleMap;
                    if (mFencesViewHolder.gmap != null) {
                        gmap.setOnMapClickListener(mFencesViewHolder.mOnMapClickListener);
                    }
                }
            }
        }

        private void setFenceMapLocation() {
            if (gmap == null) return;

            gmap.getUiSettings().setMapToolbarEnabled(false);
            gmap.clear();
            Message.Fence fence = (Message.Fence) gmapView.getTag();
            if (fence == null) return;

            Message.LatLon latLon = fence.getShape().getRound().getLatlon();


            gmap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    new com.google.android.gms.maps.model.LatLng(latLon.getLatitude(), latLon.getLongitude()), 13f));

            gmap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .anchor(63f/125f, 97f/139f)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon))
                    .position(new com.google.android.gms.maps.model.LatLng(latLon.getLatitude(), latLon.getLongitude())));

            gmap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                    .center(new com.google.android.gms.maps.model.LatLng(latLon.getLatitude(), latLon.getLongitude()))
                    .radius(fence.getShape().getRound().getRadius())
                    .strokeWidth(1)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR));
            gmap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        }

        void bindView(Message.Fence fence) {
            itemView.setTag(this);
            gmapView.setTag(fence);
            setFenceMapLocation();
        }
    }

    public class FenceSchoolViewHolder extends RecyclerView.ViewHolder {
        TextView guardianTime, guardianSchoolAddress, guardianHomeAddress;
        RelativeLayout guardianInfo, addGuardianImage, addGuardianInfo;
        SwitchCompat scGuardian;
        ImageView addGuardian;

        Message.LatLon latLonSchool = null;
        Message.LatLon latLonHome = null;

        ConstraintLayout fenceSchoolLayout;

        TextureMapView amapView;
        MapView gmapView;
        GoogleMap gMap;

        GoogleMap.OnMapClickListener mOnGuardianMapClickListener;

        FenceSchoolViewHolder(View itemView, int mapType, Context context) {
            super(itemView);
            amapView = itemView.findViewById(R.id.map_amap_guardian);
            gmapView = itemView.findViewById(R.id.map_gmap_guardian);

            addGuardianImage = itemView.findViewById(R.id.add_guardian_image);
            addGuardianInfo = itemView.findViewById(R.id.add_guardian_info);
            guardianInfo = itemView.findViewById(R.id.guardian_info);
            fenceSchoolLayout = itemView.findViewById(R.id.cl_guardian_fence);
            scGuardian = itemView.findViewById(R.id.ib_switch);
            addGuardian = itemView.findViewById(R.id.add_guardian);
            guardianTime = itemView.findViewById(R.id.guardian_time);
            guardianSchoolAddress = itemView.findViewById(R.id.guardian_school_address);
            guardianHomeAddress = itemView.findViewById(R.id.guardian_home_address);

            if (gmapView != null) {
                gmapView.onCreate(null);
                gmapView.getMapAsync(new FenceSchoolViewHolder.MyOnMapReadyCallback(this));
            }
        }


        class MyOnMapReadyCallback implements OnMapReadyCallback {
            @NonNull
            final FenceSchoolViewHolder mFenceSchoolViewHolder;

            MyOnMapReadyCallback(@NonNull FenceSchoolViewHolder fenceSchoolViewHolder) {
                mFenceSchoolViewHolder = fenceSchoolViewHolder;
            }

            @Override
            public void onMapReady(GoogleMap googleMap) {
                MapsInitializer.initialize(context);
                synchronized (mFenceSchoolViewHolder) {
                    gMap = googleMap;
                    if (mFenceSchoolViewHolder.gMap != null) {
                        gMap.setOnMapClickListener(mFenceSchoolViewHolder.mOnGuardianMapClickListener);
                    }
                }
            }
        }

        void bindGuardianView(Message.SchoolGuard schoolGuard) {
            itemView.setTag(this);
            gmapView.setTag(schoolGuard);
            setGuardianMapLocation();
        }

        private void setGuardianMapLocation() {
            if (gMap == null) return;

            gMap.getUiSettings().setMapToolbarEnabled(false);
            gMap.clear();
            com.google.android.gms.maps.model.LatLngBounds.Builder gmapBoundsBuilder = new com.google.android.gms.maps.model.LatLngBounds.Builder();

            com.google.android.gms.maps.model.LatLng schoolLatlng = new com.google.android.gms.maps.model.LatLng(latLonSchool.getLatitude(), latLonSchool.getLongitude());
            com.google.android.gms.maps.model.LatLng homeLatlng = new com.google.android.gms.maps.model.LatLng(latLonHome.getLatitude(), latLonHome.getLongitude());

            gmapBoundsBuilder.include(schoolLatlng);
            gmapBoundsBuilder.include(homeLatlng);

            Message.SchoolGuard schoolGuard = (Message.SchoolGuard) gmapView.getTag();
            if (schoolGuard == null) return;


            gMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .anchor(0.49f, 0.89f)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.location_school))
                    .position(new com.google.android.gms.maps.model.LatLng(schoolLatlng.latitude, schoolLatlng.longitude)));

            gMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .anchor(0.49f, 0.89f)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.location_home))
                    .position(new com.google.android.gms.maps.model.LatLng(homeLatlng.latitude, homeLatlng.longitude)));

            gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            gMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(gmapBoundsBuilder.build(), 80));// 方法设置地图的可视区域。
        }

        Marker amapPointMarkerSchool = null;
        Marker amapPointMarkerHome = null;

        void addElementOnGuardianMap(AMap amap, Message.LatLon latLonSchool, Message.LatLon latLonHome, FenceSchoolViewHolder viewHolder) {
            if (amapPointMarkerSchool != null) {
                amapPointMarkerSchool.remove();
            }
            if (amapPointMarkerHome != null) {
                amapPointMarkerHome.remove();
            }
            amap.clear();
            amap.getUiSettings().setZoomControlsEnabled(false); // 不显示缩放按钮
            amap.getUiSettings().setLogoBottomMargin(-50);// 隐藏logo
            LatLngBounds.Builder amapBoundsBuilder = new LatLngBounds.Builder();


            LatLng latLngSchool = new LatLng(latLonSchool.getLatitude(), latLonSchool.getLongitude());
            LatLng latLngHome = new LatLng(latLonHome.getLatitude(), latLonHome.getLongitude());
            amapBoundsBuilder.include(latLngSchool);
            amapBoundsBuilder.include(latLngHome);

            MarkerOptions markerOptionsSchool = new MarkerOptions().anchor(0.49f, 0.89f)
                    .position(latLngSchool)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_school));
            amapPointMarkerSchool = amap.addMarker(markerOptionsSchool);

            MarkerOptions markerOptionsHome = new MarkerOptions().anchor(0.49f, 0.89f)
                    .position(latLngHome)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_home));
            amapPointMarkerHome = amap.addMarker(markerOptionsHome);

            amap.animateCamera(CameraUpdateFactory.newLatLngBounds(amapBoundsBuilder.build(), 120));// 方法设置地图的可视区域。
        }

    }


    private CompoundButton.OnCheckedChangeListener modifyGuardianListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (listener != null) {
                buttonView.setOnCheckedChangeListener(null);
                buttonView.setEnabled(false);
                buttonView.setChecked(!isChecked);
                listener.onCompoundButtonClick((Integer) buttonView.getTag(), buttonView);
            }
        }
    };


    /**
     * 列表里面缓存了很多地图对象，所以一定要调用销毁方法
     */
    public void onDestroy() {
        for (TextureMapView mapView : mapViewLists) {
            mapView.onDestroy();
        }
    }

}
