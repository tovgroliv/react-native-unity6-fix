package com.azesmwayreactnativeunity;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.ViewGroup;
import android.view.WindowManager;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import java.lang.reflect.InvocationTargetException;

public class ReactNativeUnity {
    private static UPlayer unityPlayer;
    public static boolean _isUnityReady;
    public static boolean _isUnityPaused;
    public static boolean _fullScreen;

    public static UPlayer getPlayer() {
        if (!_isUnityReady) {
            return null;
        }
        return unityPlayer;
    }

    public static boolean isUnityReady() {
        return _isUnityReady;
    }

    public static boolean isUnityPaused() {
        return _isUnityPaused;
    }

    public static void createPlayer(final Activity activity, final UnityPlayerCallback callback) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (unityPlayer != null) {
            callback.onReady();

            return;
        }

        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.getWindow().setFormat(PixelFormat.RGBA_8888);
                    int flag = activity.getWindow().getAttributes().flags;
                    boolean fullScreen = false;
                    if ((flag & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
                        fullScreen = true;
                    }

                    try {
                        unityPlayer = new UPlayer(activity, callback);
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {}

                    try {
                        // wait a moment. fix unity cannot start when startup.
                        Thread.sleep(1000);
                    } catch (Exception e) {}

                    // start unity
                    try {
                        addUnityViewToBackground();
                    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {}

                    unityPlayer.windowFocusChanged(true);

                    try {
                        unityPlayer.requestFocusPlayer();
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {}

                    unityPlayer.resume();

                    if (!fullScreen) {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }

                    _isUnityReady = true;

                    try {
                        callback.onReady();
                    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {}
                }
            });
        }
    }

    public static void pause() {
        if (unityPlayer != null) {
            unityPlayer.pause();
            _isUnityPaused = true;
        }
    }

    public static void resume() {
        if (unityPlayer != null) {
            unityPlayer.resume();
            _isUnityPaused = false;
        }
    }

    public static void unload() {
        if (unityPlayer != null) {
            unityPlayer.unload();
            _isUnityPaused = false;
        }
    }

    public static void addUnityViewToBackground() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (unityPlayer == null) {
            return;
        }

        // Get Unity's Frame view
        android.view.View unityFrame = unityPlayer.requestFrame();
        if (unityFrame == null) {
            return;
        }

        // Multiple checks and remove parent view to ensure removal operation is fully effective
        for (int i = 0; i < 3; i++) {
            android.view.ViewParent parent = unityFrame.getParent();
            if (parent != null) {
                // NOTE: If we're being detached as part of the transition, make sure
                // to explicitly finish the transition first, as it might still keep
                // the view's parent around despite calling `removeView()` here. This
                // prevents a crash on an `addContentView()` later on.
                // Otherwise, if there's no transition, it's a no-op.
                // See https://stackoverflow.com/a/58247331
                ViewGroup parentGroup = (ViewGroup) parent;
                parentGroup.endViewTransition(unityFrame);
                parentGroup.removeView(unityFrame);
                
                // Check again if removal is successful
                if (unityFrame.getParent() == null) {
                    break;
                }
            } else {
                break;
            }
        }

        // Last check to ensure no parent view
        if (unityFrame.getParent() != null) {
            // If there's still a parent, force remove
            try {
                ViewGroup parentGroup = (ViewGroup) unityFrame.getParent();
                parentGroup.removeView(unityFrame);
            } catch (Exception e) {
                // If removal fails, do not continue
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            unityPlayer.setZ(-1f);
        }

        final Activity activity = ((Activity) unityPlayer.getContextPlayer());
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(1, 1);
        
        // Last check before add
        if (unityFrame.getParent() == null) {
            activity.addContentView(unityFrame, layoutParams);
        }
    }

    public static void addUnityViewToGroup(ViewGroup group) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (unityPlayer == null) {
            return;
        }

        // Get Unity's Frame view
        android.view.View unityFrame = unityPlayer.requestFrame();
        if (unityFrame == null) {
            return;
        }

        // Multiple checks and remove parent view to ensure removal operation is fully effective
        for (int i = 0; i < 3; i++) {
            android.view.ViewParent parent = unityFrame.getParent();
            if (parent != null) {
                ViewGroup parentGroup = (ViewGroup) parent;
                parentGroup.removeView(unityFrame);
                
                // Check again if removal is successful
                if (unityFrame.getParent() == null) {
                    break;
                }
            } else {
                break;
            }
        }

        // Last check to ensure no parent view
        if (unityFrame.getParent() != null) {
            // If there's still a parent, force remove
            try {
                ViewGroup parentGroup = (ViewGroup) unityFrame.getParent();
                parentGroup.removeView(unityFrame);
            } catch (Exception e) {
                // If removal fails, do not continue
                return;
            }
        }

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        
        // Last check before add
        if (unityFrame.getParent() == null) {
            group.addView(unityFrame, 0, layoutParams);
            unityPlayer.windowFocusChanged(true);
            unityPlayer.requestFocusPlayer();
            unityPlayer.resume();
        }
    }

    public interface UnityPlayerCallback {
        void onReady() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

        void onUnload();

        void onQuit();
    }
}