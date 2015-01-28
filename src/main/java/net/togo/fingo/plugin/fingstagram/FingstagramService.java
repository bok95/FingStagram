package net.togo.fingo.plugin.fingstagram;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class FingstagramService extends Service {
    private TextView mPopupView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    private float START_X, START_Y;
    private int PREV_X, PREV_Y;
    private int MAX_X = -1, MAX_Y = -1;
    @Override
    public void onCreate() {
        super.onCreate();
        mPopupView = new TextView(this);
        mPopupView.setText("dskfjs");
        mPopupView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mPopupView.setTextColor(Color.BLUE);
        mPopupView.setBackgroundColor(Color.argb(127, 0, 255, 255));

        mPopupView.setOnTouchListener(mViewTouchListener);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.LEFT | Gravity.TOP;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mPopupView, mParams);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setMaxPosition();
        optimizePosition();
    }

    @Override
    public void onDestroy() {
        if(mWindowManager != null) {
            if(mPopupView != null) mWindowManager.removeView(mPopupView);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(MAX_X == -1)
                        setMaxPosition();
                    START_X = event.getRawX();
                    START_Y = event.getRawY();
                    PREV_X = mParams.x;
                    PREV_Y = mParams.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int x = (int)(event.getRawX() - START_X);
                    int y = (int)(event.getRawY() - START_Y);

                    mParams.x = PREV_X + x;
                    mParams.y = PREV_Y + y;

                    optimizePosition();
                    mWindowManager.updateViewLayout(mPopupView, mParams);
                    break;
            }

            return true;
        }
    };
    private void setMaxPosition() {
        DisplayMetrics matrix = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(matrix);

        MAX_X = matrix.widthPixels - mPopupView.getWidth();
        MAX_Y = matrix.heightPixels - mPopupView.getHeight();
    }

    private void optimizePosition() {
        if(mParams.x > MAX_X) mParams.x = MAX_X;
        if(mParams.y > MAX_Y) mParams.y = MAX_Y;
        if(mParams.x < 0) mParams.x = 0;
        if(mParams.y < 0) mParams.y = 0;
    }
}
