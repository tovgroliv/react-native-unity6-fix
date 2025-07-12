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
            // Delay execution to ensure all detach operations are completed
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        addUnityViewToBackground();
                    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                        // If failed, do not throw exception, but log
                        android.util.Log.e("ReactNativeUnity", "Failed to add Unity view to background", e);
                    }
                }
            });
        } catch (Exception e) {
            // If post fails, try to call directly
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