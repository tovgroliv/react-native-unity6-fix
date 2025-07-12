package com.azesmwayreactnativeunity;

import android.app.Activity;
import android.content.res.Configuration;
import android.widget.FrameLayout;

import com.unity3d.player.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UPlayer {
    private static UnityPlayer unityPlayer;

    public UPlayer(final Activity activity, final ReactNativeUnity.UnityPlayerCallback callback) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super();
        Class<?> _player = null;

        try {
            _player = Class.forName("com.unity3d.player.UnityPlayerForActivityOrService");
        } catch (ClassNotFoundException e) {
            _player = Class.forName("com.unity3d.player.UnityPlayer");
        }

        Constructor<?> constructor = _player.getConstructors()[1];
        unityPlayer = (UnityPlayer) constructor.newInstance(activity, new IUnityPlayerLifecycleEvents() {
            @Override
            public void onUnityPlayerUnloaded() {
                callback.onUnload();
            }

            @Override
            public void onUnityPlayerQuitted() {
                callback.onQuit();
            }
        });
    }

    public static void UnitySendMessage(String gameObject, String methodName, String message) {
        UnityPlayer.UnitySendMessage(gameObject, methodName, message);
    }

    public void pause() {
        unityPlayer.pause();
    }

    public void windowFocusChanged(boolean b) {
        unityPlayer.windowFocusChanged(b);
    }

    public void resume() {
        unityPlayer.resume();
    }

    public void unload() {
        unityPlayer.unload();
    }

    public Object getParentPlayer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            // Unity 6+ use getFrameLayout() method
            android.view.View frame = this.requestFrame();
            return frame != null ? frame.getParent() : null;
        } catch (Exception e) {
            // Unity 6 previous version, call getParent directly on UnityPlayer
            try {
                Method getParent = unityPlayer.getClass().getMethod("getParent");
                return getParent.invoke(unityPlayer);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // If all fail, return null
                return null;
            }
        }
    }

    public void configurationChanged(Configuration newConfig) {
        unityPlayer.configurationChanged(newConfig);
    }

    public void destroy() {
        unityPlayer.destroy();
    }

    public void requestFocusPlayer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            // Unity 6+ use getFrameLayout() method
            android.view.View frame = this.requestFrame();
            if (frame != null) {
                frame.requestFocus();
            }
        } catch (Exception e) {
            // Unity 6 previous version, call requestFocus directly on UnityPlayer
            try {
                Method requestFocus = unityPlayer.getClass().getMethod("requestFocus");
                requestFocus.invoke(unityPlayer);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // If all fail, do nothing
            }
        }
    }

    public android.view.View requestFrame() {
        try {
            // Unity 6+ use getFrameLayout() method
            Method getFrameLayout = unityPlayer.getClass().getMethod("getFrameLayout");
            return (android.view.View) getFrameLayout.invoke(unityPlayer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Unity 6 previous version, use reflection to check if UnityPlayer is a subclass of View
            try {
                // Try to convert UnityPlayer to View, if fails will throw ClassCastException
                return (android.view.View) (Object) unityPlayer;
            } catch (ClassCastException ex) {
                // If conversion fails, return null
                return null;
            }
        }
    }

    public void setZ(float v) {
        try {
            // Get Unity's Frame view, set Z value on Frame
            android.view.View frame = this.requestFrame();
            if (frame != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                frame.setZ(v);
            }
        } catch (Exception e) {
            // If get Frame fails, try to set Z value directly on UnityPlayer
            try {
                Method setZ = unityPlayer.getClass().getMethod("setZ", float.class);
                setZ.invoke(unityPlayer, v);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // Ignore, some versions of Unity may not have this method
            }
        }
    }

    public Object getContextPlayer() {
        return unityPlayer.getContext();
    }
}