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
            // Unity 6+ 使用 getFrameLayout() 方法
            android.view.View frame = this.requestFrame();
            return frame != null ? frame.getParent() : null;
        } catch (Exception e) {
            // Unity 6 之前的版本，直接在 UnityPlayer 上调用 getParent
            try {
                Method getParent = unityPlayer.getClass().getMethod("getParent");
                return getParent.invoke(unityPlayer);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // 如果都失败了，返回 null
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
            // Unity 6+ 使用 getFrameLayout() 方法
            android.view.View frame = this.requestFrame();
            if (frame != null) {
                frame.requestFocus();
            }
        } catch (Exception e) {
            // Unity 6 之前的版本，直接在 UnityPlayer 上调用 requestFocus
            try {
                Method requestFocus = unityPlayer.getClass().getMethod("requestFocus");
                requestFocus.invoke(unityPlayer);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // 如果都失败了，就不做任何操作
            }
        }
    }

    public android.view.View requestFrame() {
        try {
            // Unity 6+ 使用 getFrameLayout() 方法
            Method getFrameLayout = unityPlayer.getClass().getMethod("getFrameLayout");
            return (android.view.View) getFrameLayout.invoke(unityPlayer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Unity 6 之前的版本，使用反射检查 UnityPlayer 是否是 View 的子类
            try {
                // 尝试将 UnityPlayer 转换为 View，如果失败会抛出 ClassCastException
                return (android.view.View) (Object) unityPlayer;
            } catch (ClassCastException ex) {
                // 如果转换失败，返回 null
                return null;
            }
        }
    }

    public void setZ(float v) {
        try {
            // 获取 Unity 的 Frame 视图，在 Frame 上设置 Z 值
            android.view.View frame = this.requestFrame();
            if (frame != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                frame.setZ(v);
            }
        } catch (Exception e) {
            // 如果获取 Frame 失败，尝试直接在 UnityPlayer 上设置 Z 值
            try {
                Method setZ = unityPlayer.getClass().getMethod("setZ", float.class);
                setZ.invoke(unityPlayer, v);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // 忽略，某些版本的 Unity 可能没有这个方法
            }
        }
    }

    public Object getContextPlayer() {
        return unityPlayer.getContext();
    }
}