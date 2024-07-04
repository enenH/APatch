package me.bmax.apatch;

import static me.bmax.apatch.util.APatchCliKt.checkRoot;

import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import me.bmax.apatch.services.RootServices;

public class Native {
    public static IAPRootService ipc;

    public static int init(String card, String config, boolean isHide, boolean isSecure, boolean isAppTouch) {
        if (checkRoot()) {
            try {
                return ipc.init(card, config, isHide, isSecure, isAppTouch);
            } catch (RemoteException ignored) {
            }
        } else {
            return RootServices.native_init(card, config, isHide, isSecure, isAppTouch);
        }
        return -1;
    }

    public static void setTouch(float x, float y) {
        if (checkRoot()) {
            try {
                ipc.setTouch(x, y);
            } catch (RemoteException ignored) {
            }
        } else {
            RootServices.native_setTouch(x, y);
        }
    }


    public static float[] getWindowSize() {
        if (checkRoot()) {
            try {
                return ipc.getWindowSize();
            } catch (RemoteException ignored) {
            }
        } else {
            return RootServices.native_getWindowSize();
        }
        return new float[4];
    }

    public static void surfaceCreated(Surface surface, int width, int height) {
        if (checkRoot()) {
            try {
                ipc.surfaceCreated(surface, width, height);
            } catch (RemoteException ignored) {
            }
        } else {
            RootServices.native_setSurface(surface, width, height);
        }
    }

    public static void surfaceDestroyed() {
        if (checkRoot()) {
            try {
                ipc.surfaceDestroyed();
            } catch (RemoteException ignored) {
            }
        } else {
            RootServices.native_shutDown();
        }
    }
}
