package com.cqkct.FunKidII.Ui.Activity.Keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.Keyboard.Adapter.FaceCategoryAdapter;
import com.cqkct.FunKidII.Ui.view.MicroChatEditText;
import com.cqkct.FunKidII.Ui.view.RecordButton;
import com.cqkct.FunKidII.Utils.LengthLimitTextWatcher;

import org.greenrobot.eventbus.EventBus;


public class ChatKeyboard extends RelativeLayout {

    private static final String SHARE_PREFERENCE_NAME = "EmotionKeyboard";
    private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";
    private SharedPreferences sp;
    private int mKeyBoardHeight;
    private boolean keyboardShown;

    private View thisRootView;

    /**
     * 最上层输入框
     */
    private MicroChatEditText textEditor;
    private ImageView mBtnEmoticon, mBtnBoard;
    private RecordButton recordButton;
    /**
     * 表情
     */
    private ViewPager mPagerFaceCategory;
    private View mEmoticonLayout;

    private FaceCategoryAdapter adapter;  //点击表情按钮时的适配器

    private Context context;


    private Animation mEmoticonVisibleAnim, mEmoticonGoneAnim;

    private View recordIndicatorDialogDepView;

    public ChatKeyboard(Context context) {
        super(context);
        init(context);
    }

    public ChatKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatKeyboard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        thisRootView = View.inflate(context, R.layout.include_emotion_bar, null);
        this.addView(thisRootView);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initData();
        this.initWidget();
    }

    private void initData() {
        sp = getContext().getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        mKeyBoardHeight = sp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 0);
    }

    private void initWidget() {
        textEditor = findViewById(R.id.text_editor);
        textEditor.addTextChangedListener(new LengthLimitTextWatcher(textEditor, getResources().getInteger(R.integer.maxLength_of_text_chat_message)));

        mBtnBoard = findViewById(R.id.voice_message_mode_button);
        mBtnBoard.setOnClickListener(v -> {
            if (textEditor.isShown()) {
                switchToVoiceMode();
            } else {
                switchToTextMode();
            }
        });
        mBtnEmoticon = findViewById(R.id.emotion_button);
        mBtnEmoticon.setOnClickListener(v -> {
            if (isEmoticonBoardShow()) {
                hideEmoticonLayout(textEditor.isShown());
            } else {
                showEmoticonLayout();
            }
        });
        recordButton = findViewById(R.id.bt_press_speak);
        recordButton.setActivity((Activity) context);
        recordButton.setOnFinishedRecordListener((audioFile, time) -> {
            if (audioFile.exists()) {
                EventBus.getDefault().post(new Event.SendVoiceChatMessage(System.currentTimeMillis(), audioFile, time));
            }
        });
        View buttonLayoutForeground = findViewById(R.id.button_layout_foreground);
        View EmoticonLayoutForeground = findViewById(R.id.toolbox_layout_face_foreground);
        recordButton.setOnDialogShowDismissListener(show -> {
            if (show) {
                mBtnBoard.setImageResource(R.drawable.keyboard_voicing);
                mBtnEmoticon.setImageResource(R.drawable.expression_voicing);
                buttonLayoutForeground.setVisibility(VISIBLE);
                EmoticonLayoutForeground.setVisibility(VISIBLE);
            } else {
                mBtnBoard.setImageResource(R.drawable.keyboard);
                mBtnEmoticon.setImageResource(R.drawable.expression);
                buttonLayoutForeground.setVisibility(GONE);
                EmoticonLayoutForeground.setVisibility(GONE);
            }
        });

        mEmoticonLayout = findViewById(R.id.toolbox_layout_face);
        mPagerFaceCategory = findViewById(R.id.toolbox_pagers_face);

        adapter = new FaceCategoryAdapter(((FragmentActivity) getContext()).getSupportFragmentManager());
        mPagerFaceCategory.setAdapter(adapter);

        // 点击消息输入框
        textEditor.setOnTouchListener((v, event) -> {
            hideEmoticonLayout(true);
            return false;
        });

        textEditor.setOnEditorActionListener((v, actionId, event) -> {
            // 当 actionId == XX_SEND 或者 XX_DONE 时都触发
            // 或者 event.getKeyCode == ENTER 且 event.getAction == ACTION_DOWN 时也触发
            // 注意，这时一定要判断 event != null。因为在某些输入法上会返回 null。
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                if (v.length() > 0) {
                    String content = v.getText().toString();
                    EventBus.getDefault().post(new Event.SendTextChatMessage(System.currentTimeMillis(), content));
                    v.setText("");
                }

                return true;
            }
            return false;
        });
    }

    public void switchToTextMode() {
        if (!textEditor.isShown()) {
            showTextModeView();
            textEditor.requestFocus();
            hideEmoticonLayout(true);
        }
    }

    public void switchToVoiceMode() {
        if (!recordButton.isShown()) {
            textEditor.clearFocus();
            showVoiceModeView();
            hideKeyboard();
            hideEmoticonLayout(false);
        }
    }

    private void showTextModeView() {
        textEditor.setVisibility(View.VISIBLE);
        recordButton.setVisibility(View.GONE);
        mBtnBoard.setImageResource(R.drawable.voice);
    }

    private void showVoiceModeView() {
        textEditor.setVisibility(View.GONE);
        recordButton.setVisibility(View.VISIBLE);
        mBtnBoard.setImageResource(R.drawable.keyboard);
    }


    public void setTextEditorEnable(boolean enable) {
        if (mBtnBoard.isEnabled() != enable) {
            if (!enable) {
                switchToVoiceMode();
            }
            mBtnBoard.setEnabled(enable);
            mBtnBoard.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public EditText getEditTextBox() {
        return textEditor;
    }

    public void showEmoticonLayout() {
        if (!isEmoticonBoardShow()) {
            if (keyboardShown) {
                lockHeight();
                hideKeyboard();
            }
            if (mBtnBoard.isEnabled() && !textEditor.isShown()) {
                showTextModeView();
            }
            if (mEmoticonVisibleAnim == null) {
                mEmoticonVisibleAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0, Animation.RELATIVE_TO_SELF, 0,
                        Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF,
                        0);
                mEmoticonVisibleAnim.setDuration(50);
                mEmoticonVisibleAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {
                        mEmoticonLayout.setVisibility(VISIBLE);
                    }
                    @Override public void onAnimationEnd(Animation animation) { }

                    @Override public void onAnimationRepeat(Animation animation) { }
                });
            }
            mEmoticonLayout.startAnimation(mEmoticonVisibleAnim);
        }
    }


    public boolean isEmoticonBoardShow() {
        return mEmoticonLayout.getVisibility() == VISIBLE;
    }

    public boolean hideEmoticonLayout(boolean wantShowKeyboard) {
        boolean shouldHideEmoticonLayout = isEmoticonBoardShow();
        boolean shouldShowKeyboard = wantShowKeyboard && textEditor.isShown();
        if (shouldHideEmoticonLayout) {
            if (shouldShowKeyboard) {
                // 表情面板正在显示，且需要弹出键盘
                // 我们锁定一下面板高度，待键盘弹出后解锁高度
                lockHeight();
            }
            if (mEmoticonGoneAnim == null) {
                mEmoticonGoneAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0, Animation.RELATIVE_TO_SELF, 0,
                        Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF,
                        1);
                mEmoticonGoneAnim.setDuration(50);
                mEmoticonGoneAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) { }
                    @Override public void onAnimationEnd(Animation animation) {
                        mEmoticonLayout.setVisibility(GONE);
                    }
                    @Override public void onAnimationRepeat(Animation animation) { }
                });
            }
            mEmoticonLayout.startAnimation(mEmoticonGoneAnim);
        }
        if (shouldShowKeyboard) {
            showKeyboard();
        }
        return shouldHideEmoticonLayout;
    }

    private void lockHeight() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        if (!isHeightLocked(params)) {
            params.topToTop = ConstraintSet.PARENT_ID;
            params.topMargin = (int) getY();
            params.bottomToBottom = ConstraintSet.UNSET;
            setLayoutParams(params);
        }
    }

    private boolean isHeightLocked() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        return params.topToTop == ConstraintSet.PARENT_ID;
    }

    private boolean isHeightLocked(ConstraintLayout.LayoutParams params) {
        return params.topToTop == ConstraintSet.PARENT_ID;
    }

    private void unlockHeight() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        if (isHeightLocked(params)) {
            params.topToTop = ConstraintSet.UNSET;
            params.topMargin = 0;
            params.bottomToBottom = ConstraintSet.PARENT_ID;
            setLayoutParams(params);
        }
    }

    public boolean isInterceptBackPress() {
        return hideEmoticonLayout(false);
    }

    public void onKeyboardChange(boolean isPopup, int keyboardHeight) {
        if (isPopup) {
            if (mKeyBoardHeight != keyboardHeight) {
                mKeyBoardHeight = keyboardHeight;
                sp.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, mKeyBoardHeight).apply();
            }
        }
        keyboardShown = isPopup;

        unlockHeight();
    }

    public boolean iskeyboardShown() {
        return keyboardShown;
    }

    private volatile InputMethodManager mInputMethodManager;
    private InputMethodManager getInputMethodManager() {
        if (mInputMethodManager == null) {
            synchronized (this) {
                if (mInputMethodManager == null) {
                    mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                }
            }
        }
        return mInputMethodManager;
    }

    /**
     * 隐藏软键盘
     */
    public void hideKeyboard() {
        if (keyboardShown) {
            InputMethodManager imm = getInputMethodManager();
            if (imm != null) {
                imm.hideSoftInputFromWindow(textEditor.getWindowToken(), 0);
            }
        }
    }

    /**
     * 显示软键盘
     */
    public void showKeyboard() {
        if (!keyboardShown) {
            InputMethodManager imm = getInputMethodManager();
            if (imm != null) {
                textEditor.requestFocus();
                textEditor.post(() -> imm.showSoftInput(textEditor, 0));
            }
        }
    }

    public void setRecordIndicatorDialogDepView(View view) {
        recordIndicatorDialogDepView = view;
        if (recordButton != null) {
            recordButton.setRecordIndicatorDialogDepView(recordIndicatorDialogDepView);
        }
    }
}
