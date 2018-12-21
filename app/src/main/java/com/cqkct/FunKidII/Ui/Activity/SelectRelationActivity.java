package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.BlurActivity.BaseBlurActivity;
import com.cqkct.FunKidII.Ui.fragment.ChooseAvatarWayDialogFragment;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.LengthLimitTextWatcher;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.RelationAvatar;
import com.cqkct.FunKidII.service.OkHttpRequestManager;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.ucrop.UCropActivity;
import com.cqkct.FunKidII.zxing.capture.CaptureBindNumberActivity;
import com.gyf.barlibrary.ImmersionBar;
import com.gyf.barlibrary.OnKeyboardListener;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/9/2.
 */

public class SelectRelationActivity extends BaseBlurActivity implements OnKeyboardListener {
    public static final String TAG = SelectRelationActivity.class.getSimpleName();

    /**
     * Activity 模式
     */
    public static final String PARAM_KEY_MODE = "mode";
    /**
     * 选择关系模式：需要的参数为 PARAM_KEY_RELATION，可能还需要 PARAM_KEY_AVATAR
     */
    public static final int PARAM_MODE_SELECT_RELATION = 0;
    /**
     * 绑定模式：需要的参数为 PARAM_KEY_BIND_NUM
     */
    public static final int PARAM_MODE_BIND = 1;

    /**
     * 原先的关系 （String）
     */
    public static final String PARAM_KEY_RELATION = "relation";
    public static final String PARAM_KEY_AVATAR = "avatar"; // 原先的用户头像
    public static final String RESULT_ACTION_RELATION = SelectRelationActivity.class.getName() + ".RELATION";
    /**
     * 之前的关系 （String）
     */
    public static final String RESULT_KEY_OLD_RELATION = "old_relation";
    /**
     * 新的关系 （String）
     */
    public static final String RESULT_KEY_NEW_RELATION = "new_relation";
    public static final String RESULT_KEY_USER_AVATAR = "RESULT_KEY_USER_AVATAR";

    /**
     * 绑定号
     */
    public static final String PARAM_KEY_BIND_NUM = "bind_num";
    public static final String RESULT_ACTION_BIND = SelectRelationActivity.class.getName() + ".BIND";
    public static final String RESULT_KEY_BIND_NUM = "bind_num";
    public static final String RESULT_KEY_ALREADY_BIND = "already_bind";
    /**
     * 绑定成功后的结果（绑定关系：UsrDevInfo）
     */
    public static final String RESULT_KEY_USER_OF_DEV = "user_of_device";


    int mode;
    private String oldRelation;
    private String bindNum;
    private String relation;
    private String avatarFilename;
    private File avatarFile;

    ConstraintLayout contentView;

    private ImageView imageViewBaba, imageViewMama, imageViewJiejie, imageViewYeye, imageViewNainai,
            imageViewGege, imageViewWaigong, imageViewWaipo, imageViewLaoshi;
    //------------------------
    private Button defaultRelationsButton;
    private Button customRelationButton;
    private View defaultRelationsPanel;
    private View customRelationPanel;
    //    ------------自定义关系
    private static final int TAKE_PICTURE = 0;//相机选择头像-返回
    private static final int RESULT_LOAD_IMAGE = 1;//相册选择头像-返回
    private static final int UCROP_REQUEST_CODE = 2;//裁剪头像-返回

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private Uri photoUri;
    private ImageView customRelationAvatarView;

    private EditText customRelation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_relation);
        setTitleBarTitle(R.string.select_relation);
        L.v("Az", RelationUtils.RELATION_BABA);
        processParams();

        initView();
    }

    @Override
    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.keyboardEnable(true).setOnKeyboardListener(this).init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mode == PARAM_MODE_BIND && TextUtils.isEmpty(bindNum)) {
            popErrorDialog(R.string.bind_device_no_bind_number);
        }
    }

    @Override
    public void onKeyboardChange(boolean isPopup, int keyboardHeight) {
        if (isPopup) {
            lockViewHeight();
        } else {
            unlockViewHeight();
        }
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.button_positive:
                hideSoftKeyBoard();
                choose();
                return;
            case R.id.button_negative:
                hideSoftKeyBoard();
                finish();
                break;

            case R.id.baba_layout:
                if (!relation.equals(RelationUtils.RELATION_BABA)) {
                    relation = RelationUtils.RELATION_BABA;
                    setView(relation);
                }
                break;
            case R.id.mama_layout:
                if (!relation.equals(RelationUtils.RELATION_MAMA)) {
                    relation = RelationUtils.RELATION_MAMA;
                    setView(relation);
                }
                break;
            case R.id.jiejie_layout:
                if (!relation.equals(RelationUtils.RELATION_JIEJIE)) {
                    relation = RelationUtils.RELATION_JIEJIE;
                    setView(relation);
                }
                break;
            case R.id.yeye_layout:
                if (!relation.equals(RelationUtils.RELATION_YEYE)) {
                    relation = RelationUtils.RELATION_YEYE;
                    setView(relation);
                }
                break;
            case R.id.nainai_layout:
                if (!relation.equals(RelationUtils.RELATION_NAINAI)) {
                    relation = RelationUtils.RELATION_NAINAI;
                    setView(relation);
                }
                break;
            case R.id.gege_layout:
                if (!relation.equals(RelationUtils.RELATION_GEGE)) {
                    relation = RelationUtils.RELATION_GEGE;
                    setView(relation);
                }
                break;
            case R.id.waigong_layout:
                if (!relation.equals(RelationUtils.RELATION_WAIGONG)) {
                    relation = RelationUtils.RELATION_WAIGONG;
                    setView(relation);
                }
                break;
            case R.id.waipo_layout:
                if (!relation.equals(RelationUtils.RELATION_WAIPO)) {
                    relation = RelationUtils.RELATION_WAIPO;
                    setView(relation);
                }
                break;
            case R.id.laoshi_layout:
                if (!relation.equals(RelationUtils.RELATION_LAOSHI)) {
                    relation = RelationUtils.RELATION_LAOSHI;
                    setView(relation);
                }
                break;

            case R.id.custom_relation_button:
                switchToCustomRelationPanel();
                return;
            case R.id.default_relations_button:
                switchToDefaultRelationsPanel();
                return;
            case R.id.custom_avatar:
            case R.id.custom_avatar_text:
                hideSoftKeyBoard();
                changeBabyHead();
                return;
        }

//        if (mode == PARAM_MODE_SELECT_RELATION) {
//            setResultAndFinishForRelation();
//        }
    }

    private void choose() {
        if (customRelationPanel.getVisibility() == View.VISIBLE) {
            //自定义
            String relationTmp = customRelation.getText().toString().trim();
            if (TextUtils.isEmpty(relationTmp)) {
                toast(R.string.bind_device_custom_not_null);
                return;
            }
            relation = relationTmp;
        } else if (TextUtils.isEmpty(relation)) {
            toast(R.string.please_choose_relation);
            return;
        }

        if (RelationUtils.isCustomRelation(relation) && avatarFile != null) {
            uploadUserHeadIcon(avatarFile, mUserId);
        } else {
            doChoose(false);
        }
    }

    private void doChoose(boolean alreadyHasWaitingDialog) {
        switch (mode) {
            case PARAM_MODE_BIND:
                bindDevice(alreadyHasWaitingDialog);
                break;
            default:
                if (alreadyHasWaitingDialog) {
                    dismissDialog();
                }
                setResultAndFinishForRelation();
                break;
        }
    }

    private boolean isViewHeightLocked;

    private void lockViewHeight() {
        if (isViewHeightLocked)
            return;
        isViewHeightLocked = true;

        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.height = contentView.getHeight() * 4 / 5;
        contentView.setLayoutParams(layoutParams);

//        ConstraintSet set = new ConstraintSet();
//        set.clone(content);
//        set.connect(R.id.content, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) x);
//        set.constrainHeight(R.id.content, h);
//        set.applyTo(content);
    }

    private void unlockViewHeight() {
        if (!isViewHeightLocked)
            return;
        isViewHeightLocked = false;

        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.height = 0;
        contentView.setLayoutParams(layoutParams);
    }

    private void processParams() {
        Intent intent = getIntent();
        mode = intent.getIntExtra(PARAM_KEY_MODE, PARAM_MODE_SELECT_RELATION);
        oldRelation = intent.getStringExtra(PARAM_KEY_RELATION);
        avatarFilename = intent.getStringExtra(PARAM_KEY_AVATAR);
        bindNum = intent.getStringExtra(PARAM_KEY_BIND_NUM);
        L.d(TAG, "param: mode：" + mode + ", relation: " + oldRelation + ", bindNum: " + bindNum);
        relation = oldRelation;
        if (relation == null)
            relation = "";
    }

    private void initView() {
        TextView titleView = findViewById(R.id.dialog_title);
        titleView.setText(R.string.bind_edit_identity);

        contentView = findViewById(R.id.content);

        findViewById(R.id.button_positive).setOnClickListener(getDebouncedOnClickListener());
        findViewById(R.id.button_negative).setOnClickListener(getDebouncedOnClickListener());

        defaultRelationsButton = findViewById(R.id.default_relations_button);
        defaultRelationsButton.setBackground(getResources().getDrawable(R.drawable.text_red_background_two));
        defaultRelationsButton.setTextColor(Color.WHITE);

        customRelationButton = findViewById(R.id.custom_relation_button);
        customRelationButton.setBackground(getResources().getDrawable(R.drawable.bind_select_relation_common_bg));

        defaultRelationsPanel = findViewById(R.id.default_relations_panel);
        customRelationPanel = findViewById(R.id.custom_relation_panel);

        customRelationAvatarView = findViewById(R.id.custom_avatar);

        customRelation = findViewById(R.id.et_input_name);
        customRelation.setFilters(new InputFilter[]{emojiFilter});
        customRelation.addTextChangedListener(new LengthLimitTextWatcher(customRelation, getResources().getInteger(R.integer.maxLength_of_custom_relation)));


        imageViewBaba = findViewById(R.id.baba_icon);
        imageViewMama = findViewById(R.id.mama_icon);
        imageViewJiejie = findViewById(R.id.jiejie_icon);

        imageViewYeye = findViewById(R.id.yeye_icon);
        imageViewNainai = findViewById(R.id.nainai_icon);
        imageViewGege = findViewById(R.id.gege_icon);

        imageViewWaigong = findViewById(R.id.waigong_icon);
        imageViewWaipo = findViewById(R.id.waipo_icon);
        imageViewLaoshi = findViewById(R.id.laoshi_icon);

        if (mode == PARAM_MODE_BIND && TextUtils.isEmpty(oldRelation)) {
            resetView();
        } else {
            setView(relation);
        }
    }

    private void setView(String relation) {
        if (TextUtils.isEmpty(relation)) {
            relation = "";
        }
        switch (relation) {
            case RelationUtils.RELATION_BABA:
                setView(imageViewBaba);
                break;
            case RelationUtils.RELATION_MAMA:
                setView(imageViewMama);
                break;
            case RelationUtils.RELATION_JIEJIE:
                setView(imageViewJiejie);
                break;
            case RelationUtils.RELATION_YEYE:
                setView(imageViewYeye);
                break;
            case RelationUtils.RELATION_NAINAI:
                setView(imageViewNainai);
                break;
            case RelationUtils.RELATION_GEGE:
                setView(imageViewGege);
                break;
            case RelationUtils.RELATION_WAIGONG:
                setView(imageViewWaigong);
                break;
            case RelationUtils.RELATION_WAIPO:
                setView(imageViewWaipo);
                break;
            case RelationUtils.RELATION_LAOSHI:
                setView(imageViewLaoshi);
                break;
            default:
                // 自定义关系
                switchToCustomRelationPanel();
                initCustomRelationPanel(relation, avatarFilename);
                break;
        }
    }

    private void setView(ImageView imageView) {
        resetView();

        if (imageView != null) {
            imageView.getBackground().setAlpha(100);
            imageView.setImageResource(R.drawable.card_choose);
        }
    }

    private void resetView() {
        switchToDefaultRelationsPanel();

        imageViewBaba.getBackground().mutate().setAlpha(255);
        imageViewMama.getBackground().mutate().setAlpha(255);
        imageViewJiejie.getBackground().mutate().setAlpha(255);
        imageViewYeye.getBackground().mutate().setAlpha(255);
        imageViewNainai.getBackground().mutate().setAlpha(255);
        imageViewGege.getBackground().mutate().setAlpha(255);
        imageViewWaigong.getBackground().mutate().setAlpha(255);
        imageViewWaipo.getBackground().mutate().setAlpha(255);
        imageViewLaoshi.getBackground().mutate().setAlpha(255);

        imageViewBaba.setImageResource(0);
        imageViewMama.setImageResource(0);
        imageViewJiejie.setImageResource(0);
        imageViewYeye.setImageResource(0);
        imageViewNainai.setImageResource(0);
        imageViewGege.setImageResource(0);
        imageViewWaigong.setImageResource(0);
        imageViewWaipo.setImageResource(0);
        imageViewLaoshi.setImageResource(0);
    }

    private void switchToDefaultRelationsPanel() {
        hideSoftKeyBoard();
        defaultRelationsPanel.setVisibility(View.VISIBLE);
        customRelationPanel.setVisibility(View.GONE);
        customRelationButton.setBackground(getResources().getDrawable(R.drawable.bind_select_relation_common_bg));
        defaultRelationsButton.setBackground(getResources().getDrawable(R.drawable.text_red_background_two));
        defaultRelationsButton.setTextColor(Color.WHITE);
        customRelationButton.setTextColor(getResources().getColor(R.color.text_color_four));
    }

    private void switchToCustomRelationPanel() {
        customRelationPanel.setVisibility(View.VISIBLE);
        defaultRelationsPanel.setVisibility(View.GONE);
        defaultRelationsButton.setBackground(getResources().getDrawable(R.drawable.bind_select_relation_common_bg));
        customRelationButton.setBackground(getResources().getDrawable(R.drawable.text_red_background_two));
        customRelationButton.setTextColor(Color.WHITE);
        defaultRelationsButton.setTextColor(getResources().getColor(R.color.text_color_four));
    }

    private void initCustomRelationPanel(String relation, String avatar) {
        Glide.with(customRelationAvatarView)
                .load(new RelationAvatar(null, mUserId, avatar))
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.placeholderOf(customRelationAvatarView.getDrawable()))
                .apply(RequestOptions.skipMemoryCacheOf(false))
                .apply(RequestOptions.errorOf(R.drawable.head_relation))
                .into(customRelationAvatarView);

        customRelation.setText(relation);
        customRelation.setSelection(customRelation.length());
    }

    InputFilter emojiFilter = new EmojiInputFilter();

    private void editCustomRelation(String customRel) {
        String customRelation_str = customRelation.getText().toString();
        customRelation.setText(customRel);//将原来的关系显示到EditText
        customRelation.setSelection(customRelation_str.length());//将光标移动至文字末尾

    }

    private void uploadUserHeadIcon(File file, String userId) {
        popWaitingDialog(R.string.please_wait);

        OkHttpRequestManager.getInstance(this)
                .uploadDeviceHeadIcon(file, userId, userId,
                        new OkHttpRequestManager.ReqProgressCallBack<String>() {
                            @Override
                            public void onProgress(long total, long current) {
                            }

                            @Override
                            public void onReqSuccess(String result) {
                                try {
                                    JSONObject jsonObject = new JSONObject(result);
                                    if (jsonObject.getBoolean("success")) {
                                        doChoose(true);
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

    private void bindDevice(boolean alreadyHasWaitingDialog) {
        if (alreadyHasWaitingDialog) {
            updateDialogText(R.string.bind_device_binding);
        } else {
            popWaitingDialog(R.string.bind_device_binding);
        }

        Message.UsrDevAssoc.Builder builder = Message.UsrDevAssoc.newBuilder()
                .setDeviceId(bindNum)
                .setUserId(mUserId)
                .setPermission(Message.UsrDevAssoc.Permission.NORMAL)
                .setRelation(relation);
        if (!TextUtils.isEmpty(avatarFilename)) {
            builder.setAvatar(avatarFilename);
        }
        L.e(TAG, "UsrDevAssoc: " + builder);
        exec(
                Message.BindDevReqMsg.newBuilder()
                        .setUsrDevAssoc(builder
                                .build())
                        .build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.BindDevRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "bindDevice() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case ALREADY_EXISTS:
                                    L.i(TAG, "bindDevice() -> exec() -> onResponse(): EXISTS 已绑定");
                                    setResultAndFinishForBind(true, rspMsg.getUsrDevAssoc());
                                    break;
                                case SUCCESS:
                                    L.i(TAG, "bindDevice() -> exec() -> onResponse(): SUCCESS 已绑定");
                                    setResultAndFinishForBind(true, rspMsg.getUsrDevAssoc());
                                    break;
                                case IN_PROGRESS:
                                    popSuccessDialog(R.string.bind_device_wait_administrator_agree);
                                    setResultAndFinishForBind(false, null);
                                    break;
                                default:
                                    popErrorDialog(R.string.bind_device_bind_fail);
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "bindDevice() -> exec() -> onResponse() process failure", e);
                            popErrorDialog(R.string.bind_device_bind_fail);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "bindDevice() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.submit_timeout);
                        } else {
                            popErrorDialog(R.string.bind_device_bind_fail);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }

    private void setResultAndFinishForBind(boolean alreadyBind, protocol.Message.UsrDevAssoc uda) {
        Intent intent = new Intent(RESULT_ACTION_BIND);
        intent.putExtra(RESULT_KEY_BIND_NUM, bindNum);
        intent.putExtra(RESULT_KEY_ALREADY_BIND, alreadyBind);
        if (uda != null) {
            intent.putExtra(RESULT_KEY_USER_OF_DEV, uda);
        }
        setResult(RESULT_OK, intent);
        finish();
        //绑定后关掉二维码扫描界面
        if (isBabyCardBind()) {
            if (null != InputBindNumberActivity.ActivityInputBindNumber)
                CaptureBindNumberActivity.ActivityCapture.finish();
        }
    }

    private void setResultAndFinishForRelation() {
        Intent intent = new Intent(RESULT_ACTION_RELATION);
        intent.putExtra(RESULT_KEY_OLD_RELATION, oldRelation);
        intent.putExtra(RESULT_KEY_NEW_RELATION, relation);
        if (RelationUtils.isCustomRelation(relation) && !TextUtils.isEmpty(avatarFilename)) {
            intent.putExtra(RESULT_KEY_USER_AVATAR, avatarFilename);
        }
        setResult(RESULT_OK, intent);
        finish();
        //绑定后关掉二维码扫描界面
        if (isBabyCardBind()) {
            if (null != InputBindNumberActivity.ActivityInputBindNumber)
                CaptureBindNumberActivity.ActivityCapture.finish();
        }
    }

    private boolean isBabyCardBind() {
        Intent intent1 = getIntent();
        Bundle extras = intent1.getExtras();
        String activity_mode = null;
        if (extras != null) {
            activity_mode = extras.getString("ACTIVITY_MODE");
        }
        if (activity_mode != null) {
            return activity_mode.equals("BABYCARD_BIND");
        }
        return false;
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return mode == PARAM_MODE_SELECT_RELATION;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return mode == PARAM_MODE_SELECT_RELATION;
    }

    //    -----------------------------自定义关系 头像、关系-------
    private void changeBabyHead() {
        ChooseAvatarWayDialogFragment dialogFragment = new ChooseAvatarWayDialogFragment();
        dialogFragment.setFromCameraOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(SelectRelationActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SelectRelationActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            } else {
                photoUri = takePhotoByCamera(TAKE_PICTURE);
            }
        });
        dialogFragment.setFromAlbumOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(SelectRelationActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SelectRelationActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
        dialogFragment.show(getSupportFragmentManager(), "CustomRelationChooseAvatarWayDialog");
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
                    UCrop.of(photoUri, Uri.fromFile(new File(FileUtils.getExternalStorageImageCacheDirFile(), FileUtils.genUserHeadIconFilename(mUserId))))
                            .withOptions(options)
                            .start(this, UCropActivity.class, UCROP_REQUEST_CODE);
                }
                break;

                case UCROP_REQUEST_CODE: {
                    Uri resultUri = UCrop.getOutput(data);
                    if (resultUri != null) {
                        try {
                            if (resultUri.getScheme().toLowerCase().equals("file")) {
                                avatarFile = new File(resultUri.getPath());
                            } else {
                                avatarFile = new File(FileUtils.getExternalStorageImageCacheDirFile(), FileUtils.genUserHeadIconFilename(mUserId));
                                FileUtils.copyFile(this, resultUri, Uri.fromFile(avatarFile));
                            }

                            if (avatarFile.exists()) {
                                avatarFilename = avatarFile.getName();
                                Glide.with(customRelationAvatarView)
                                        .load(avatarFile)
                                        .apply(RequestOptions.circleCropTransform())
                                        .apply(RequestOptions.placeholderOf(customRelationAvatarView.getDrawable()))
                                        .apply(RequestOptions.skipMemoryCacheOf(false))
                                        .apply(RequestOptions.errorOf(R.drawable.head_relation))
                                        .into(customRelationAvatarView);
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
}
