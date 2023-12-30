package org.apache.cordova.overApps.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import org.apache.cordova.overApps.GeneralUtils.KeyDispatchLayout;
import org.apache.cordova.overApps.Services.ServiceParameters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OverAppsService extends Service {

    private static final String TAG = OverAppsService.class.getSimpleName();

    private WindowManager windowManager;
    private LayoutInflater inflater;

    private View overAppsHead, overAppsView;
    private ImageView imgClose;
    private WebView webView;

    private WindowManager.LayoutParams paramsHeadFloat, paramsHeadView, paramsKeyDispatch;

    private ServiceParameters serviceParameters;
    private GestureDetector gestureDetector;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String filePath = intent.getStringExtra("file_path");
        webView.loadUrl(filePath);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
        serviceParameters = new ServiceParameters(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        overAppsHead = inflater.inflate(R.layout.service_over_apps_head, null);
        overAppsView = inflater.inflate(R.layout.service_over_apps_view, null);

        imgClose = overAppsView.findViewById(R.id.imgClose);
        imgClose.setOnClickListener(view -> {
            stopSelf();
            try {
                if (overAppsView != null) windowManager.removeView(overAppsView);
                if (overAppsHead != null) windowManager.removeView(overAppsHead);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        webView = overAppsView.findViewById(R.id.webView);
        webViewSettings();

        int layoutParamType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        paramsHeadFloat = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsHeadFloat.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

        paramsHeadView = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsHeadView.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

        adjustWebViewGravity();

        boolean hasHead = serviceParameters.getBoolean("has_head", true);
        boolean enableHardwareBack = serviceParameters.getBoolean("enable_hardware_back", true);

        if (hasHead) {
            windowManager.addView(overAppsHead, paramsHeadFloat);
            showKeyDispatchVisibility(false);
        } else {
            windowManager.addView(overAppsView, paramsHeadView);
            showKeyDispatchVisibility(enableHardwareBack);
        }

        overAppsHead.setOnTouchListener((v, event) -> {
            if (event != null) {
                if (gestureDetector.onTouchEvent(event)) {
                    windowManager.removeView(overAppsHead);
                    overAppsHead = null;
                    windowManager.addView(overAppsView, paramsHeadView);
                    showKeyDispatchVisibility(enableHardwareBack);
                } else {
                    handleTouchEvents(event);
                }
            }
            return false;
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (overAppsView != null) {
                windowManager.removeView(overAppsView);
            }
            showKeyDispatchVisibility(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTouchEvents(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Handle ACTION_DOWN
                break;
            case MotionEvent.ACTION_MOVE:
                // Handle ACTION_MOVE
                break;
            case MotionEvent.ACTION_UP:
                // Handle ACTION_UP
                break;
        }
    }

    private void showKeyDispatchVisibility(boolean visible) {
        if (visible) {
            // Show the key dispatch layout
            windowManager.addView(rlKeyDispatch, paramsKeyDispatch);
            Log.d(TAG, "Key DISPATCH -- ADDED");
        } else {
            // Hide the key dispatch layout
            try {
                windowManager.removeView(rlKeyDispatch);
                Log.d(TAG, "Key DISPATCH -- REMOVED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void webViewSettings() {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.addJavascriptInterface(new WebAppInterface(this), "OverApps");
        WebSettings webSettings = webView.getSettings();
        // Configure other WebView settings here
    }

    private void adjustWebViewGravity() {
        String verticalPosition = serviceParameters.getString("vertical_position");
        String horizontalPosition = serviceParameters.getString("horizontal_position");
        String position = verticalPosition + "_" + horizontalPosition;

        switch (position) {
            case "top_right":
                paramsHeadView.gravity = Gravity.TOP | Gravity.RIGHT;
                break;
            case "top_center":
                paramsHeadView.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                break;
            case "top_left":
                paramsHeadView.gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case "center_right":
                paramsHeadView.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
                break;
            case "center_center":
                paramsHeadView.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
                break;
            case "center_left":
                paramsHeadView.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
                break;
            case "bottom_right":
                paramsHeadView.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case "bottom_center":
                paramsHeadView.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                break;
            case "bottom_left":
                paramsHeadView.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            default:
                paramsHeadView.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        }
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    public class WebAppInterface {
        Context mContext;

        public WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void closeWebView() {
            stopSelf();
            try {
                if (overAppsView != null) windowManager.removeView(overAppsView);
                if (overAppsHead != null) windowManager.removeView(overAppsHead);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void openApp() {
            // Implement openApp functionality as needed
        }
    }
}
