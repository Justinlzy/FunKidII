package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.ContactsAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.fragment.ContactsAddDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PhoneNumberUtils;
import com.cqkct.FunKidII.Utils.UserPermission;
import com.cqkct.FunKidII.db.Dao.ContactEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/11/6.
 */

public class ContactsActivity extends BaseActivity {
    private static final String TAG = ContactsActivity.class.getSimpleName();

    public static final int ACTIVITY_FOR_RESULT_DETAIL = 1;
    public static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_CONTACTS_PERMISSION = 2;

    public static final int PERMISSIONS_REQUEST_CONTACTS = 1;

    //******************************* 分割标题 *****************************************************/
    // 创建 3 个空的 ContactEntity，用来显示 3 个分割标题
    public final ContactEntity ASSOC_SEPARATOR_DATA = new ContactEntity(Long.MIN_VALUE | (Long.MIN_VALUE >>> 1) | ContactEntity.TYPE_ASSOC);
    public final ContactEntity NORMAL_SEPARATOR_DATA = new ContactEntity(Long.MIN_VALUE | ContactEntity.TYPE_NORMAL);
    public final ContactEntity FRIENDS_SEPARATOR_DATA = new ContactEntity(Long.MIN_VALUE | ContactEntity.TYPE_FRIEND);

    // ID 取值：
    // 0x8000000000000000 最高位标记是否是分割标题
    // 0x7000000000000000 次高位表示是否展开
    // 0x3FFFFFFFFFFFFFFF 分割类型 MASK，即剩余数据表示类型
    public static boolean isSeparatorData(ContactEntity data) {
        if (data == null)
            return false;
        Long id = data.getId();
        if (id == null)
            return false;
        return (id & Long.MIN_VALUE) != 0;
    }

    public static boolean isExpanded(ContactEntity data) {
        if (data == null)
            return true;
        Long id = data.getId();
        if (id == null)
            return true;
        return (id & (Long.MIN_VALUE >>> 1)) != 0;
    }

    public static void setExpand(ContactEntity data, boolean expand) {
        if (isSeparatorData(data)) {
            long id = data.getId();
            if (expand) {
                id |= Long.MIN_VALUE >>> 1;
            } else {
                id &= ~(Long.MIN_VALUE >>> 1);
            }
            data.setId(id);
        }
    }

    public static int getSeparatorCategory(ContactEntity data) {
        if (isSeparatorData(data)) {
            return (int) (data.getId() & ~(Long.MIN_VALUE | (Long.MIN_VALUE >>> 1)));
        }
        return ContactEntity.TYPE_NONE;
    }

    //******************************* 分割标题 *****************************************************/
    // list 数据
    private final List<ContactEntity> mData = new ArrayList<>();
    // 存储不同类型的联系人数据
    private final List<ContactEntity> mAssocContacts = new ArrayList<>();
    private final List<ContactEntity> mNormalContacts = new ArrayList<>();
    private final List<ContactEntity> mFriendsContacts = new ArrayList<>();

    private int curUserPermission = 0;
    private boolean hasEditPermission = false;
    private int limit = -1;

    private SwipeMenuRecyclerView mListView;
    private LinearLayout mLoading;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
//        setTitleBarTitle(R.string.contact);

        if (TextUtils.isEmpty(mDeviceId)) {
            L.e(TAG, "onCreate mDeviceId isEmpty");
            this.finish();
            return;
        }

        curUserPermission = mCurrentBabyBean == null ? 0 : mCurrentBabyBean.getPermission();
        hasEditPermission = UserPermission.hasEditPermission(curUserPermission);

        hasEditPermission = hasEditPermission();

        mData.add(ASSOC_SEPARATOR_DATA);
        mData.add(NORMAL_SEPARATOR_DATA);
        mData.add(FRIENDS_SEPARATOR_DATA);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (limit < 0) {
            limit = 0;
            mListView.postDelayed(this::loadData, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_FOR_RESULT_DETAIL) {
            Serializable contactSerializable = data == null ? null : data.getSerializableExtra(ContactDetailActivity.RETURN_KEY_CONTACT);
            ContactEntity returnContact = contactSerializable == null ? null : (ContactEntity) contactSerializable;
            if (returnContact != null) {
                switch (resultCode) {
                    case ContactDetailActivity.RESULT_DELETED:
                        listViewRemoveItem(returnContact.getType(), returnContact.getId());
                        break;
                    case ContactDetailActivity.RESULT_MODIFIED:
                        listViewChangeItem(returnContact);
                        break;
                }
            }
        } else if (requestCode == ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_CONTACTS_PERMISSION) {

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CONTACTS:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_contacts_permission);
                }
                break;
            default:
                break;
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
            case R.id.title_bar_right_icon:
                addContact();
                break;
        }
    }

    private void initView() {
        tvTitle = findViewById(R.id.title_text);
        tvTitle.setText(R.string.contact);
        mLoading = findViewById(R.id.ll_loading);

        mListView = findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setSwipeItemClickListener((itemView, position) -> {
            ContactEntity data = mData.get(position);
            if (isSeparatorData(data)) {
                setExpand(data, !isExpanded(data));
                foldData(position, data);
            } else {
                Intent intent = new Intent(ContactsActivity.this, ContactDetailActivity.class);
                intent.putExtra(ContactDetailActivity.PARAM_KEY_CONTACT, data);
                startActivityForResult(intent, ACTIVITY_FOR_RESULT_DETAIL);
            }
        });
        mListView.setSwipeMenuCreator((swipeLeftMenu, swipeRightMenu, viewType) -> {
            if (viewType == ContactsAdapter.SWIPE_MENU_NONE)
                return;

            int width = getResources().getDimensionPixelSize(R.dimen.dp_70);
            // 1. MATCH_PARENT 自适应高度，保持和Item一样高;
            // 2. 指定具体的高，比如80;
            // 3. WRAP_CONTENT，自身高度，不推荐;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            // 添加右侧的，如果不添加，则右侧不会出现菜单。
            SwipeMenuItem deleteItem = new SwipeMenuItem(this)
                    .setImage(R.drawable.delete_left_slip)
                    .setWidth(width)
                    .setHeight(height);
            swipeRightMenu.addMenuItem(deleteItem);// 添加菜单到右侧。
        });
        mListView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            int adapterPosition = menuBridge.getAdapterPosition();
            ContactEntity data = mData.get(adapterPosition);
            switch (mListView.getAdapter().getItemViewType(adapterPosition)) {
                case ContactsAdapter.SWIPE_MENU_ASSOC:
                    ContactsActivity.this.unbindDevice(data);
                    break;
                case ContactsAdapter.SWIPE_MENU_NORMAL:
                    ContactsActivity.this.deleteContact(data);
                    break;
                case ContactsAdapter.SWIPE_MENU_FRIEND:
                    ContactsActivity.this.deleteFriend(data);
                    break;
                default:
                    break;
            }
        });
        mListView.setAdapter(new ContactsAdapter(this, mData, mUserId, curUserPermission, hasEditPermission));

        if (hasEditPermission) {
            ImageView addBtn = findViewById(R.id.title_bar_right_icon);
            addBtn.setVisibility(View.VISIBLE);
        }
    }

    private void loadData() {
        limit = loadLimit(mDeviceId);
        loadDataFromDb();
        getContactsFromService(true);
    }

    private int loadLimit(String mDeviceId) {
        if (!TextUtils.isEmpty(mDeviceId)) {
            List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder()
                    .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId))
                    .build().list();
            if (!deviceEntities.isEmpty()) {
                DeviceEntity deviceEntity = deviceEntities.get(0);
                Message.DeviceSysInfo sysInfo = deviceEntity.getSysInfo();
                int cnt = sysInfo.getLimit().getCountOfContact();
                if (cnt > 0)
                    return cnt;
            }
        }
        //default 5
        return 5;
    }

    private void loadDataFromDb() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadDataFromDb: deviceId is empty");
            return;
        }

        mAssocContacts.clear();
        mNormalContacts.clear();
        mFriendsContacts.clear();
        List<ContactEntity> indb = GreenUtils.getContactEntityDao().queryBuilder().where(ContactEntityDao.Properties.DeviceId.eq(deviceId)).list();
        for (ContactEntity entity : indb) {
            if (entity.getType() == ContactEntity.TYPE_ASSOC) {
                mAssocContacts.add(entity);
                continue;
            }
            if (entity.getType() == ContactEntity.TYPE_NORMAL) {
                mNormalContacts.add(entity);
                continue;
            }
            if (entity.getType() == ContactEntity.TYPE_FRIEND) {
                mFriendsContacts.add(entity);
                continue;
            }
        }

        fillData();
    }

    private void fillData() {
        if (isExpanded(ASSOC_SEPARATOR_DATA)) {
            fillData(ASSOC_SEPARATOR_DATA, mAssocContacts);
        }
        if (isExpanded(NORMAL_SEPARATOR_DATA)) {
            fillData(NORMAL_SEPARATOR_DATA, mNormalContacts);
        }
        if (isExpanded(FRIENDS_SEPARATOR_DATA)) {
            fillData(FRIENDS_SEPARATOR_DATA, mFriendsContacts);
        }
    }

    private void fillData(ContactEntity sep, List<ContactEntity> data) {
        int sepIdx = 0;
        for (Iterator<ContactEntity> it = mData.iterator(); it.hasNext(); ) {
            ContactEntity entity = it.next();
            if (entity == sep) {
                int i = sepIdx;
                while (it.hasNext()) {
                    ContactEntity c = it.next();
                    if (isSeparatorData(c)) {
                        break;
                    }
                    it.remove();
                    i++;
                }
                mData.addAll(sepIdx + 1, data);

                int removed = i - sepIdx;
                int inserted = data.size();
                int changed = Math.min(removed, inserted);
                removed = removed - changed;
                inserted = inserted - changed;

                mListView.smoothCloseMenu();
                if (changed > 0) {
                    mListView.getAdapter().notifyItemRangeChanged(sepIdx + 1, changed);
                }
                if (removed > 0) {
                    mListView.getAdapter().notifyItemRangeRemoved(sepIdx + 1 + changed, removed);
                }
                if (inserted > 0) {
                    mListView.getAdapter().notifyItemRangeInserted(sepIdx + 1 + changed, inserted);
                }

                break;
            }

            sepIdx++;
        }
    }

    private void foldData(int position, ContactEntity sep) {
        if (!isSeparatorData(sep))
            return;
        if (isExpanded(sep)) {
            int insertCount = 0;
            if (sep == ASSOC_SEPARATOR_DATA) {
                mData.addAll(position + 1, mAssocContacts);
                insertCount = mAssocContacts.size();
            } else if (sep == NORMAL_SEPARATOR_DATA) {
                mData.addAll(position + 1, mNormalContacts);
                insertCount = mNormalContacts.size();
            } else if (sep == FRIENDS_SEPARATOR_DATA) {
                mData.addAll(position + 1, mFriendsContacts);
                insertCount = mFriendsContacts.size();
            }
            mListView.getAdapter().notifyItemRangeInserted(position + 1, insertCount);
        } else {
            int removeCount = 0;
            for (Iterator<ContactEntity> it = mData.iterator(); it.hasNext(); ) {
                ContactEntity entity = it.next();
                if (entity == sep) {
                    // fond sep
                    // remove from after this
                    while (it.hasNext()) {
                        ContactEntity c = it.next();
                        if (isSeparatorData(c)) {
                            break;
                        }
                        it.remove();
                        removeCount++;
                    }
                    mListView.getAdapter().notifyItemRangeRemoved(position + 1, removeCount);
                    break;
                }
            }
        }
        mListView.getAdapter().notifyItemChanged(position);
    }

    private void getContactsFromService(boolean showWaitingDialog) {
        if (showWaitingDialog) {
            mLoading.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.GONE);
        }
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "getContactsFromService: deviceId is empty: " + deviceId);
            return;
        }
        String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "getContactsFromService: userId is empty: " + deviceId);
            return;
        }
        Message.GetContactReqMsg reqMsg = Message.GetContactReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .setShouldContainFriends(true)
                .build();
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetContactRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "getContactsFromService onResponse: " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                limit = rspMsg.getCountLimit();
                                GreenUtils.saveContact(mDeviceId, rspMsg.getContactList());
                                loadDataFromDb();

                                if (showWaitingDialog) {
                                    mLoading.setVisibility(View.GONE);
                                    tvTitle.setVisibility(View.VISIBLE);
                                }
                                return false;
                            }
                            L.w(TAG, "getContactsFromService() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "getContactsFromService() -> exec() -> onResponse() process failure", e);
                        }
                        if (showWaitingDialog) {
                            mLoading.setVisibility(View.GONE);
                            tvTitle.setVisibility(View.VISIBLE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "getContactsFromService() -> exec() -> onException()", cause);
                        if (showWaitingDialog) {
                            mLoading.setVisibility(View.GONE);
                            tvTitle.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void addContact() {
        if (mData.size() >= limit) {
            toast(R.string.number_of_contacts_out_of_limit);
            return;
        }
        ContactsAddDialogFragment dialogFragment = new ContactsAddDialogFragment();
        dialogFragment.setContactActivity(this);
        dialogFragment.setOnPositiveButtonClickListener((name, number) -> {
            if (TextUtils.isEmpty(name)) {
                toast(R.string.contacts_name_not_null);
                return;
            }

            if (TextUtils.isEmpty(number)) {
                toast(R.string.contacts_number_not_null);
                return;
            }

            // 检查号码重复

            // 登录用户的国家代码
            String userCountryCode = PhoneNumberUtils.pickCountryCodeFromNumber(GreenUtils.getUserPhone(mUserId));
            if (TextUtils.isEmpty(userCountryCode))
                userCountryCode = "";

            // 带国家代码的输入号码
            String fullNumber = number;
            if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(number))) {
                fullNumber = userCountryCode + number;
            }
            List<ContactEntity> all = new ArrayList<>();
            for (ContactEntity one : mAssocContacts) {
                // 带国家代码的已有联系人号码
                String otherFullNum = one.getNumber();
                if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(otherFullNum))) {
                    otherFullNum = userCountryCode + otherFullNum;
                }

                if (fullNumber.equals(otherFullNum)) {
                    toast(R.string.number_already_exists);
                    return;
                }
            }
            for (ContactEntity one : mNormalContacts) {
                // 带国家代码的已有联系人号码
                String otherFullNum = one.getNumber();
                if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(otherFullNum))) {
                    otherFullNum = userCountryCode + otherFullNum;
                }

                if (fullNumber.equals(otherFullNum)) {
                    toast(R.string.number_already_exists);
                    return;
                }
            }
            for (ContactEntity one : mFriendsContacts) {
                // 带国家代码的已有联系人号码
                String otherFullNum = one.getNumber();
                if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(otherFullNum))) {
                    otherFullNum = userCountryCode + otherFullNum;
                }

                if (fullNumber.equals(otherFullNum)) {
                    toast(R.string.number_already_exists);
                    return;
                }
            }

            doAddContact(name, number);

            dialogFragment.dismiss();
        });

        dialogFragment.show(getSupportFragmentManager(), "AddContactDialog");
    }

    private void doAddContact(String name, String number) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteContact: deviceId (" + deviceId + ") is empty");
            popErrorDialog(R.string.delete_failure);
            return;
        }

        Message.Contact contact = Message.Contact.newBuilder().setName(name).setNumber(number).build();
        Message.AddContactReqMsg addContactReqMsg = Message.AddContactReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addContact(contact)
                .build();
        exec(
                addContactReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddContactRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.d(TAG, "doAddContact() -> exec() -> onResponse() SUCCESS");
                                    if (rspMsg.getContactCount() > 0) {
                                        ContactEntity entity = GreenUtils.addOrModifyContact(deviceId, rspMsg.getContact(0));
                                        if (entity != null) {
                                            listViewAddItem(entity);
                                            if (!isExpanded(NORMAL_SEPARATOR_DATA)) {
                                                setExpand(NORMAL_SEPARATOR_DATA, true);
                                                fillData();
                                            }
                                        }
                                    }
                                    return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_contacts_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.d(TAG, "doAddContact() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.d(TAG, "doAddContact() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "doAddContact() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.submit_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void unbindDevice(ContactEntity contact) {
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
                .setTitle(getString(R.string.unbind))
                .setMessage(getString(owner ? R.string.unbind_owner_device_tip : R.string.contact_unbind_user_sure))
                .setPositiveButton(getString(R.string.unbind_short), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_unbinding_device);
                    doUnbindDevice(contact, deviceId, userId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "UnbindDeviceDialog");
    }

    public void doUnbindDevice(ContactEntity contact, String deviceId, String userId) {
        if (contact == null) {
            L.e(TAG, "the contact invalid: ");
            return;
        }
        if (TextUtils.isEmpty(contact.getUserId())) {
            L.e(TAG, "the contact (" + contact.getContactId() + ") contact's userId is empty");
            return;
        }
        final Message.UnbindDevReqMsg reqMsg = Message.UnbindDevReqMsg.newBuilder()
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
                                case SUCCESS:
                                    L.i(TAG, "doUnbindDevice: " + contact.getDeviceId() + ": " + rspMsg.getErrCode());

                                    if (contact.getUserId().equals(userId)) {
                                        L.i(TAG, "the baby " + deviceId + " unbind");
                                        popSuccessDialog(R.string.unbind_and_bind_unbind_suc, hud -> GreenUtils.clearDeviceWhenUnbind(contact.getDeviceId(), contact.getUserId(), rspMsg.getClearLevel()));
                                    } else {
                                        popSuccessDialog(R.string.unbind_and_bind_unbind_suc, hud -> {
                                            GreenUtils.deleteContact(contact);
                                            listViewRemoveItem(contact);
                                        });
                                    }

                                    return false;

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

    private void deleteContact(ContactEntity contact) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteContact: deviceId (" + deviceId + ") is empty");
            popErrorDialog(R.string.delete_failure);
            return;
        }
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.delete_contact))
                .setMessage(getString(R.string.contact_delete_user_sure))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_deleting);
                    doDeleteContact(contact, deviceId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "DeleteContactDialog");
    }

    private void doDeleteContact(ContactEntity contact, String deviceId) {
        final Message.DelContactReqMsg reqMsg = Message.DelContactReqMsg.newBuilder()
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
                                case SUCCESS:
                                    L.d(TAG, "doDeleteContact() -> exec() -> onResponse() SUCCESS");
                                    GreenUtils.deleteContact(contact);
                                    popSuccessDialog(R.string.delete_success, hud -> listViewRemoveItem(contact));
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
                .setTitle(getString(R.string.delete_friend))
                .setMessage(getString(R.string.ask_sure_delete_the_friend))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    popWaitingDialog(R.string.tip_deleting);
                    doDelFriend(contact, deviceId);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "DeleteFriendDialog");
    }

    public void doDelFriend(ContactEntity contact, String deviceId) {
        final Message.DelFriendReqMsg reqMsg = Message.DelFriendReqMsg.newBuilder()
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
                            L.e(TAG, "doDelFriend " + deviceId + " " + rspMsg.getErrCode());
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    GreenUtils.deleteContact(contact);
                                    popSuccessDialog(R.string.delete_success, hud -> listViewRemoveItem(contact));
                                    return true;

                                default:
                                    break;
                            }
                            L.e(TAG, "doDelFriend() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doDelFriend() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doDelFriend() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.submit_timeout : R.string.delete_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void listViewAddItem(ContactEntity contact) {
        switch (contact.getType()) {
            case ContactEntity.TYPE_ASSOC:
                mAssocContacts.add(contact);
                if (isExpanded(ASSOC_SEPARATOR_DATA)) {
                    int pos = 0;
                    for (ContactEntity entity : mData) {
                        if (entity == NORMAL_SEPARATOR_DATA) {
                            mData.add(pos, contact);
                            mListView.smoothCloseMenu();
                            mListView.getAdapter().notifyItemInserted(pos);
                            break;
                        }
                        pos++;
                    }
                }
                break;
            case ContactEntity.TYPE_NORMAL:
                mNormalContacts.add(contact);
                if (isExpanded(NORMAL_SEPARATOR_DATA)) {
                    int pos = 0;
                    for (ContactEntity entity : mData) {
                        if (entity == FRIENDS_SEPARATOR_DATA) {
                            mData.add(pos, contact);
                            mListView.smoothCloseMenu();
                            mListView.getAdapter().notifyItemInserted(pos);
                            break;
                        }
                        pos++;
                    }
                }
                break;
            case ContactEntity.TYPE_FRIEND:
                mFriendsContacts.add(contact);
                if (isExpanded(FRIENDS_SEPARATOR_DATA)) {
                    mData.add(contact);
                    mListView.smoothCloseMenu();
                    mListView.getAdapter().notifyItemInserted(mData.size() - 1);
                }
                break;
            default:
                break;
        }
    }

    private void listViewChangeItem(ContactEntity contact) {
        List<ContactEntity> category = null;
        List<ContactEntity> dataList = null;
        switch (contact.getType()) {
            case ContactEntity.TYPE_ASSOC:
                category = mAssocContacts;
                if (isExpanded(ASSOC_SEPARATOR_DATA))
                    dataList = mData;
                break;
            case ContactEntity.TYPE_NORMAL:
                category = mNormalContacts;
                if (isExpanded(NORMAL_SEPARATOR_DATA))
                    dataList = mData;
                break;
            case ContactEntity.TYPE_FRIEND:
                category = mFriendsContacts;
                if (isExpanded(FRIENDS_SEPARATOR_DATA))
                    dataList = mData;
                break;
            default:
                break;
        }

        if (category != null) {
            int pos = 0;
            for (ContactEntity entity : category) {
                if (entity.getId().equals(contact.getId())) {
                    category.set(pos, contact);
                    break;
                }
                pos++;
            }
        }
        if (dataList != null) {
            int pos = 0;
            for (ContactEntity entity : dataList) {
                if (entity.getId().equals(contact.getId())) {
                    dataList.set(pos, contact);
                    mListView.smoothCloseMenu();
                    mListView.getAdapter().notifyItemChanged(pos);
                    break;
                }
                pos++;
            }
        }
    }

    private void listViewRemoveItem(ContactEntity contact) {
        if (contact.getId() == null)
            return;
        listViewRemoveItem(contact.getType(), contact.getId());
    }

    private void listViewRemoveItem(int contactType, long entityId) {
        List<ContactEntity> category;
        List<ContactEntity> datas = null;
        switch (contactType) {
            case ContactEntity.TYPE_NORMAL:
                category = mNormalContacts;
                if (isExpanded(NORMAL_SEPARATOR_DATA)) {
                    datas = mData;
                }
                break;
            case ContactEntity.TYPE_ASSOC:
                category = mAssocContacts;
                if (isExpanded(ASSOC_SEPARATOR_DATA)) {
                    datas = mData;
                }
                break;
            case ContactEntity.TYPE_FRIEND:
                category = mFriendsContacts;
                if (isExpanded(FRIENDS_SEPARATOR_DATA)) {
                    datas = mData;
                }
                break;
            default:
                return;
        }

        if (category != null) {
            for (Iterator<ContactEntity> it = category.iterator(); it.hasNext(); ) {
                if (it.next().getId() == entityId) {
                    it.remove();
                    break;
                }
            }
        }
        if (datas != null) {
            int pos = 0;
            for (Iterator<ContactEntity> it = mData.iterator(); it.hasNext(); ) {
                if (it.next().getId() == entityId) {
                    it.remove();
                    mListView.smoothCloseMenu();
                    mListView.getAdapter().notifyItemRemoved(pos);
                    break;
                }
                pos++;
            }
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        getContactsFromService(false);
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (!reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId))
            return;

        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId)) {
            return;
        }

        String unbindDeviceId = reqMsg.getUsrDevAssoc().getDeviceId();
        L.i(TAG, "device " + unbindDeviceId + " unbind");
        for (ContactEntity entity : mAssocContacts) {
            if (unbindDeviceId.equals(entity.getDeviceId())) {
                listViewRemoveItem(entity);
                break;
            }
        }
    }

    @Override
    public void onUsrDevAssocModified(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUsrDevAssocModifiedReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            getContactsFromService(false);
        }
    }

    @Override
    public void onDevContactChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyContactChangedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            loadDataFromDb();
        }
    }

    @Override
    public void onDeviceFriendChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyFriendChangedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            getContactsFromService(false);
        }
    }
}
