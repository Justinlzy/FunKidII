package com.cqkct.FunKidII.Ui.Activity;


import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.BabyCardAdapter;
import com.cqkct.FunKidII.Ui.Adapter.BabyCardClipViewPagerAdapter;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.Model.BabyCardModel;
import com.cqkct.FunKidII.Ui.fragment.ChooseAvatarWayDialogFragment;
import com.cqkct.FunKidII.Ui.view.ClipViewPager;
import com.cqkct.FunKidII.Ui.view.PullBackLayout;
import com.cqkct.FunKidII.Ui.view.ScalePageTransformer;
import com.cqkct.FunKidII.Ui.view.ScalePageTransformerCard;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.QRCodeUtils;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.OkHttpRequestManager;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.ucrop.UCropActivity;
import com.google.protobuf.GeneratedMessageV3;
import com.google.zxing.WriterException;
import com.gyf.barlibrary.ImmersionBar;
import com.yalantis.ucrop.UCrop;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import protocol.Message;


/**
 * Created by justin on 2017/8/3.
 */

public class BabyCardActivity extends BaseActivity implements BabyCardAdapter.BabyDataChangedListener {
    public String TAG = BabyCardActivity.class.getSimpleName();

    public static final String ACTIVITY_PARAM_DEVICE_ID = "device_id"; //没用

    private static final int TAKE_PICTURE = 0;//相机选择头像-返回
    private static final int RESULT_LOAD_IMAGE = 1;//相册选择头像-返回
    private static final int UCROP_REQUEST_CODE = 2;//裁剪头像-返回
    private static final int ACTIVITY_REQUEST_CODE_SELECT_RELATION = 3;
    private static final int ACTIVITY_REQUEST_CODE_ALL_BABY_LIST = 10;


    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private PullBackLayout mPullBackLayout;

    private ViewPager mBabyCardPager;
    private ClipViewPager mClipPager;
    private BabyCardAdapter mBabyCardAdapter;
    private BabyCardClipViewPagerAdapter mClipAdapter;

    private List<BabyEntity> babyDataList = new ArrayList<>();
    public TaskHandler mTaskHandler = new TaskHandler(this);
    private int modifyBabyHeardPosition = -1;
    private BabyCardModel model;

    private Uri photoUri;
    private File uploadFile;      //图片文件

    public static class TaskHandler extends Handler {

        private static final int REFRESH_PAGER = 100;
        private static final int BABY_CARD_PAGER_SET_CURRENT_ITEM = 101;
        private static final int CLIP_PAGER_SET_CURRENT_ITEM = 102;

        private static final int CHANGE_BABY_HEAD_ICON = 105;
        private static final int SHOW_QR_CODE = 106;
        private static final int SELECT_AS_CURRENT = 107;
        private static final int UNBIND_AND_BIND = 108;
        private static final int MODIFY_RELATION = 109;
        WeakReference<BabyCardActivity> mA;

        TaskHandler(BabyCardActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            BabyCardActivity a = mA.get();
            if (a == null)
                return;
            if (a.isFinishing())
                return;
            if (a.mTlcService == null) {
                L.w(a.TAG, "TaskHandler: handleMessage: a.mTlcService == null");
                return;
            }

            switch (msg.what) {
                case REFRESH_PAGER:
                    a.refreshClipViewPagerData();
                    break;

                case BABY_CARD_PAGER_SET_CURRENT_ITEM:
                    a.mBabyCardPager.setCurrentItem(msg.arg1);
                    break;

                case CLIP_PAGER_SET_CURRENT_ITEM:
                    a.mClipPager.setCurrentItem(msg.arg1);
                    break;

                case CHANGE_BABY_HEAD_ICON: {
                    a.modifyBabyHeardPosition = (int) msg.obj;
                    a.changeBabyHead();
                }
                break;
                case SHOW_QR_CODE:
                    a.showQRCode(a.babyDataList.get((int) msg.obj));
                    break;

                case SELECT_AS_CURRENT:
                    int position = (int) msg.obj;
                    BabyEntity babyEntity = a.babyDataList.get(position);
                    if (!babyEntity.getIs_select()) {
                        GreenUtils.selectBaby(babyEntity.getUserId(), babyEntity.getDeviceId());
                    }
                    break;
                case UNBIND_AND_BIND:
                    a.showUnbind((int) msg.obj);
                    break;
                case MODIFY_RELATION:
                    a.modifyBabyHeardPosition = (int) msg.obj;
                    a.changeRelation();
                    break;
                default:
                    break;
            }
        }


        public void refreshPager() {
            removeMessages(REFRESH_PAGER);
            sendEmptyMessageDelayed(REFRESH_PAGER, 10);
        }

        public synchronized void babyCardPagerSelectBaby(int position) {
            removeMessages(BABY_CARD_PAGER_SET_CURRENT_ITEM);
            sendMessageDelayed(obtainMessage(BABY_CARD_PAGER_SET_CURRENT_ITEM, position, 0), 50);
        }

        public synchronized void clipPagerSelectBaby(int position) {
            removeMessages(CLIP_PAGER_SET_CURRENT_ITEM);
            sendMessageDelayed(obtainMessage(CLIP_PAGER_SET_CURRENT_ITEM, position, 0), 50);
        }

        public synchronized void changeBabyHeadIcon(int position) {
            removeMessages(CHANGE_BABY_HEAD_ICON);
            sendMessageDelayed(obtainMessage(CHANGE_BABY_HEAD_ICON, position), 50);
        }

        public synchronized void babyCardQrCode(int position) {
            removeMessages(SHOW_QR_CODE);
            sendMessageDelayed(obtainMessage(SHOW_QR_CODE, position), 50);
        }

        public synchronized void selectAsCurrent(int position) {
            removeMessages(SELECT_AS_CURRENT);
            sendMessageDelayed(obtainMessage(SELECT_AS_CURRENT, position), 50);
        }

        public synchronized void unbindAndBind(int position) {
            removeMessages(UNBIND_AND_BIND);
            sendMessageDelayed(obtainMessage(UNBIND_AND_BIND, position), 50);
        }

        public synchronized void modifyRelation(int position) {
            removeMessages(MODIFY_RELATION);
            sendMessageDelayed(obtainMessage(MODIFY_RELATION, position), 50);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.baby_information);
        setTitleBarTitle(R.string.title_baby_card);
        initView();
        mTaskHandler.refreshPager();
    }

    private void initView() {
        model = new BabyCardModel(this);

        mClipPager = findViewById(R.id.clip_viewpager);
        mClipPager.setPageTransformer(true, new ScalePageTransformer());
        mClipPager.setOffscreenPageLimit(3);
        findViewById(R.id.page_container).setOnTouchListener((v, event) -> mClipPager.dispatchTouchEvent(event));

        mClipAdapter = new BabyCardClipViewPagerAdapter(this, babyDataList);
        mClipPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.dimen_8));
        mClipPager.setAdapter(mClipAdapter);
        mClipPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                mTaskHandler.babyCardPagerSelectBaby(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

//        mPullBackLayout = findViewById(R.id.pull_back_layout);
//        mPullBackLayout.setCallback(new PullBackLayout.Callback() {
//            @Override
//            public void onPullStart() { }
//            @Override
//            public void onPullDown(float progress) { }
//            @Override
//            public void onPullCancel() { }
//
//            @Override
//            public void onPullComplete() {
//                finish();
//                overridePendingTransition(0, R.anim.out_to_bottom);
//            }
//
//            @Override
//            public void onPullUp() {
//                overridePendingTransition(0, R.anim.out_to_top);
//                Intent intent = new Intent(BabyCardActivity.this, AllBabiesActivity.class);
//                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ALL_BABY_LIST);
//            }
//        }).setOutToTop(true);
        mBabyCardPager = findViewById(R.id.baby_card_vp);
        mBabyCardPager.setPageTransformer(true, new ScalePageTransformerCard());
        mBabyCardPager.setOffscreenPageLimit(3);
        mBabyCardAdapter = new BabyCardAdapter(this, babyDataList, mTaskHandler, this);
        mBabyCardPager.setAdapter(mBabyCardAdapter);
        mBabyCardPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                mTaskHandler.clipPagerSelectBaby(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
//        findViewById(R.id.ll_add_baby).setOnClickListener(v -> {
//            Intent intent = new Intent(this, CaptureBindNumberActivity.class);
//            intent.putExtra("ACTIVITY_MODE", "BABYCARD_BIND");
//            intent.putExtra(CaptureBindNumberActivity.PARAM_KEY_MODE, CaptureBindNumberActivity.PARAM_VALUE_MODE_BIND_DEVICE);
//            startActivity(intent);
//        });
    }

    @Override
    protected void onDestroy() {
        babyDataList.clear();
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();
        mTaskHandler.refreshPager();
    }


    private void changeRelation() {
        BabyEntity entity = babyDataList.get(modifyBabyHeardPosition);
        Intent intent = new Intent(this, SelectRelationActivity.class);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_MODE, SelectRelationActivity.PARAM_MODE_SELECT_RELATION);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_RELATION, entity.getRelation());
        intent.putExtra(SelectRelationActivity.PARAM_KEY_AVATAR, entity.getUserAvatar());
        intent.putExtra(SelectRelationActivity.PARAM_KEY_BIND_NUM, entity.getDeviceId());
        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SELECT_RELATION);
    }


    private void showQRCode(BabyEntity entity) {

        View mView = LayoutInflater.from(this).inflate(R.layout.baby_card_rq_code, null);
        final Dialog dialog = createDialog(this, mView);
        dialog.show();
        ((TextView) mView.findViewById(R.id.device_id)).setText(entity.getDeviceId());
        mView.findViewById(R.id.ok).setOnClickListener(v -> dialog.dismiss());
        ImageView qrCode = mView.findViewById(R.id.bd_qrc);
        ViewTreeObserver vto2 = qrCode.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                qrCode.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                try {
                    int w = qrCode.getWidth();
                    int h = qrCode.getHeight();
                    if (w < 1 || h < 1)
                        return;
                    String encodedDeviceId;
                    try {
                        encodedDeviceId = URLEncoder.encode(entity.getDeviceId(), "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        L.e(TAG, "URLEncoder.encode(" + entity.getDeviceId() + ")", e);
                        encodedDeviceId = entity.getDeviceId();
                    }
                    Bitmap bitmap = QRCodeUtils.createCode(BabyCardActivity.this, "https://app.cqkct.com/funkidii?bindnum=" + encodedDeviceId, w, h);
                    qrCode.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    L.e(TAG, "getQrc failure", e);
                }
            }
        });

    }

    //解绑
    private void showUnbind(int position) {
        BabyEntity entity = babyDataList.get(position);
        View mView = LayoutInflater.from(this).inflate(R.layout.baby_card_unbind_device, null);
        final Dialog dialog = createDialog(this, mView);
        dialog.show();

        TextView babyName = mView.findViewById(R.id.baby_name);
        babyName.setText((!StringUtils.isEmpty(entity.getName()) ? entity.getName() : getString(R.string.baby)));
        mView.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
        mView.findViewById(R.id.ok).setOnClickListener(v -> {
            //确定
            popWaitingDialog(R.string.tip_unbinding_device);
            //开始解绑
            model.unbindDevice(entity.getDeviceId(), mUserId, new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    mTaskHandler.refreshPager();
                    popSuccessDialog(R.string.unbind_and_bind_unbind_suc);
                    //最后一个也解绑后跳转去绑定界面
                    List<BabyEntity> babyEntityList = GreenUtils.getBabyEntityDao().loadAll();
                    if (babyEntityList.isEmpty()) {
                        L.e(TAG, "not found bind device go to bind ");
                        Intent intent = new Intent(getApplication(), BindDeviceActivity.class);
                        startActivity(intent);
                    }

                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    popErrorDialog(errorCode == Message.ErrorCode.TIMEOUT ? R.string.request_timed_out : R.string.unbind_and_bind_unbind_failure);
                }
            });
            dialog.dismiss();
        });
    }

    private boolean scrollToCurrentBaby = true;
    private void refreshClipViewPagerData() {
        babyDataList.clear();
        List<BabyEntity> newBabyBeanList = new ArrayList<>();

        String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "refreshClipViewPagerData userId is isEmpty");
            return;
        }
        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> list = dao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .list();
        int curPos = mBabyCardPager.getCurrentItem();

        for (int i = 0; i < list.size(); ++i) {
            BabyEntity babyEntity = list.get(i);
            dao.detach(babyEntity);
            newBabyBeanList.add(babyEntity);
            if (babyEntity.getIs_select()) {
                curPos = i;
            }
        }
        int newSize = newBabyBeanList.size();
        if (curPos >= newSize) {
            curPos = newSize - 1;
        }
        if (newSize != babyDataList.size()) {
            babyDataList.clear();
            babyDataList.addAll(newBabyBeanList);
        } else {
            for (int i = 0; i < newSize; ++i) {
                if (!babyDataList.get(i).equals(newBabyBeanList.get(i))) {
                    babyDataList.clear();
                    babyDataList.addAll(newBabyBeanList);
                    break;
                }
            }
        }
        mClipAdapter.notifyDataSetChanged();
        mBabyCardAdapter.notifyDataSetChanged();

        if (scrollToCurrentBaby) {
            scrollToCurrentBaby = false;
            if (mBabyCardPager.getCurrentItem() != curPos) {
                mTaskHandler.babyCardPagerSelectBaby(curPos);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_camera_permission);
                } else {
                    photoUri = takePhotoByCamera(TAKE_PICTURE);
                }
                break;
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_album_permission);
                } else {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_LOAD_IMAGE);
                }
                break;
            default:
                break;
        }
    }

    private Uri takePhotoByCamera(int startActivityRequestCode) {
        // 创建一个File，用来存储拍照后的照片
        File outputFile = new File(getExternalCacheDir(), "camera.png");
        if (outputFile.exists()) {
            outputFile.delete(); // 删除旧文件
        }
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 7.0 调用系统相机拍照不再允许使用Uri方式，应该替换为FileProvider
            imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", // provider 的 authorities
                    outputFile);
        } else {
            imageUri = Uri.fromFile(outputFile);
        }
        //启动相机程序
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, startActivityRequestCode);
        return imageUri;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_SELECT_RELATION:
                    String newRelation = data.getStringExtra(SelectRelationActivity.RESULT_KEY_NEW_RELATION);
                    String userAvatar = data.getStringExtra(SelectRelationActivity.RESULT_KEY_USER_AVATAR);
                    BabyEntity entity = babyDataList.get(modifyBabyHeardPosition);

                    if (entity.getRelation().equals(newRelation)) {
                        if (!RelationUtils.isCustomRelation(newRelation)) {
                            // not change
                            return;
                        }
                        if (TextUtils.isEmpty(entity.getUserAvatar())) {
                            if (TextUtils.isEmpty(userAvatar)) {
                                // not change
                                return;
                            }
                        } else if (entity.getUserAvatar().equals(userAvatar)) {
                            // not change
                            return;
                        }
                    }

                    popWaitingDialog(R.string.submitting);

                    Message.UsrDevAssoc.Builder builder = Message.UsrDevAssoc.newBuilder()
                            .setDeviceId(entity.getDeviceId())
                            .setUserId(entity.getUserId())
                            .setPermission(Message.UsrDevAssoc.Permission.forNumber(entity.getPermission()))
                            .setRelation(newRelation);
                    if (RelationUtils.isCustomRelation(newRelation) && !TextUtils.isEmpty(userAvatar)) {
                        builder.setAvatar(userAvatar);
                    } else if (!TextUtils.isEmpty(entity.getUserAvatar())) {
                        builder.setAvatar(entity.getUserAvatar());
                    }

                    model.submitBabyRelation(builder.build(), new OperateDataListener() {
                        @Override
                        public void operateSuccess(GeneratedMessageV3 messageV3) {
                            dismissDialog();
                            entity.setRelation(builder.getRelation());
                            entity.setUserAvatar(builder.getAvatar());
                            mBabyCardAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void operateFailure(Message.ErrorCode errorCode) {

                        }
                    });
                    break;
                case RESULT_LOAD_IMAGE:
                    photoUri = data.getData();
                case TAKE_PICTURE: {
                    UCrop.Options options = new UCrop.Options();
                    options.withMaxResultSize(480, 480);
                    options.withAspectRatio(1, 1);
                    options.setHideBottomControls(true);
                    options.setToolbarColor(getResources().getColor(R.color.common_background));
                    options.withImmersionSystemBar(true);
                    options.setToolbarTitle(getString(R.string.crop));
                    options.setToolbarWidgetColor(getResources().getColor(R.color.title_bar_text));
                    try {
                        UCrop.of(photoUri, Uri.fromFile(new File(FileUtils.getExternalStorageImageCacheDirFile(), FileUtils.genBabyHeadIconFilename(babyDataList.get(modifyBabyHeardPosition).getDeviceId()))))
                                .withOptions(options)
                                .start(this, UCropActivity.class, UCROP_REQUEST_CODE);
                    } catch (Exception e) {
                        L.e(TAG, "TAKE_PICTURE UCrop.of Exception: " + e);
                        e.printStackTrace();
                    }
                }
                break;

                case UCROP_REQUEST_CODE: {
                    Uri resultUri = UCrop.getOutput(data);
                    if (resultUri != null) {
                        try {
                            File file;
                            if (resultUri.getScheme().toLowerCase().equals("file")) {
                                file = new File(resultUri.getPath());
                            } else {
                                file = new File(FileUtils.getExternalStorageImageCacheDirFile(), FileUtils.genBabyHeadIconFilename(babyDataList.get(modifyBabyHeardPosition).getDeviceId()));
                                FileUtils.copyFile(this, resultUri, Uri.fromFile(file));
                            }

                            if (file.exists()) {
                                uploadFile = file;
                                babyDataListChanged(uploadFile.getName());
                                BabyEntity babyBean = babyDataList.get(modifyBabyHeardPosition);
                                popWaitingDialog(R.string.please_wait);
                                OkHttpRequestManager.getInstance(this)
                                        .uploadDeviceHeadIcon(uploadFile, babyBean.getUserId(), babyBean.getDeviceId(),
                                                new OkHttpRequestManager.ReqProgressCallBack<String>() {
                                                    @Override
                                                    public void onProgress(long total, long current) {
                                                    }

                                                    @Override
                                                    public void onReqSuccess(String result) {
                                                        try {
                                                            JSONObject jsonObject = new JSONObject(result);
                                                            if (jsonObject.getBoolean("success")) {
                                                                babyBean.setBabyAvatar(uploadFile.getName());
                                                                uploadBabyInfo(babyBean);
                                                                mBabyCardAdapter.notifyDataSetChanged();
                                                                mClipAdapter.notifyDataSetChanged();
                                                                return;
                                                            } else {
                                                                L.w(TAG, "uploadDeviceHeadIcon() onReqSuccess() processs failure: " + jsonObject.getString("description"));
                                                            }
                                                        } catch (JSONException e) {
                                                            L.e(TAG, "uploadDeviceHeadIcon() onReqSuccess() processs failure", e);
                                                        }
                                                        popErrorDialog(R.string.baby_card_up_head_icon_failed);
                                                    }

                                                    @Override
                                                    public void onReqFailed(String errorMsg) {
                                                        popErrorDialog(R.string.baby_card_up_head_icon_failed);
                                                    }
                                                });
                            }
                        } catch (Exception e) {
                            L.e(TAG, "UCROP_REQUEST_CODE", e);
                        }
                    } else {
                        L.e(TAG, "返回裁剪图片URl出错....");
                    }
                }
                break;
            }
        }
        if (requestCode == ACTIVITY_REQUEST_CODE_ALL_BABY_LIST) {
            // 将拖拽的视图回复原样
            mPullBackLayout.reset();
            if (resultCode == RESULT_OK) {
                scrollToCurrentBaby = true;
            }
        }
    }


    private void babyDataListChanged(String fileName) {
        for (int i = 0; i < babyDataList.size(); i++) {
            if (i == modifyBabyHeardPosition) {
                babyDataList.get(i).setBabyAvatar(fileName);
                mBabyCardAdapter.notifyDataSetChanged();
                mClipAdapter.notifyDataSetChanged();
            }
        }
    }

    private void changeBabyHead() {
        ChooseAvatarWayDialogFragment dialogFragment = new ChooseAvatarWayDialogFragment();
        dialogFragment.setFromCameraOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(BabyCardActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BabyCardActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            } else {
                photoUri = takePhotoByCamera(TAKE_PICTURE);
            }
        });
        dialogFragment.setFromAlbumOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(BabyCardActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BabyCardActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
        dialogFragment.show(getSupportFragmentManager(), "BabyCardChooseAvatarWayDialog");
    }

    @Override
    public void dataChanged(BabyEntity entity, int position) {
        popWaitingDialog(R.string.loading);
        if (uploadBabyInfo(entity)) {
            mBabyCardAdapter.notifyDataSetChanged();
        } else {
            mTaskHandler.refreshPager();
        }
    }

    @Override
    public void showErrorMessage(int id) {
        toast(id);
    }

    private boolean uploadBabyInfo(BabyEntity entity) {
        final boolean[] result = {false};
        model.submitBabyInformationToService(entity, new OperateDataListener() {
            @Override
            public void operateSuccess(GeneratedMessageV3 rspMsg) {
                Message.PushDevConfReqMsg pushConfigReqMsg = (Message.PushDevConfReqMsg) rspMsg;
                dismissDialog();
                GreenUtils.saveConfigs(pushConfigReqMsg.getConf(), pushConfigReqMsg.getFlag(), pushConfigReqMsg.getDeviceId());
                result[0] = true;
            }

            @Override
            public void operateFailure(Message.ErrorCode errorCode) {
                popErrorDialog(errorCode == Message.ErrorCode.TIMEOUT ? R.string.time_out_to_save : R.string.fail_to_save);
                result[0] = false;
            }
        });
        return result[0];
    }


    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        super.onCurrentBabyChanged(oldBabyBean, newBabyBean, isSticky);
        mTaskHandler.refreshPager();
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        mTaskHandler.refreshPager();
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        mTaskHandler.refreshPager();
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        mTaskHandler.refreshPager();
    }

    @Override
    public void onLoggedin(TlcService tlcService, @NonNull String userId, boolean isSticky) {
        super.onLoggedin(tlcService, userId, isSticky);
        mTaskHandler.refreshPager();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDeviceOnline(Event.DeviceOnline ev) {
        mClipAdapter.notifyDataSetChanged();
    }
}

