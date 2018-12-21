package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.PraiseEditViewAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Dao.CollectPraiseEntityDao;
import com.cqkct.FunKidII.db.Entity.CollectPraiseEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

import protocol.Message;

/**
 * Created by T on 2018/3/19.
 */

public class CollectPraiseEditActivity extends BaseActivity {
    private static final String TAG = CollectPraiseEditActivity.class.getSimpleName();

    private Message.Praise originPraise;
    private Message.Praise.Builder editPraise = Message.Praise.newBuilder();

    private LinearLayout collectPraiseing, addGiftBtn;
    private TextView collectPraiseName, scheduleTv;
    private TextView praiseRebuildName;

    private PraiseEditViewAdapter mEditPraiseViewAdapter;
    private List<PraiseEditViewAdapter.Item> mViewItemList = new ArrayList<>();
//    private List<Message.Praise> mHistoryPraiseList = new ArrayList<>();

    public static class PrizeEditorViewHolder {
        public View view;
        public ViewGroup parent;
    }

    private Button button;
    InputFilter emojiFilter = new EmojiInputFilter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_praise_edit);
        setTitleBarTitle(R.string.edit_collect_praise);
        initView();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        Serializable praiseSerializable = intent.getSerializableExtra(CollectPraiseActivity.PARAM_PRAISE);
        setOriginPraise((Message.Praise) praiseSerializable);
        if (originPraise == null) {
            loadData();
        }
        updateView();
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.title_bar_right_text:
                startActivity(new Intent(this, CollectPraiseHistoryActivity.class));
                break;

            case R.id.edit_praise_bt:
                if (originPraise == null) {
                    addPraiseNumberDialog();
                } else {
                    ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                            .setTitle(getString(R.string.collect_hint))
                            .setMessage(getString(R.string.collect_sure_cancel_cp))
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> cancelPraise())
                            .setNegativeButton(getString(R.string.cancel), null);
                    dialogFragment.show(getSupportFragmentManager(), "showCancelPraiseDialog");
                }
                break;
            case R.id.set_collect_name:
            case R.id.collect_praise_rebuild:
                showPraiseName();
                break;
        }
    }

    private void initView() {
        findViewById(R.id.title_bar_right_text).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.title_bar_right_text)).setText(R.string.collect_history);
        button = findViewById(R.id.edit_praise_bt);

        collectPraiseing = findViewById(R.id.collect_praise_ing);
        addGiftBtn = findViewById(R.id.ll_add_gift_name);
        collectPraiseName = findViewById(R.id.collect_praise_name);
        scheduleTv = findViewById(R.id.collect_praise_schedule);

        praiseRebuildName = findViewById(R.id.collect_praise_rebuild);


        SwipeMenuRecyclerView recyclerView = findViewById(R.id.list);
        mEditPraiseViewAdapter = new PraiseEditViewAdapter(mViewItemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator((swipeLeftMenu, swipeRightMenu, viewType) -> {
            if (viewType == PraiseEditViewAdapter.Item.ITEM_HEAD)
                return;
            if (hasEditPermission()) {
                int width = getResources().getDimensionPixelSize(R.dimen.dp_70);
                int height = ViewGroup.LayoutParams.MATCH_PARENT;
                SwipeMenuItem deleteItem = new SwipeMenuItem(CollectPraiseEditActivity.this)
                        .setImage(R.drawable.delete_left_slip)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(deleteItem);// 添加菜单到右侧。
            }
        });
        //右菜单删除
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            int position = menuBridge.getAdapterPosition();
            PraiseEditViewAdapter.Item viewItem = mViewItemList.get(position);
            Message.Praise.Item.Builder praiseItem = (Message.Praise.Item.Builder) viewItem.dat;
            if (!TextUtils.isEmpty(praiseItem.getId())) {
                // 以前就有该项
                // 标记为删除
                praiseItem.setIsDelete(true);
            } else {
                // 这是后来添加的项
                // 应该完整移除
                for (int i = 0; i < editPraise.getItemCount(); ++i) {
                    if (editPraise.getItemBuilder(i) == viewItem.dat) {
                        editPraise.removeItem(i);
                        break;
                    }
                }
            }
            if (originPraise != null) {
                CollectPraiseEditActivity.this.modifyPraise();
            } else {
                mViewItemList.remove(position);
                CollectPraiseEditActivity.this.updateView();
                mEditPraiseViewAdapter.notifyDataSetChanged();
            }
        });
        recyclerView.setAdapter(mEditPraiseViewAdapter);
        mEditPraiseViewAdapter.setItemEditable(hasEditPermission());

        mEditPraiseViewAdapter.addPraiseEditNameClickListener((new PraiseEditViewAdapter.OnPraiseEditNameClickListener() {
            @Override
            public void onPraiseEditNameClick(View view, int position) {
                showAddPraiseTask();
            }

            @Override
            public void nPraiseEditItemClick(View view, int position) {
                if (mViewItemList.get(position).type != PraiseEditViewAdapter.Item.ITEM_ITEM) {
                    return;
                }
                if (editPraise.getCompleteTime() == 0) {
                    showEditTask(mViewItemList.get(position));
                }
            }
        }));
    }

    private void setOriginPraise(Message.Praise praise) {
        if (praise != null && praise.equals(originPraise)) {
            return;
        }
        originPraise = praise;
        if (originPraise != null) {
            editPraise = originPraise.toBuilder();
        } else {
            editPraise = Message.Praise.newBuilder();
        }
    }

    private void loadData() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadData: deviceId is empty");
            return;
        }
        List<CollectPraiseEntity> listInDb = App.getInstance().getDaoSession().getCollectPraiseEntityDao()
                .queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId))
                .orderDesc(CollectPraiseEntityDao.Properties.StartTime)
                .list();

        for (CollectPraiseEntity entity : listInDb) {
            if (originPraise == null) {
                Message.Praise praise = entity.getPraise();
                if (praise.getFinishTime() == 0 && praise.getStartTime() != 0) {
                    setOriginPraise(praise);
                    break;
                }
            }
        }
    }

    private void updateView() {
        button.setText(originPraise == null ? R.string.collect_begin_collect : R.string.collect_cancel_collect);

        scheduleTv.setText(getString(R.string.collect_praise_schedule) + editPraise.getTotalReached() + "/" + editPraise.getTotalGoal());
        if (editPraise.getPrize().isEmpty()) {
            addGiftBtn.setVisibility(View.VISIBLE);
            collectPraiseing.setVisibility(View.GONE);
        } else {
            addGiftBtn.setVisibility(View.GONE);
            collectPraiseing.setVisibility(View.VISIBLE);
            collectPraiseName.setText(editPraise.getPrize());
            if (editPraise.getTotalGoal() == 0) {
                praiseRebuildName.setVisibility(View.VISIBLE);
                scheduleTv.setVisibility(View.GONE);
            }
        }

        mViewItemList.clear();
        mViewItemList.add(mEditPraiseViewAdapter.new Item(PraiseEditViewAdapter.Item.ITEM_HEAD));
        for (Message.Praise.Item.Builder praiseItem : editPraise.getItemBuilderList()) {
            if (praiseItem.getIsDelete()) {
                continue;
            }
            mViewItemList.add(mEditPraiseViewAdapter.new Item(PraiseEditViewAdapter.Item.ITEM_ITEM, praiseItem));
        }
        mEditPraiseViewAdapter.setItemEditable(mViewItemList.size() <= 6);
        mEditPraiseViewAdapter.notifyDataSetChanged();
    }

    private void updateViewForPraiseItem() {
        for (ListIterator<PraiseEditViewAdapter.Item> it = mViewItemList.listIterator(); it.hasNext(); ) {
            PraiseEditViewAdapter.Item ViewItem = it.next();
            if (ViewItem.type == PraiseEditViewAdapter.Item.ITEM_ITEM) {
                it.remove();
            }
        }
        for (ListIterator<PraiseEditViewAdapter.Item> it = mViewItemList.listIterator(); it.hasNext(); ) {
            PraiseEditViewAdapter.Item ViewItem = it.next();
            if (ViewItem.type == PraiseEditViewAdapter.Item.ITEM_HEAD) {
                for (Message.Praise.Item.Builder praiseItem : editPraise.getItemBuilderList()) {
                    if (praiseItem.getIsDelete()) {
                        continue;
                    }
                    it.add(mEditPraiseViewAdapter.new Item(PraiseEditViewAdapter.Item.ITEM_ITEM, praiseItem));
                }
                break;
            }
        }
        mEditPraiseViewAdapter.setItemEditable(mViewItemList.size() <= 6);
        mEditPraiseViewAdapter.notifyDataSetChanged();
    }

    private void showAddPraiseTask() {
        int n = 0;
        for (Message.Praise.Item.Builder praiseItem : editPraise.getItemBuilderList()) {
            if (praiseItem.getIsDelete()) {
                continue;
            }
            ++n;
        }
        if (n >= 6) {
            toast(R.string.collect_Max_6_task);
            return;
        }

        final View view = getLayoutInflater().inflate(R.layout.view_input_praise_item_name, null);
        android.app.Dialog dialog = createDialog(this, view);
        dialog.show();
        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(R.string.collect_task_name);
        final EditText nameEditor = view.findViewById(R.id.task_name);
        showSoftKeyboard(nameEditor);
        nameEditor.setFilters(new InputFilter[]{emojiFilter});
        nameEditor.addTextChangedListener(new TextWatcher() {
            private final int maxLength = getResources().getInteger(R.integer.maxLength_of_item_name_of_collect_praise);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int editStart = nameEditor.getSelectionStart();
                int editEnd = nameEditor.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                nameEditor.removeTextChangedListener(this);

                // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
                // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
                while (Utils.charSequenceLength_zhCN(s) > maxLength) { // 当输入字符个数超过限制的大小时，进行截断操作
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                nameEditor.setSelection(editStart);

                // 恢复监听器
                nameEditor.addTextChangedListener(this);
            }
        });

        view.findViewById(R.id.ok).setOnClickListener(v -> {
            String name = nameEditor.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                toast(R.string.collect_task_name_not_null);
                return;
            }
            Message.Praise.Item.Builder praiseItemAdd = null;
            for (Message.Praise.Item.Builder praiseItem : editPraise.getItemBuilderList()) {
                if (praiseItem.getName().equals(name)) {
                    praiseItemAdd = praiseItem;
                    if (!praiseItem.getIsDelete()) {
                        toast(R.string.collect_task_same);
                        return;
                    } else {
                        praiseItem.setIsDelete(false);
                    }
                    break;
                }
            }
            if (praiseItemAdd == null) {
                praiseItemAdd = Message.Praise.Item.newBuilder().setName(name);
                editPraise.addItem(praiseItemAdd);
            }
            updateViewForPraiseItem();
            if (originPraise != null) {
                modifyPraise();
            }
            dialog.dismiss();
        });
        view.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());


    }

    private void showEditTask(final PraiseEditViewAdapter.Item viewItem) {
        final View view = getLayoutInflater().inflate(R.layout.view_input_praise_item_name, null);
        android.app.Dialog dialog = createDialog(this, view);
        dialog.show();
        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(R.string.collect_task_name);
        final EditText nameEditor = view.findViewById(R.id.task_name);
        final Message.Praise.Item.Builder praiseItem = (Message.Praise.Item.Builder) viewItem.dat;
        nameEditor.setText(praiseItem.getName());
        nameEditor.setSelection(nameEditor.length());
        nameEditor.setFilters(new InputFilter[]{emojiFilter});
        nameEditor.addTextChangedListener(new TextWatcher() {
            private final int maxLength = getResources().getInteger(R.integer.maxLength_of_item_name_of_collect_praise);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int editStart = nameEditor.getSelectionStart();
                int editEnd = nameEditor.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                nameEditor.removeTextChangedListener(this);

                // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
                // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
                while (Utils.charSequenceLength_zhCN(s) > maxLength) { // 当输入字符个数超过限制的大小时，进行截断操作
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                nameEditor.setSelection(editStart);

                // 恢复监听器
                nameEditor.addTextChangedListener(this);
            }
        });
        dialog.findViewById(R.id.ok).setOnClickListener(v -> {
            String name = nameEditor.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                toast(R.string.collect_task_name_not_null);
                return;
            }
            for (Message.Praise.Item.Builder praiseItem1 : editPraise.getItemBuilderList()) {
                if (praiseItem1.getName().equals(name)) {
                    toast(R.string.collect_task_same);
                    return;
                }
            }
            praiseItem.setName(name);
            if (originPraise != null) {
                modifyPraise();
            } else {
                mEditPraiseViewAdapter.notifyDataSetChanged();
            }
            dialog.dismiss();
        });

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());

    }

    private void addPraise(Message.Praise praise) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "addPraise: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.submitting);

        exec(Message.AddPraiseReqMsg.newBuilder().setDeviceId(deviceId).setPraise(praise).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case DATA_CONFLICT: {
                                    GreenUtils.savePraise(deviceId, rspMsg.getPraise());
                                    dismissDialog();
                                    Intent intent = new Intent(TAG);
                                    intent.putExtra(CollectPraiseActivity.PARAM_PRAISE, rspMsg.getPraise());
                                    setResult(rspMsg.getErrCode() == Message.ErrorCode.SUCCESS ? RESULT_OK : RESULT_FIRST_USER, intent);
                                    finish();
                                }
                                return false;

                                default:
                                    break;
                            }
                            L.w(TAG, "addPraise() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "addPraise() -> exec() -> onResponse()", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "addPraise() -> exec() -> onException()", cause);
                        popErrorDialog(R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

    private void modifyPraise() {
        if (editPraise.getItemCount() == 0) {
            toast(R.string.collect_create_task_one);
            return;
        }

        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "addPraise: deviceId is empty");
            return;
        }
        popWaitingDialog(R.string.submitting);

        Message.Praise praise = editPraise.build();

        exec(Message.ModifyPraiseReqMsg.newBuilder().setDeviceId(deviceId).setPraise(praise).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case DATA_CONFLICT: {
                                    dismissDialog();
                                    GreenUtils.savePraise(deviceId, rspMsg.getPraise());
//                                    Intent intent = new Intent(TAG);
//                                    intent.putExtra(CollectPraiseActivity.PARAM_PRAISE, rspMsg.getPraise());
//                                    setResult(rspMsg.getErrCode() == Message.ErrorCode.SUCCESS ? RESULT_OK : RESULT_FIRST_USER, intent);
//                                    finish();
                                    updateView();
                                }
                                return false;

                                default:
                                    editPraise = originPraise.toBuilder();
                                    updateView();
                                    break;
                            }
                            L.w(TAG, "modifyPraise() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "modifyPraise() -> exec() -> onResponse()", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        editPraise = originPraise.toBuilder();
                        updateView();
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "modifyPraise() -> exec() -> onException()", cause);
                        popErrorDialog(R.string.submit_failure);
                        editPraise = originPraise.toBuilder();
                        updateView();
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

    private void cancelPraise() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "cancelPraise: deviceId is empty");
            return;
        }
        final Message.Praise praise = originPraise;
        if (praise == null) {
            L.w(TAG, "cancelPraise: praise is null");
            return;
        }
        if (TextUtils.isEmpty(praise.getId())) {
            L.w(TAG, "cancelPraise: praise.id is empty");
            return;
        }
        popWaitingDialog(R.string.submitting);
        exec(Message.CancelPraiseReqMsg.newBuilder().setDeviceId(deviceId)
                        .setPraiseId(praise.getId()).build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.CancelPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case DATA_CONFLICT: {
                                    GreenUtils.savePraise(deviceId, rspMsg.getPraise());
                                    Intent intent = new Intent(CollectPraiseActivity.class.getSimpleName());
                                    setResult(rspMsg.getErrCode() == Message.ErrorCode.SUCCESS ? RESULT_OK : RESULT_FIRST_USER, intent);
                                    dismissDialog();
                                    finish();
                                }
                                return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "cancelPraise() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "cancelPraise() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "cancelPraise() -> exec() -> onException()", cause);
                        popErrorDialog(R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }


    private void addPraiseNumberDialog() {
        if (editPraise.getItemCount() == 0) {
            toast(R.string.collect_create_task_one);
            return;
        }
        if (TextUtils.isEmpty(editPraise.getPrize())) {
            toast(R.string.collect_gift_name_not_null);
            return;
        }

        final View view = getLayoutInflater().inflate(R.layout.view_input_praise_total_goal, null);
        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(R.string.collect_target_collects);
        EditText editText = view.findViewById(R.id.number);
        showSoftKeyboard(editText);
        editText.setText(editPraise.getTotalGoal() == 0 ? "" : String.valueOf(editPraise.getTotalGoal()));
        editText.setSelection(editText.length());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0)
                    return;

                int editStart = editText.getSelectionStart();
                int editEnd = editText.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                editText.removeTextChangedListener(this);

                while (true) {
                    String num = s.toString();
                    try {
                        Integer.parseInt(num);
                        break;
                    } catch (NumberFormatException ignored) {
                    }
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                editText.setSelection(editStart);

                // 恢复监听器
                editText.addTextChangedListener(this);
            }
        });
        android.app.Dialog Dialog = createDialog(this, view);
        Dialog.show();

        view.findViewById(R.id.ok).setOnClickListener(v -> {
            String number = editText.getText().toString().trim();
            if (TextUtils.isEmpty(number)) {
                toast(R.string.collect_input_target_collects);
                return;
            }
            int goal;
            try {
                goal = Integer.parseInt(number);
            } catch (NumberFormatException e) {
                if (TextUtils.isDigitsOnly(number)) {
                    toast(R.string.collect_input_large);
                } else {
                    toast(R.string.collect_input_numbers);
                }
                return;
            }
            if (goal <= 0) {
                toast(R.string.collect_input_target_collects);
                return;
            }
//                        if (goal < editPraise.getItemCount()) {
//                            toast("目标赞数不能小于任务数");
//                            return;
//                        }
            editPraise.setTotalGoal(goal);
            if (TextUtils.isEmpty(editPraise.getTimezone().getZone())) {
                editPraise.setTimezone(Message.Timezone.newBuilder().setZone(TimeZone.getDefault().getID()));
            }
            addPraise(editPraise.build());
            Dialog.dismiss();
        });
        view.findViewById(R.id.cancel).setOnClickListener(v -> Dialog.dismiss());
    }

    private void showPraiseName() {
        final View view = getLayoutInflater().inflate(R.layout.collect_parise_name, null);
        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(R.string.collect_create_gift);

        EditText editText = view.findViewById(R.id.name);
        showSoftKeyboard(editText);
        editText.setFilters(new InputFilter[]{emojiFilter});
        editText.addTextChangedListener(new TextWatcher() {
            private final int maxLength = getResources().getInteger(R.integer.maxLength_of_prize_name_of_collect_praise);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int editStart = editText.getSelectionStart();
                int editEnd = editText.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                editText.removeTextChangedListener(this);

                // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
                // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
                while (Utils.charSequenceLength_zhCN(s) > maxLength) { // 当输入字符个数超过限制的大小时，进行截断操作
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                editText.setSelection(editStart);

                // 恢复监听器
                editText.addTextChangedListener(this);
            }
        });
        Dialog dialog = createDialog(this, view);
        view.findViewById(R.id.ok).setOnClickListener(v -> {
            String praiseName = editText.getText().toString().trim();
            editPraise.setPrize(praiseName);
            dialog.dismiss();
            updateView();
        });
        view.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
