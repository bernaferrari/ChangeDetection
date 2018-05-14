package com.example.changedetection.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.changedetection.R;

/**
 * Created by Kosh on 20 Nov 2016, 12:21 AM
 */
public class StateLayout extends NestedScrollView {

    private static final int SHOW_PROGRESS_STATE = 1;
    private static final int HIDE_PROGRESS_STATE = 2;
    private static final int HIDE_RELOAD_STATE = 3;
    private static final int SHOW_RELOAD_STATE = 4;
    private static final int SHOW_EMPTY_STATE = 7;
    private static final int HIDDEN = 5;
    private static final int SHOWN = 6;
    TextView emptyText;
    ProgressBar loading;
    int layoutState = HIDDEN;
    String emptyTextValue;
//    boolean showReload = true;
//    private OnClickListener onReloadListener;

    public StateLayout(Context context) {
        super(context);
    }

    public StateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StateLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void newLoading() {
//        Logger.d("state: newLoading");
        layoutState = SHOW_PROGRESS_STATE;
        setVisibility(VISIBLE);
        emptyText.setVisibility(GONE);
        loading.setVisibility(VISIBLE);
    }

//    public void showProgress() {
//        Logger.d("state: showProgress");
//
//        layoutState = SHOW_PROGRESS_STATE;
//        setVisibility(VISIBLE);
//        emptyText.setVisibility(GONE);
//        loading.setVisibility(GONE);
//    }
//
//    public void hideProgress() {
////        Logger.d("state: hideProgress");
//
//        layoutState = HIDE_PROGRESS_STATE;
//        emptyText.setVisibility(VISIBLE);
//        loading.setVisibility(VISIBLE);
//        setVisibility(GONE);
//    }

//    public void hideReload() {
////        Logger.d("state: hideReload");
//
//        layoutState = HIDE_RELOAD_STATE;
//        loading.setVisibility(GONE);
//        emptyText.setVisibility(GONE);
//        setVisibility(GONE);
//    }

//    public void showReload(int adapterCount) {
//        showReload = adapterCount == 0;
//        showReload();
//    }

//    protected void showReload() {
////        Logger.d("state: showReload");
//
//        hideProgress();
//        if (showReload) {
//            layoutState = SHOW_RELOAD_STATE;
//            loading.setVisibility(VISIBLE);
//            emptyText.setVisibility(VISIBLE);
//            setVisibility(VISIBLE);
//        }
//    }

    public void setEmptyText(@StringRes int resId) {
        setEmptyText(getResources().getString(resId));
    }

    public void setEmptyText(@NonNull String text) {
//        this.emptyTextValue = text + "\n\n¯\\_(ツ)_/¯";
        emptyText.setText(text);
    }

    public void showEmptyState() {
//        Logger.d("state: showEmptyState");

//        hideProgress();
//        hideReload();

//        loading.setVisibility(GONE);

        setVisibility(VISIBLE);
        emptyText.setVisibility(VISIBLE);
        loading.setVisibility(GONE);

        layoutState = SHOW_EMPTY_STATE;// last so it override visibility state.
    }

//    public void setOnReloadListener(OnClickListener onReloadListener) {
//        this.onReloadListener = onReloadListener;
//    }

//    @Override
//    public void setVisibility(int visibility) {
//        super.setVisibility(visibility);
//        if (visibility == GONE || visibility == INVISIBLE) {
//            layoutState = HIDDEN;
//        } else {
//            layoutState = SHOWN;
//        }
//    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inflate(getContext(), R.layout.empty_layout, this);

        emptyText = findViewById(R.id.empty_text);
        loading = findViewById(R.id.progress_bar);

//        if (isInEditMode()) return;
//        emptyText.setFreezesText(true);
    }

//    @Override
//    protected void onDetachedFromWindow() {
////        onReloadListener = null;
//        super.onDetachedFromWindow();
//    }

//    @Override public Parcelable onSaveInstanceState() {
//        return StateSaver.saveInstanceState(this, super.onSaveInstanceState());
//    }
//
//    @Override public void onRestoreInstanceState(Parcelable state) {
//        super.onRestoreInstanceState(StateSaver.restoreInstanceState(this, state));
//        onHandleLayoutState();
//    }

//    private void onHandleLayoutState() {
//        setEmptyText(emptyTextValue);
//        switch (layoutState) {
//            case SHOW_PROGRESS_STATE:
////                showProgress();
//                break;
//            case HIDE_PROGRESS_STATE:
////                hideProgress();
//                break;
//            case HIDE_RELOAD_STATE:
////                hideReload();
//                break;
//            case SHOW_RELOAD_STATE:
////                showReload();
//                break;
//            case HIDDEN:
//                setVisibility(GONE);
//                break;
//            case SHOW_EMPTY_STATE:
//                showEmptyState();
//                break;
//            case SHOWN:
//                setVisibility(VISIBLE);
////                showReload();
//                break;
//        }
//    }
}
