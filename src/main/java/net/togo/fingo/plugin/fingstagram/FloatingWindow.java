package net.togo.fingo.plugin.fingstagram;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class FloatingWindow implements View.OnTouchListener {
  private static final String TAG = "FloatingWindow";
  private static final boolean DEBUG = true;

  // TODO: this size works well on a Nexus 7... make this more generic later.
  private static final int INITIAL_WIDTH = 400;
  private static final int INITIAL_HEIGHT = 400;

  private static final float MIN_SCALE_FACTOR = 0.75f;
  private static final float MAX_SCALE_FACTOR = 3.0f;

  private static final long SLEEP_DELAY_MILLIS = 5000;

  private static final int MSG_SNAP = 0;

  @SuppressLint("HandlerLeak")
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_SNAP:
          int fromX = msg.arg1, toX = msg.arg2;
          snap(fromX, toX);
          break;
      }
    }
  };

  private final Context mContext;
  private final WindowManager mWindowManager;
  private final WindowManager.LayoutParams mWindowParams;
  private final Point mDisplaySize = new Point();

  // True if the window is clinging to the left side of the screen; false
  // if the window is clinging to the right side of the screen.
  private boolean mIsOnLeft;

  private final View mRootView;
  private final CameraPreview mPreview;
  private final Camera mCamera;

  private final PointF mInitialDown = new PointF();
  private final Point mInitialPosition = new Point();

  private final GestureDetectorCompat mGestureDetector;
  private final ScaleGestureDetector mScaleDetector;
  private float mScaleFactor = 1.f;

  private ValueAnimator mSnapAnimator;
  private boolean mAnimating;

  public FloatingWindow(Context context) {
    mContext = context;
    mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    mWindowManager.getDefaultDisplay().getSize(mDisplaySize);
    mWindowParams = createWindowParams(INITIAL_WIDTH, INITIAL_HEIGHT);
    mIsOnLeft = true;

    mGestureDetector = new GestureDetectorCompat(context, new GestureListener());
    mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

    // TODO: don't hardcode cameraId '0' here... figure this out later.
    mCamera = Camera.open(0);
    CameraPreview.setCameraDisplayOrientation(context, 0, mCamera);

    mPreview = new CameraPreview(context);
    mPreview.setCamera(mCamera);
    mPreview.setOnTouchListener(this);

    mRootView = mPreview;
  }

  public void show() {
    mWindowManager.addView(mRootView, mWindowParams);
  }

  public void hide() {
    mCamera.release();
    mWindowManager.removeView(mRootView);
  }

  private static WindowManager.LayoutParams createWindowParams(int width, int height) {
    WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    params.width = width;
    params.height = height;
    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    params.format = PixelFormat.TRANSLUCENT;
    params.gravity = Gravity.LEFT | Gravity.TOP;
    return params;
  }

  private void updateWindowPosition(int x, int y) {
    if (DEBUG) Log.i(TAG, "updateWindowPosition(" + x + ", " + y + ")");
    mWindowParams.x = x;
    mWindowParams.y = y;
    mWindowManager.updateViewLayout(mRootView, mWindowParams);
  }

  private void updateWindowSize(int width, int height) {
    if (DEBUG) Log.i(TAG, "updateWindowSize(" + width + ", " + height + ")");
    mWindowParams.width = width;
    mWindowParams.height = height;
    mWindowManager.updateViewLayout(mRootView, mWindowParams);
  }

  /** Called by the FloatingWindowService when a configuration change occurs. */
  public void onConfigurationChanged(Configuration newConfiguration) {
    // TODO: properly handle configuration changes (we probably will only have
    // to care about orientation changes).
    mWindowManager.getDefaultDisplay().getSize(mDisplaySize);
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    if (mAnimating) {
      return true;
    }

    // Unschedule any pending animations.
    mHandler.removeMessages(MSG_SNAP);

    mScaleDetector.onTouchEvent(event);
    mGestureDetector.onTouchEvent(event);

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
        mInitialPosition.set(mWindowParams.x, mWindowParams.y);
        mInitialDown.set(event.getRawX(), event.getRawY());
        break;
      }
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP: {
//         if (mInitialDown.equals(event.getRawX(), event.getRawY())) {
//         }



        int screenWidth = mDisplaySize.x;
        int windowWidth = mRootView.getWidth();
        int oldX = mWindowParams.x;

        if (oldX + windowWidth / 2 < screenWidth / 2) {
//          snap(oldX, - 2 * windowWidth / 3);
          mIsOnLeft = true;
        } else {
//          snap(oldX, screenWidth - windowWidth / 3);
          mIsOnLeft = false;
        }
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        int newX = mInitialPosition.x + (int) (event.getRawX() - mInitialDown.x);
        int newY = mInitialPosition.y + (int) (event.getRawY() - mInitialDown.y);
        updateWindowPosition(newX, newY);
        break;
      }
    }
    return true;
  }

  private void snap(final int fromX, final int toX) {
    if (DEBUG) Log.i(TAG, "snap(" + fromX + ", " + toX + ")");

    mSnapAnimator = ValueAnimator.ofFloat(0, 1);
    mSnapAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        mRootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mAnimating = true;
      }
      @Override
      public void onAnimationEnd(Animator animation) {
        mRootView.setLayerType(View.LAYER_TYPE_NONE, null);
        mAnimating = false;
      }
    });
    mSnapAnimator.addUpdateListener(new AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        int currX = fromX + (int) (animation.getAnimatedFraction() * (toX - fromX));
        updateWindowPosition(currX, mWindowParams.y);
      }
    });
    mSnapAnimator.start();
  }

  private class GestureListener extends SimpleOnGestureListener {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onDoubleTap(MotionEvent event) {
      // TODO: use the code below to launch the camera activity or take a
      // picture?

      // Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // Bundle options = ActivityOptions.makeScaleUpAnimation(mRootView, 0, 0,
      // mRootView.getWidth(), mRootView.getHeight()).toBundle();
      // mContext.startActivity(intent, options);
      // } else {
      // mContext.startActivity(intent);
      // }

      // Double tap closes the camera window for now. This is temporary... will
      // figure out the correct gesture to use later.
//      hide();

      return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
//      Toast.makeText(mContext, "touch", Toast.LENGTH_SHORT).show();
      int fromX = mWindowParams.x;
      int toX = mIsOnLeft ? 0 : mDisplaySize.x - mRootView.getWidth();
      //snap(fromX, toX);
      Message snapMsg = mHandler.obtainMessage(MSG_SNAP, toX, fromX);
      mHandler.sendMessageDelayed(snapMsg, SLEEP_DELAY_MILLIS);
      return true;
    }

      @Override
      public void onLongPress(MotionEvent e) {
          Toast.makeText(mContext, "longpress", Toast.LENGTH_SHORT).show();
          mPreview.capture(new Camera.PictureCallback() {

              // JPEG 사진파일 생성후 호출됨
              // 찍은 사진을 처리
              // PictureCallback 인터페이스 에 있는 onPictureTaken() 메서드
              // byte[] data - 사진 데이타
              public void onPictureTaken(byte[] data, Camera camera) {
                  try {

                      // 사진데이타를 비트맵 객체로 저장
                      Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                      // bitmap 이미지를 이용해 앨범에 저장
                      // 내용재공자를 통해서 앨범에 저장
                      String outUriStr = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), bitmap,"Captured Image","Captured Image using Camera.");

                      if (outUriStr == null) {
                          Log.d("SampleCapture", "Image insert failed.");
                          return;
                      } else {
                          Uri outUri = Uri.parse(outUriStr);
//                          mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,outUri));
                          Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                          shareIntent.setType("image/*");
                          shareIntent.putExtra(Intent.EXTRA_STREAM, outUri); // set uri
                          shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                          shareIntent.setPackage("com.instagram.android");
                          mContext.startActivity(shareIntent);
                          int fromX = mWindowParams.x;
                          int toX = mIsOnLeft ? 0 : mDisplaySize.x - mRootView.getWidth();
                          snap(fromX, toX);
                      }

                      Toast.makeText(mContext,"카메라로 찍은 사진을 앨범에 저장했습니다.",Toast.LENGTH_LONG).show();

                      // 다시 미리보기 화면 보여줌
                      camera.startPreview();
                  } catch (Exception e) {
                      Log.e("SampleCapture", "Failed to insert image.", e);
                  }
              }
          });
      }
  }

  private class ScaleListener extends SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();
        mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, MAX_SCALE_FACTOR));
        int newWidth = (int) (INITIAL_WIDTH * mScaleFactor);
        int newHeight = (int) (INITIAL_HEIGHT * mScaleFactor);
        updateWindowSize(newWidth, newHeight);
        return true;
    }
  }

}
