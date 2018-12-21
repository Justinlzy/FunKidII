package com.cqkct.FunKidII.Ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialog;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.ContactsActivity;
import com.cqkct.FunKidII.Ui.Listener.DebouncedOnClickListener;
import com.cqkct.FunKidII.Utils.AndroidPermissions;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.LengthLimitTextWatcher;
import com.cqkct.FunKidII.Utils.PhoneNumberInputFilter;

public class ContactsAddDialogFragment extends BaseBlurDialogFragment {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private static final int ACTIVITY_REQUEST_CODE_READ_SYSTEM_CONTACTS = 1;
    private static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_READ_CONTACTS_PERMISSION = 2;
    private ContactsActivity contactsActivity;

    public void setContactActivity(ContactsActivity activity){
        contactsActivity = activity;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_READ_SYSTEM_CONTACTS && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String[] contact = getPhoneContacts(uri);
                if (contact != null) {
                    if (mNumberEdit != null) {
                        mNumberEdit.setText(contact[1]); // 手机号
                        mNumberEdit.setSelection(mNumberEdit.length());
                    }
                    if (mNameEdit != null) {
                        mNameEdit.requestFocus();
                        mNameEdit.setSelection(mNameEdit.length());
                    }
                }
            }
        } else if (requestCode == ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_READ_CONTACTS_PERMISSION) {
        }
    }

    /**
     * 读取联系人信息
     *
     * @param uri
     */
    private String[] getPhoneContacts(Uri uri) {
        String[] contact = new String[2];
        //得到ContentResolver对象
        ContentResolver cr = getActivity().getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            //取得联系人姓名
            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            contact[0] = cursor.getString(nameFieldColumnIndex);
            contact[1] = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            L.i("contacts", contact[0]);
            L.i("contactsUsername", contact[1]);
            if (!TextUtils.isEmpty(contact[1])) {
                contact[1] = contact[1].replace(" ", "");
                contact[1] = contact[1].replace("-", "");
            }
            cursor.close();
        } else {
            return null;
        }
        return contact;
    }

    private AppCompatDialog mDialog;
    private InputFilter emotionFilter = new EmojiInputFilter();
    private EditText mNameEdit, mNumberEdit;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new AppCompatDialog(getActivity(), R.style.FunKidII_2_Dialog_style);
        mDialog.setContentView(R.layout.contacts_add_dialog_fragment_layout);

        mNameEdit = mDialog.findViewById(R.id.name);
        mNameEdit.setFilters(new InputFilter[]{emotionFilter});
        mNameEdit.addTextChangedListener(new LengthLimitTextWatcher(mNameEdit, getResources().getInteger(R.integer.maxLength_of_contact_name)));
        if (contactsActivity != null) {
            contactsActivity.showSoftKeyboard(mNameEdit);
        }

        mNumberEdit = mDialog.findViewById(R.id.number);
        mNumberEdit.setFilters(new InputFilter[]{new PhoneNumberInputFilter(getResources().getInteger(R.integer.maxLength_of_phone_number))});

        View importSystemContacts = mDialog.findViewById(R.id.import_system_contact);
        importSystemContacts.setOnClickListener(new DebouncedOnClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    if (AndroidPermissions.shouldShowGuide(ContactsAddDialogFragment.this, Manifest.permission.READ_CONTACTS)) {
                        showContactsPermissionGuide();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
                    }
                    return;
                }

                startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), ACTIVITY_REQUEST_CODE_READ_SYSTEM_CONTACTS);
            }
        });

        Button buttonPositive = mDialog.findViewById(R.id.button_positive);
        buttonPositive.setOnClickListener(v -> {
            if (mOnPositiveButtonClickListener != null) {
                mOnPositiveButtonClickListener.onClick(mNameEdit.getText().toString(), mNumberEdit.getText().toString());
            } else {
                dismiss();
            }
        });
        mDialog.findViewById(R.id.button_negative).setOnClickListener(v -> dismiss());

        mDialog.setCanceledOnTouchOutside(false);
        return mDialog;
    }

    private OnPositiveButtonClickListener mOnPositiveButtonClickListener;

    public void setOnPositiveButtonClickListener(OnPositiveButtonClickListener listener) {
        mOnPositiveButtonClickListener = listener;
    }

    public interface OnPositiveButtonClickListener {
        void onClick(String name, String number);
    }

    private void showContactsPermissionGuide() {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getText(R.string.contacts_permission))
                .setMessage(getText(R.string.please_enable_contacts_permission_in_setting))
                .setPositiveButton(getText(R.string.ok), (dialog, which) -> {
                    Intent intent = AndroidPermissions.permissionSettingPageIntent(getContext());
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_READ_CONTACTS_PERMISSION);
                })
                .setNegativeButton(getText(R.string.cancel), null);
        dialogFragment.show(getFragmentManager(), "showContactsPermissionGuide");
    }

    @Override
    public void dismiss() {
        hideSoftKeyBoard();
        super.dismiss();
    }

    private void hideSoftKeyBoard() {
        if (mDialog == null || mNameEdit == null || mNumberEdit == null)
            return;
        View currentFocus = null;
        if (mNameEdit.isFocused()) {
            currentFocus = mNameEdit;
        } else if (mNumberEdit.isFocused()) {
            currentFocus = mNumberEdit;
        }
        if (currentFocus != null) {
            hideSoftKeyBoard(currentFocus);
        }
    }

    public void hideSoftKeyBoard(@NonNull final View v) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0  /* flags */);
        }
    }
}
