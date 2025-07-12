package com.azesmwayreactnativeunity;

import static com.azesmwayreactnativeunity.ReactNativeUnity.*;

import android.content.Context;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.widget.FrameLayout;

import java.lang.reflect.InvocationTargetException;

@SuppressLint("ViewConstructor")
public class ReactNativeUnityView extends FrameLayout {
  private UPlayer view;
  public boolean keepPlayerMounted = false;

  public ReactNativeUnityView(Context context) {
    super(context);
  }

  public void setUnityPlayer(UPlayer player) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    this.view = player;
    addUnityViewToGroup(this);
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    super.onWindowFocusChanged(hasWindowFocus);

    if (view == null) {
      return;
    }

    view.windowFocusChanged(hasWindowFocus);

    if (!keepPlayerMounted || !_isUnityReady) {
      return;
    }

    // pause Unity on blur, resume on focus
    if (hasWindowFocus && _isUnityPaused) {
      // view.requestFocus();
      view.resume();
    } else if (!hasWindowFocus && !_isUnityPaused) {
      view.pause();
    }
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    if (view != null) {
      view.configurationChanged(newConfig);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (!this.keepPlayerMounted) {
        try {
            // 延迟执行，确保所有的 detach 操作都完成
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        addUnityViewToBackground();
                    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                        // 如果失败，不抛出异常，而是记录日志
                        android.util.Log.e("ReactNativeUnity", "Failed to add Unity view to background", e);
                    }
                }
            });
        } catch (Exception e) {
            // 如果 post 失败，直接尝试调用
            try {
                addUnityViewToBackground();
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
                android.util.Log.e("ReactNativeUnity", "Failed to add Unity view to background", ex);
            }
        }
    }

    super.onDetachedFromWindow();
  }
}