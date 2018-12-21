package com.cqkct.FunKidII.Ui.Adapter;

import android.app.Dialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BabyCardActivity;
import com.cqkct.FunKidII.Ui.fragment.CustomDialog;
import com.cqkct.FunKidII.Ui.view.CustomDatePicker;
import com.cqkct.FunKidII.Ui.view.NumberPickerBlue;
import com.cqkct.FunKidII.Ui.view.RecyclingPagerAdapter;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.PhoneNumberInputFilter;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.Utils.UserPermission;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import protocol.Message;

public class BabyCardAdapter extends RecyclingPagerAdapter implements View.OnClickListener {

    private final List<BabyEntity> babyEntityList;
    private final Context mContext;
    private BabyCardActivity.TaskHandler taskHandler;
    private BabyDataChangedListener dataListener;

    public BabyCardAdapter(Context context, List<BabyEntity> data, BabyCardActivity.TaskHandler mTaskHandler, BabyDataChangedListener babyDataChangedListener) {
        mContext = context;
        babyEntityList = data;
        this.taskHandler = mTaskHandler;
        this.dataListener = babyDataChangedListener;
    }

    @Override
    public int getCount() {
        return babyEntityList == null ? 0 : babyEntityList.size();
    }

    @Override
    public int getItemPosition(Object object) {
        if (object == null)
            return POSITION_UNCHANGED;

        View view = (View) object;
        BabyCardTagData tagData = view.getTag() == null ? null : (BabyCardTagData) view.getTag();
        if (tagData == null)
            return POSITION_NONE;

        if (tagData.data == null)
            return POSITION_NONE;

        if (babyEntityList == null || babyEntityList.isEmpty())
            return POSITION_NONE;

        if (tagData.position < 0 || tagData.position >= babyEntityList.size())
            return POSITION_NONE;

        BabyEntity babyEntity = babyEntityList.get(tagData.position);
        if (!BabyEntity.equals(tagData.data, babyEntity) || tagData.data.getIs_select() != babyEntity.getIs_select())
            return POSITION_NONE;

        return POSITION_UNCHANGED;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int position) {
        return AdapterView.ITEM_VIEW_TYPE_IGNORE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            convertView = LayoutInflater.from(container.getContext()).inflate(R.layout.baby_card, container, false);
            BabyCardTagData tagData = new BabyCardTagData();
            convertView.setTag(tagData);
        }
        BabyCardTagData tagData = (BabyCardTagData) convertView.getTag();
        BabyEntity data = babyEntityList.get(position);
        tagData.position = position;
        tagData.data = data;

        ImageView babyIcon = convertView.findViewById(R.id.head_icon);
        babyIcon.setOnClickListener(v -> taskHandler.changeBabyHeadIcon(position));

        TextView babyName = convertView.findViewById(R.id.baby_name);
        TextView babyDevNumber = convertView.findViewById(R.id.device_number);


        if (!TextUtils.isEmpty(data.getDeviceId())) {
            babyDevNumber.setText(data.getDeviceId());

            int alternateAvatarResId = DeviceInfo.getBabySex(data.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
            Glide.with(babyIcon)
                    .load(new DeviceAvatar(data.getDeviceId(), data.getBabyAvatar(), alternateAvatarResId))
                    .apply(RequestOptions.placeholderOf(babyIcon.getDrawable()))
                    .apply(RequestOptions.errorOf(alternateAvatarResId))
                    .apply(GlideDefines.DEVICE_AVATAR_OPTIONS)
                    .into(babyIcon);

            babyName.setText(TextUtils.isEmpty(data.getName()) ? container.getContext().getString(R.string.baby) : data.getName());
        }


        TextView number = convertView.findViewById(R.id.number);
        if (!TextUtils.isEmpty(data.getPhone()))
            number.setText(data.getPhone());

        TextView sex = convertView.findViewById(R.id.sex);
        if (!TextUtils.isEmpty(getSex(data.getSex())))
            sex.setText(getSex(data.getSex()));
        TextView tvBirthday = convertView.findViewById(R.id.birthday);
        tvBirthday.setText(data.getBirthday() == 0 ? mContext.getString(R.string.please_select_birthday) : new SimpleDateFormat("yyyy-MM-dd").format(new Date(data.getBirthday() * 1000L)));
        TextView relation = convertView.findViewById(R.id.relation);
        if (!TextUtils.isEmpty(data.getRelation()))
            relation.setText(RelationUtils.decodeRelation(mContext, data.getRelation()));

        TextView setCurrentBaby = convertView.findViewById(R.id.select_as_current);
        setCurrentBaby.setOnClickListener(this);
        setCurrentBaby.setTag(position);
        setCurrentBaby.setText(data.getIs_select() ? mContext.getText(R.string.current_baby) : mContext.getText(R.string.set_as_current));
        convertView.findViewById(R.id.number_layout).setOnClickListener(this);
        convertView.findViewById(R.id.number_layout).setTag(position);
        convertView.findViewById(R.id.birthday_layout).setOnClickListener(this);
        convertView.findViewById(R.id.birthday_layout).setTag(position);
        convertView.findViewById(R.id.sex_layout).setOnClickListener(this);
        convertView.findViewById(R.id.sex_layout).setTag(position);
        convertView.findViewById(R.id.relation_layout).setOnClickListener(this);
        convertView.findViewById(R.id.relation_layout).setTag(position);
        convertView.findViewById(R.id.unbind_layout).setOnClickListener(this);
        convertView.findViewById(R.id.unbind_layout).setTag(position);
        convertView.findViewById(R.id.baby_card_qr_code).setOnClickListener(this);
        convertView.findViewById(R.id.baby_card_qr_code).setTag(position);


        convertView.findViewById(R.id.baby_name).setOnClickListener(this);
        convertView.findViewById(R.id.baby_name).setTag(position);
        convertView.findViewById(R.id.baby_name_edit).setOnClickListener(this);
        convertView.findViewById(R.id.baby_name_edit).setTag(position);

        if (UserPermission.hasEditPermission(data.getPermission())) {
            convertView.findViewById(R.id.number_can_edit_icon);
            convertView.findViewById(R.id.sex_can_edit_icon);
            convertView.findViewById(R.id.birthday_can_edit_icon);
            convertView.findViewById(R.id.relation_can_edit_icon);
        }

        return convertView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.head_icon: {
                taskHandler.changeBabyHeadIcon((int) v.getTag());
            }
            break;
            case R.id.baby_card_qr_code:
                taskHandler.babyCardQrCode((int) v.getTag());
                break;
            case R.id.select_as_current: {
                taskHandler.selectAsCurrent((int) v.getTag());
            }
            break;
            case R.id.unbind_layout:
                taskHandler.unbindAndBind((int) v.getTag());
                break;
            case R.id.relation_layout:
                taskHandler.modifyRelation((int) v.getTag());
                break;
            case R.id.baby_name:
            case R.id.baby_name_edit:
                showEditNameDialog(mContext, (int) v.getTag());
                break;
            case R.id.number_layout: {
                showNumberDialog(mContext, (int) v.getTag());
            }
            break;
            case R.id.sex_layout: {
                showSexDialog(mContext, (int) v.getTag());
            }
            break;
            case R.id.birthday_layout: {
                showBirthdayDialog((int) v.getTag());
            }
            break;
        }
    }

    private void showEditNameDialog(final Context mContext, int position) {
        BabyEntity entity = babyEntityList.get(position);
        Dialog dialog = new CustomDialog(mContext, R.style.TimePickerDialog);
        View mView = LayoutInflater.from(mContext).inflate(R.layout.baby_information_name, null);
        TextView title = mView.findViewById(R.id.dialog_title);
        title.setText(R.string.baby_name);
        final EditText EtBabyName = mView.findViewById(R.id.tv_baby_information_name);
        EtBabyName.setText(entity.getName());
        EtBabyName.setSelection(EtBabyName.length());
        mView.findViewById(R.id.ok).setOnClickListener(v -> {
            String newBabyName = EtBabyName.getText().toString();
            if (newBabyName.isEmpty()) {
                dataListener.showErrorMessage(R.string.baby_name_not_allow_empty);
            } else {
                if (!entity.getName().equals(newBabyName)) {
                    entity.setName(newBabyName);
                    dataListener.dataChanged(entity, position);
                }
                dialog.dismiss();
            }
        });
        mView.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
        //最大8位
        EtBabyName.setFilters(new InputFilter[]{new EmojiInputFilter(), new InputFilter.LengthFilter(mContext.getResources().getInteger(R.integer.maxLength_of_baby_name))});
        dialog.setContentView(mView);
        dialog.show();
    }


    private void showNumberDialog(final Context mContext, int position) {
        BabyEntity entity = babyEntityList.get(position);
        Dialog dialog = new CustomDialog(mContext, R.style.TimePickerDialog);

        View mView = LayoutInflater.from(mContext).inflate(R.layout.baby_information_number, null);
        TextView title = mView.findViewById(R.id.dialog_title);
        title.setText(R.string.watch_number);
        final EditText number = mView.findViewById(R.id.tv_baby_information_number);
        number.setText(entity.getPhone());
        number.setSelection(number.length());
        mView.findViewById(R.id.ok).setOnClickListener(v -> {
            String newNumber = number.getText().toString();
            if (newNumber.isEmpty()) {
                dataListener.showErrorMessage(R.string.phonenumber_can_not_be_null);
            } else {
                if (!entity.getPhone().equals(newNumber)) {
                    entity.setPhone(newNumber);
                    dataListener.dataChanged(entity, position);
                }
                dialog.dismiss();
            }
        });
        mView.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
        InputFilter phoneNumberInputFilter = new PhoneNumberInputFilter(mContext.getResources().getInteger(R.integer.maxLength_of_phone_number));
        number.setFilters(new InputFilter[]{phoneNumberInputFilter});

        dialog.setContentView(mView);
        dialog.show();
    }

    private void showSexDialog(Context context, int position) {
        final int[] editSex = new int[1];
        final String[] displayValues = new String[]{context.getString(R.string.male), context.getString(R.string.female)};
        BabyEntity entity = babyEntityList.get(position);


        View mView = LayoutInflater.from(context).inflate(R.layout.baby_information_grade, null);
        Dialog dialog = new CustomDialog(mContext, R.style.TimePickerDialog);
        dialog.setContentView(mView);
        dialog.show();

        TextView title = mView.findViewById(R.id.dialog_title);
        title.setText(R.string.select_sex);

        NumberPickerBlue numberPicker = mView.findViewById(R.id.sex_pick);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(displayValues.length - 1);
        numberPicker.setDisplayedValues(displayValues);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Utils.setNumberPickerDividerColor(numberPicker, context);


        int sex = entity.getSex();
        editSex[0] = sex <= 0 ? 0 : sex - 1;
        numberPicker.setValue(editSex[0]);

        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> editSex[0] = newVal);

        mView.findViewById(R.id.ok).setOnClickListener(v -> {
            dialog.dismiss();
            int newSex = editSex[0] == 0 ? Message.Baby.Sex.MALE_VALUE : Message.Baby.Sex.FEMALE_VALUE;
            if (newSex != sex) {
                entity.setSex(newSex);
                dataListener.dataChanged(entity, position);
            }

        });
        mView.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());

    }

    private void showBirthdayDialog(int position) {
        BabyEntity entity = babyEntityList.get(position);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        if (entity.getBirthday() == 0) {
            calendar.add(Calendar.YEAR, -3);
        } else {
            calendar.setTimeInMillis(entity.getBirthday() * 1000L);
        }

        CustomDatePicker datePicker;
        String time, date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        time = sdf.format(new Date());
        date = simpleDateFormat.format(calendar.getTime());
        // 设置年月日
        datePicker = new CustomDatePicker(mContext, time1 -> {
            long birthday = time1 / 1000L;
            if (birthday == 0)
                birthday = 1; // 避免出现 0 值，0 表示未填写
            if (entity.getBirthday() != birthday) {
                entity.setBirthday(birthday);
                dataListener.dataChanged(entity, position);
            }
        }, "1900-01-01 00:00", time);
        datePicker.showSpecificTime(false); //显示时和分
        datePicker.setIsLoop(false);
        datePicker.setDayIsLoop(true);
        datePicker.setMonIsLoop(true);
        datePicker.show(date);
    }

    private String getSex(int sexIndex) {
        String sex = mContext.getString(R.string.please_select_sex);
        if (sexIndex == Message.Baby.Sex.MALE_VALUE) {
            sex = mContext.getString(R.string.male);
        } else if (sexIndex == Message.Baby.Sex.FEMALE_VALUE) {
            sex = mContext.getString(R.string.female);
        }
        return sex;
    }


    static class BabyCardTagData {
        public int position;
        BabyEntity data;
    }

    public interface BabyDataChangedListener {
        void dataChanged(BabyEntity entity, int position);

        void showErrorMessage(int id);
    }
}
