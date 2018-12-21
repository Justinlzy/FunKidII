package com.cqkct.FunKidII.Ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MainActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.ClassDisableActivity;
import com.cqkct.FunKidII.Ui.Adapter.CallAllBabiesAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.Rom;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;


public class CallDialogFragment extends DialogFragment {

    public static final String TAG = CallDialogFragment.class.getSimpleName();

    private String mUserId;
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 1;
    private List<BabyEntity> list = new ArrayList<>();
    private CallDialogFragmentListener listener;

    public interface CallDialogFragmentListener{
        void onSetBabyNumberListener(BabyEntity entity);
    }

    public CallDialogFragment setCallDialogFragmentListener(CallDialogFragmentListener callDialogFragmentListener){
        this.listener = callDialogFragmentListener;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dismiss();
        }
        Bundle bundle = getArguments();
        mUserId = bundle.getString(MainActivity.USER_ID);
    }

    @Override
    public void onResume() {
        super.onResume();

        MobclickAgent.onPageStart(getClass().getName());
    }

    @Override
    public void onPause() {
        super.onPause();

        MobclickAgent.onPageEnd(getClass().getName());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getActivity(), R.style.FunKidII_2_Dialog_style);

        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        window.setDimAmount(0.85f);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.windowAnimation);
        dialog.setContentView(R.layout.call_dialog_fragment_layout);
        initView(dialog);
        return dialog;
    }


    private void initView(AppCompatDialog dialog) {
        dialog.findViewById(R.id.cancel).setOnClickListener(v -> dismiss());

        RecyclerView recyclerView = dialog.findViewById(R.id.all_baby_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setReverseLayout(true); //列表翻转

        assert recyclerView != null;
        recyclerView.setLayoutManager(layoutManager);

        CallAllBabiesAdapter activityAdapter = new CallAllBabiesAdapter(list, pos -> {
            BabyEntity entity = list.get(pos);
            if (TextUtils.isEmpty(entity.getDeviceId()) || TextUtils.isEmpty(entity.getUserId()))
                return;

        showCallDialog(entity);
        }, R.layout.all_baby_list, getContext());
        recyclerView.setAdapter(activityAdapter);

        list.clear();
        list.addAll(getClipViewPagerData());

        if (list.size() % 4 > 0) {
            int line = list.size() / 4;

            switch (list.size() % 4) {
                case 1:
                    list.add(line * 4, new BabyEntity());
                    break;
                case 2:
                    list.add(line * 4, new BabyEntity());
                    break;
                case 3:
                    list.add(line * 4, new BabyEntity());
                    break;
                default:
                    break;
            }
        }
        activityAdapter.notifyDataSetChanged();
    }


    private void tryMakeCall(BabyEntity entity) {
        if (entity == null) {
            L.w(TAG, "tryMakeCall BabyEntity == null");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(entity.getDeviceId());
        if (deviceInfo == null) {
            L.e(TAG, "tryMakeCall: no this device!!!");
            return;
        }

        String num = deviceInfo.getBaby().getPhone();
        L.v(TAG, "tryMakeCall baby phone: " + num);
        if (TextUtils.isEmpty(num)) {
            if (entity.getPermission() > 0) {
                showSetBabyNumber(entity);
            } else {
                showNoHasEditPermission();
            }
            return;
        }
        call(entity);
    }

    private void showNoHasEditPermission() {
        Dialog dialog = new Dialog(getContext(), R.style.TimePickerDialog);

        View mView = LayoutInflater.from(getContext()).inflate(R.layout.confirm_dialog_fragment_layout, null);
        TextView tvMessage = mView.findViewById(R.id.message);
        tvMessage.setText(R.string.your_baby_number_no_hasEditPermission);
        mView.findViewById(R.id.button_positive).setOnClickListener(v -> dialog.dismiss());
        mView.findViewById(R.id.button_negative).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(mView);
        dialog.show();
    }


    private void showSetBabyNumber(BabyEntity entity){
        Dialog dialog = new Dialog(getContext(), R.style.TimePickerDialog);

        View mView = LayoutInflater.from(getContext()).inflate(R.layout.add_baby_phonenumber_dialog_fragment_layout, null);
        EditText tvNumber = mView.findViewById(R.id.number);
        ((TextView)mView.findViewById(R.id.dialog_title)).setText(R.string.watch_number);
        mView.findViewById(R.id.button_positive).setOnClickListener(v -> {
            String number = tvNumber.getText().toString().trim();
            if (TextUtils.isEmpty(number)) {
                Toast.makeText(getContext(), getContext().getText(R.string.please_input_baby_phone), Toast.LENGTH_SHORT).show();
                return;
            }
            entity.setPhone(number);
            listener.onSetBabyNumberListener(entity);
            dialog.dismiss();
        });
        mView.findViewById(R.id.button_negative).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(mView);
        dialog.show();

    }


    private void showCallDialog(BabyEntity entity) {
        Dialog dialog = new Dialog(getContext(),  R.style.TimePickerDialog);

        View mView = LayoutInflater.from(getContext()).inflate(R.layout.confirm_dialog_fragment_layout, null);
        TextView tvMessage = mView.findViewById(R.id.message);
        tvMessage.setText(R.string.main_sure_call_for_baby);
        mView.findViewById(R.id.button_positive).setOnClickListener(v -> {
            dialog.dismiss();
            tryMakeCall(entity);
        });
        mView.findViewById(R.id.button_negative).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(mView);
        dialog.show();

    }


    public void call(BabyEntity entity) {
        if (PublicTools.isInClassDisable(GreenUtils.QueryClassDisableEntities(entity.getDeviceId()))) {
            ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                    .setMessage(getString(R.string.main_class_disable_open))
                    .setPositiveButton(getString(R.string.ok), (dialog12, which) -> {
                        GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
                        startActivity(new Intent(getContext(), ClassDisableActivity.class));
                    })
                    .setNegativeButton(getString(R.string.cancel), null);
            dialogFragment.show(getFragmentManager(), "ConfirmDialogFragment");

        } else {
            if (Rom.isFlyme()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + entity.getPhone()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_CALL_TO_DEVICE);
            } else if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
            } else {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + entity.getPhone()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_CALL_TO_DEVICE);
            }
        }
    }

    public List<BabyEntity> getClipViewPagerData() {
        List<BabyEntity> babyDataList = new ArrayList<>();

        if (TextUtils.isEmpty(mUserId)) {
            return null;
        }
        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(mUserId))
                .build().list();

        babyDataList.addAll(list);
        return babyDataList;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}
