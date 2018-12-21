package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.SelectRelationActivity;
import com.cqkct.FunKidII.Ui.BlurActivity.BaseBlurActivity;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.fragment.ContactsEditDialogFragment;
import com.cqkct.FunKidII.Ui.view.PullBackLayout;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.Utils.UserPermission;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.RelationAvatar;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.gyf.barlibrary.ImmersionBar;

import java.util.concurrent.TimeoutException;

import protocol.Message;

public class ContactDetailActivity extends BaseBlurActivity {
    private static final String TAG = ContactDetailActivity.class.getSimpleName();

    public static final String PARAM_KEY_CONTACT = "contact";

    public static final String RETURN_ACTION = ContactDetailActivity.class.getName();
    public static final String RETURN_KEY_CONTACT = "contact";
    public static final int RESULT_DELETED = RESULT_FIRST_USER + 1;
    public static final int RESULT_MODIFIED = RESULT_FIRST_USER + 2;


    private static final int ACTIVITY_REQUEST_CODE_SELECT_RELATION = 0;

    private ContactEntity contactEntity;
    private boolean hasEditPermission = false;

    private ImageView avatarView;
    private TextView nameView;
    private TextView numberView;
    private TextView typeView;
    private TextView permissionView;
    private Button actionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        Intent intent = getIntent();
        contactEntity = (ContactEntity) intent.getSerializableExtra(PARAM_KEY_CONTACT);
        if (contactEntity == null) {
            L.e(TAG, "onCreate() invalid ACTIVITY PARAM contactEntity: is null");
            finish();
            return;
        }

        if (contactEntity.getType() == ContactEntity.TYPE_NORMAL || contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
            hasEditPermission = hasEditPermission();
        } else if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
            int curUserPermission = mCurrentBabyBean == null ? 0 : mCurrentBabyBean.getPermission();
            int otherUserPermission = contactEntity.getPermission();
            hasEditPermission = UserPermission.hasEditPermission(curUserPermission) && curUserPermission <= otherUserPermission;
        }

        initView();
    }

    @Override
    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.init();
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.name_layout:
                if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
                    changeRelation();
                } else if (contactEntity.getType() == ContactEntity.TYPE_NORMAL) {
                    changeName();
                } else if (contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
                    changeFriendNickname();
                }
                break;

            case R.id.number_layout:
                changeNumber();
                break;

            case R.id.action_btn:
                if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
                    unbindDevice();
                } else if (contactEntity.getType() == ContactEntity.TYPE_NORMAL) {
                    deleteContact(contactEntity);
                } else if (contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
                    deleteFriend(contactEntity);
                }
                break;

            case R.id.slide_down_icon:
                finish();
                overridePendingTransition(0, R.anim.out_to_bottom);
                break;
        }
    }

    private void initView() {
        ((PullBackLayout) findViewById(R.id.pull_back_layout)).setCallback(new PullBackLayout.Callback() {
            @Override public void onPullStart() { }
            @Override public void onPullDown(float progress) { }
            @Override public void onPullUp() { }
            @Override public void onPullCancel() { }
            @Override public void onPullComplete() {
                finish();
                overridePendingTransition(0, R.anim.out_to_bottom);
            }
        });

        avatarView = findViewById(R.id.avatar);
        nameView = findViewById(R.id.name);
        numberView = findViewById(R.id.number);
        typeView = findViewById(R.id.type);
        permissionView = findViewById(R.id.permission);
        actionBtn = findViewById(R.id.action_btn);
        actionBtn.setVisibility(View.GONE);

        refreshView();
    }

    private void refreshView() {
        if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
            // 绑定用户的联系人

            Glide.with(avatarView)
                    .load(RelationUtils.isCustomRelation(contactEntity.getRelation())
                            ? new RelationAvatar(contactEntity.getDeviceId(), contactEntity.getUserId(), contactEntity.getUserAvatar())
                            : new RelationAvatar(contactEntity.getDeviceId(), contactEntity.getUserId(), RelationUtils.getIconResId(contactEntity.getRelation())))
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(R.drawable.head_relation))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.head_relation))
                    .into(avatarView);

            nameView.setText(RelationUtils.decodeRelation(this, contactEntity.getRelation()));
            if (hasEditPermission) {
                findViewById(R.id.name_detail).setVisibility(View.VISIBLE);
                findViewById(R.id.name_layout).setOnClickListener(getDebouncedOnClickListener());
            }

            numberView.setText(contactEntity.getNumber());

            typeView.setText(UserPermission.toRoleString(this, contactEntity.getPermission()));

            permissionView.setText(UserPermission.toPermissionString(this, contactEntity.getPermission()));

            boolean permission = hasEditPermission;
            if (contactEntity.getUserId().equals(mUserId)) {
                permission = true;
            }
            if (permission) {
                actionBtn.setText(R.string.unbind);
                actionBtn.setVisibility(View.VISIBLE);
                actionBtn.setOnClickListener(getDebouncedOnClickListener());
            }
        } else if (contactEntity.getType() == ContactEntity.TYPE_NORMAL) {
            // 普通联系人

            avatarView.setImageResource(R.drawable.hard_contacts);

            nameView.setText(contactEntity.getName());
            if (hasEditPermission) {
                findViewById(R.id.name_detail).setVisibility(View.VISIBLE);
                findViewById(R.id.name_layout).setOnClickListener(getDebouncedOnClickListener());
            }

            numberView.setText(contactEntity.getNumber());

            if (hasEditPermission) {
                findViewById(R.id.number_detail).setVisibility(View.VISIBLE);
                findViewById(R.id.number_layout).setOnClickListener(getDebouncedOnClickListener());
            }

            typeView.setText(UserPermission.toRoleString(this, Message.UsrDevAssoc.Permission.MINI_VALUE));

            permissionView.setText(UserPermission.toPermissionString(this, Message.UsrDevAssoc.Permission.MINI_VALUE));

            if (hasEditPermission) {
                actionBtn.setText(R.string.delete_contact);
                actionBtn.setVisibility(View.VISIBLE);
                actionBtn.setOnClickListener(getDebouncedOnClickListener());
            }
        } else if (contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
            // 手表好友好友

            Glide.with(avatarView)
                    .load(new DeviceAvatar(contactEntity.getFriendDeviceId(), contactEntity.getFriendBabyAvatar(), R.drawable.ic_head_relation_friend))
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(R.drawable.ic_head_relation_friend))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.ic_head_relation_friend))
                    .into(avatarView);

            String name = friendDisplayName(contactEntity.getName(), contactEntity.getFriendNickname());
            nameView.setText(name);
            if (hasEditPermission) {
                findViewById(R.id.name_detail).setVisibility(View.VISIBLE);
                findViewById(R.id.name_layout).setOnClickListener(getDebouncedOnClickListener());
            }

            if (TextUtils.isEmpty(contactEntity.getNumber())) {
                numberView.setText(R.string.on_phone_number);
            } else {
                numberView.setText(contactEntity.getNumber());
            }

            typeView.setText(R.string.user_roles_friend);

            findViewById(R.id.permission_layout_sep).setVisibility(View.GONE);
            findViewById(R.id.permission_layout).setVisibility(View.GONE);

            if (hasEditPermission) {
                actionBtn.setText(R.string.delete_friend);
                actionBtn.setVisibility(View.VISIBLE);
                actionBtn.setOnClickListener(getDebouncedOnClickListener());
            }
        }
    }

    private boolean checkDataChange(Message.ContactOrBuilder contact) {
        boolean changed = false;
        if (contactEntity.getType() == ContactEntity.TYPE_NORMAL) {
            if (!contact.getNumber().equals(contactEntity.getNumber())
                    || !contact.getName().equals(contactEntity.getName())) {
                changed = true;
            }
        } else if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
            if (!contact.getUsrDevAssoc().getRelation().equals(contactEntity.getRelation())
                    || contact.getUsrDevAssoc().getPermissionValue() != contactEntity.getPermission()
                    || !contact.getUsrDevAssoc().getAvatar().equals(contactEntity.getUserAvatar())) {
                changed = true;
            }
        } else if (contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
            if (!contact.getFriendNickname().equals(contactEntity.getFriendNickname())
                    || !contact.getFamilyShortNum().equals(contactEntity.getFamilyShortNum())) {
                changed = true;
            }
        }

        return changed;
    }

    private void deleteContact(ContactEntity contact) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteContact: deviceId (" + deviceId + ") is empty");
            popErrorDialog(R.string.delete_failure);
            return;
        }
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setMessage(getString(R.string.contact_delete_user_sure))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_deleting);
                    doDeleteContact(contact, deviceId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "DeleteContactDialog");
    }

    private void doDeleteContact(ContactEntity contact, String deviceId) {
        Message.DelContactReqMsg reqMsg = Message.DelContactReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addContactId(contact.getContactId())
                .build();

        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelContactRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case NOT_EXISTS:
                                case SUCCESS: {
                                    L.d(TAG, "doDeleteContact() -> exec() -> onResponse(): " + rspMsg.getErrCode());

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contact);
                                    setResult(RESULT_DELETED, intent);

                                    GreenUtils.deleteContact(contact);
                                    popSuccessDialog(R.string.delete_success, true);
                                }
                                return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "doDeleteContact() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doDeleteContact() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doDeleteContact() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.delete_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void unbindDevice() {
        ContactEntity contact = contactEntity;
        if (contact == null) {
            L.e(TAG, "unbindDevice: contactEntity is null");
            return;
        }

        String deviceId = mDeviceId;
        String userId = mUserId;
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId)) {
            L.w(TAG, "unbindDevice: userId: (" + userId + ") is empty or deviceId (" + deviceId + ") is empty");
            popErrorDialog(R.string.unbind_and_bind_unbind_failure);
            return;
        }
        if (TextUtils.isEmpty(contact.getUserId())) {
            popErrorDialog(R.string.unbind_and_bind_unbind_failure);
            L.e(TAG, "unbindDevice: the contact's userId is empty");
            return;
        }

        boolean owner = contact.getUserId().equals(userId) && contact.getPermission() == Message.UsrDevAssoc.Permission.OWNER_VALUE;

        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setMessage(getString(owner ? R.string.unbind_owner_device_tip : R.string.contact_unbind_user_sure))
                .setPositiveButton(getString(R.string.unbind_short), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_unbinding_device);
                    doUnbindDevice(contact, deviceId, userId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "UnbindDeviceDialog");
    }

    private void doUnbindDevice(ContactEntity contact, String deviceId, String userId) {
        Message.UnbindDevReqMsg reqMsg = Message.UnbindDevReqMsg.newBuilder()
                .setUsrDevAssoc(contact.getUsrDevAssoc())
                .build();
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.UnbindDevRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case NOT_EXISTS:
                                case SUCCESS: {
                                    L.i(TAG, "doUnbindDevice: " + contact.getDeviceId() + ": " + rspMsg.getErrCode());

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contact);
                                    setResult(RESULT_DELETED, intent);

                                    popSuccessDialog(R.string.unbind_and_bind_unbind_suc, hud -> {
                                        if (contactEntity.getUserId().equals(userId)) {
                                            // 解绑的是自己绑定的设备，
                                            L.i(TAG, "the baby " + contact.getDeviceId() + " unbind");
                                            GreenUtils.clearDeviceWhenUnbind(contact.getDeviceId(), contact.getUserId(), rspMsg.getClearLevel());
                                        } else {
                                            GreenUtils.deleteContact(contactEntity);
                                        }
                                        finish();
                                    });
                                }
                                return true;
                                default:
                                    break;
                            }
                            L.w(TAG, "doUnbindDevice() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doUnbindDevice() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.unbind_and_bind_unbind_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doUnbindDevice() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.unbind_and_bind_unbind_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void changeName() {
        ContactsEditDialogFragment dialogFragment = new ContactsEditDialogFragment();
        dialogFragment.setTitle(getString(R.string.contact_edit_user_name))
                .setNameText(nameView.getText().toString())
                .hideNumberEditor()
                .setNegativeButton(getString(R.string.cancel))
                .setPositiveButton(getString(R.string.ok), (dialog, name, number) -> {
                    if (TextUtils.isEmpty(name)) {
                        toast(R.string.contacts_name_not_null);
                        return;
                    }
                    Message.Contact.Builder builder = contactEntity.toContact().toBuilder();
                    builder.setName(name);
                    Message.Contact contact = builder.build();
                    if (checkDataChange(contact)) {
                        saveChanges(contact);
                    }
                    dialog.dismiss();
                });
        dialogFragment.show(getSupportFragmentManager(), "changeContactNameDialog");
    }

    private void changeRelation() {
        Intent intent = new Intent(this, SelectRelationActivity.class);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_MODE, SelectRelationActivity.PARAM_MODE_SELECT_RELATION);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_RELATION, contactEntity.getRelation());
        intent.putExtra(SelectRelationActivity.PARAM_KEY_AVATAR, contactEntity.getUserAvatar());
        intent.putExtra(SelectRelationActivity.PARAM_KEY_BIND_NUM, contactEntity.getDeviceId());
        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SELECT_RELATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_SELECT_RELATION && resultCode == RESULT_OK) {
            String newRelation = data.getStringExtra(SelectRelationActivity.RESULT_KEY_NEW_RELATION);
            String avatar = data.getStringExtra(SelectRelationActivity.RESULT_KEY_USER_AVATAR);
            if (newRelation == null)
                newRelation = "";
            Message.Contact.Builder contact = contactEntity.toContact().toBuilder();
            contact.getUsrDevAssocBuilder().setRelation(newRelation);
            if (avatar != null) {
                contact.getUsrDevAssocBuilder().setAvatar(avatar);
            }
            if (checkDataChange(contact)) {
                saveChanges(contact.build());
            }
        }
    }

    private void changeNumber() {
        ContactsEditDialogFragment dialogFragment = new ContactsEditDialogFragment();
        dialogFragment.setTitle(getString(R.string.contact_edit_user_number))
                .hideNameEditor()
                .setNumberText(numberView.getText().toString())
                .setNegativeButton(getString(R.string.cancel))
                .setPositiveButton(getString(R.string.ok), (dialog, name, number) -> {
                    if (TextUtils.isEmpty(number)) {
                        toast(R.string.contacts_number_not_null);
                        return;
                    }
                    Message.Contact.Builder builder = contactEntity.toContact().toBuilder();
                    builder.setNumber(number);
                    Message.Contact contact = builder.build();
                    if (checkDataChange(contact)) {
                        saveChanges(contact);
                    }
                    dialog.dismiss();
                });
        dialogFragment.show(getSupportFragmentManager(), "changeContactNumberDialog");
    }

    private void saveChanges(Message.Contact contact) {
        if (contactEntity.getType() == ContactEntity.TYPE_NORMAL) {
            modifyNormalContact(contact);
        } else if (contactEntity.getType() == ContactEntity.TYPE_ASSOC) {
            modifyUsrDevAssoc(contact);
        } else if (contactEntity.getType() == ContactEntity.TYPE_FRIEND) {
            modifyFriendContact(contact);
        }
    }

    private void modifyNormalContact(final Message.Contact contact) {
        String userId = mUserId;
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            popErrorDialog(R.string.submit_temporarily_unable);
            return;
        }

        popWaitingDialog(R.string.submitting);

        exec(
                Message.ModifyContactReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .addContact(contact)
                        .build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyContactRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "modifyNormalContact() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    L.d(TAG, "modifyUsrDevAssoc() -> exec() -> onResponse() SUCCESS");

                                    contactEntity = GreenUtils.addOrModifyContact(deviceId, contact);

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contactEntity);
                                    setResult(RESULT_MODIFIED, intent);

                                    popSuccessDialog(R.string.submit_success, hud -> refreshView());
                                }
                                    return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_contacts_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "modifyNormalContact() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "modifyNormalContact() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "modifyNormalContact() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void modifyUsrDevAssoc(final Message.Contact contact) {
        final String userId = mUserId;
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            popErrorDialog(R.string.submit_temporarily_unable);
            return;
        }

        popWaitingDialog(R.string.submitting);

        exec(
                Message.ModifyUsrDevAssocReqMsg.newBuilder()
                        .setUsrDevAssoc(contact.getUsrDevAssoc())
                        .build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyUsrDevAssocRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    L.d(TAG, "modifyUsrDevAssoc() -> exec() -> onResponse() SUCCESS");

                                    if (contact.getUsrDevAssoc().getUserId().equals(userId)) {
                                        GreenUtils.saveUsrDevAssoc(contact.getUsrDevAssoc());
                                    }

                                    contactEntity = GreenUtils.addOrModifyContact(deviceId, contact);

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contactEntity);
                                    setResult(RESULT_MODIFIED, intent);

                                    popSuccessDialog(R.string.submit_success, hud -> refreshView());
                                }
                                return true;
                                default:
                                    break;
                            }
                            L.d(TAG, "modifyUsrDevAssoc() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.d(TAG, "modifyUsrDevAssoc() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "modifyUsrDevAssoc() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void changeFriendNickname() {
        ContactsEditDialogFragment dialogFragment = new ContactsEditDialogFragment();
        dialogFragment.setTitle(getString(R.string.contact_edit_user_name))
                .setNameText(contactEntity.getFriendNickname())
                .hideNumberEditor()
                .setNegativeButton(getString(R.string.cancel))
                .setPositiveButton(getString(R.string.ok), (dialog, name, number) -> {
                    if (TextUtils.isEmpty(name)) {
                        toast(R.string.contacts_name_not_null);
                        return;
                    }
                    Message.Contact.Builder builder = contactEntity.toContact().toBuilder();
                    builder.setFriendNickname(name);
                    Message.Contact contact = builder.build();
                    if (checkDataChange(contact)) {
                        saveChanges(contact);
                    }
                    dialog.dismiss();
                });
        dialogFragment.show(getSupportFragmentManager(), "changeContactFriendNicknameDialog");
    }

    private String friendDisplayName(String babyName, String nickname) {
        if (TextUtils.isEmpty(babyName)) {
            if (TextUtils.isEmpty(nickname)) {
                return getString(R.string.friend_of_baby);
            } else {
                return nickname;
            }
        } else if (TextUtils.isEmpty(nickname)) {
            return babyName;
        } else {
            return getString(R.string.friend_display_name_combination, nickname, babyName);
        }
    }

    private void modifyFriendContact(final Message.Contact contact) {
        String userId = mUserId;
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            popErrorDialog(R.string.submit_temporarily_unable);
            return;
        }

        popWaitingDialog(R.string.submitting);

        exec(
                Message.ModifyFriendNicknameReqMsg.newBuilder()
                        .setDeviceID(deviceId)
                        .setFriendId(contact.getFriendId())
                        .setNickname(contact.getFriendNickname())
                        .build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyFriendNicknameRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "modifyFriendContact() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    L.d(TAG, "modifyFriendContact() -> exec() -> onResponse() SUCCESS");

                                    contactEntity = GreenUtils.addOrModifyContact(deviceId, contact);

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contactEntity);
                                    setResult(RESULT_OK, intent);

                                    popSuccessDialog(R.string.submit_success, hud -> refreshView());
                                }
                                return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_contacts_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "modifyFriendContact() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "modifyFriendContact() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "modifyFriendContact() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void deleteFriend(ContactEntity contact) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteFriend: deviceId (" + deviceId + ") is empty");
            popErrorDialog(R.string.delete_failure);
            return;
        }
        if (TextUtils.isEmpty(contact.getFriendId())) {
            L.e(TAG, "deleteFriend: the contact's friend id is empty");
            return;
        }

        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setMessage(getString(R.string.ask_sure_delete_the_friend))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_deleting);
                    doDelFriend(contact, deviceId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "DeleteFriendDialog");
    }

    private void doDelFriend(ContactEntity contact, String deviceId) {
        Message.DelFriendReqMsg reqMsg = Message.DelFriendReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .setFriendId(Message.DelFriendReqMsg.FriendIdList.newBuilder().addFriendId(contact.getFriendId()))
                .build();

        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelFriendRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case NOT_EXISTS:
                                case SUCCESS: {
                                    L.d(TAG, "doDelFriend() -> exec() -> onResponse(): " + rspMsg.getErrCode());

                                    Intent intent = new Intent(RETURN_ACTION);
                                    intent.putExtra(RETURN_KEY_CONTACT, contact);
                                    setResult(RESULT_DELETED, intent);

                                    GreenUtils.deleteContact(contact);
                                    popSuccessDialog(R.string.delete_success, true);
                                }
                                return false;

                                default:
                                    break;
                            }
                            L.w(TAG, "delFriend() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "delFriend() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "delFriend() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.delete_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }
}
