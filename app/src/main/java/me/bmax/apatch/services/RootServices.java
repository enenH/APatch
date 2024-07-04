package me.bmax.apatch.services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.Surface;
import android.system.Os;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.bmax.apatch.IAPRootService;
import rikka.parcelablelist.ParcelableListSlice;

public class RootServices extends RootService {
    private static final String TAG = "RootServices";

    static {
        // Only load the library when this class is loaded in a root process.
        // The classloader will load this class (and call this static block) in the non-root
        // process because we accessed it when constructing the Intent to send.
        // Add this check so we don't unnecessarily load native code that'll never be used.
        if (Os.getuid() == 0)
            System.loadLibrary("entry");
    }

    public static native int native_init(String card, String config, boolean isHide,
                                         boolean isSecure, boolean isAppTouch);

    public static native float[] native_getWindowSize();

    public static native void native_setSurface(Surface obj, int width, int height);

    public static native void native_surfaceChanged(int rotation);

    public static native void native_shutDown();

    public static native void native_setTouch(float x, float y);


    class Stub extends IAPRootService.Stub {
        @Override
        public ParcelableListSlice<PackageInfo> getPackages(int flags) {
            List<PackageInfo> list = getInstalledPackagesAll(flags);
            Log.i(TAG, "getPackages: " + list.size());
            return new ParcelableListSlice<>(list);
        }

        @Override
        public int init(String card, String config, boolean isHide, boolean isSecure, boolean isAppTouch) throws RemoteException {
            return native_init(card, config, isHide, isSecure, isAppTouch);
        }

        @Override
        public void setTouch(float x, float y) throws RemoteException {
            native_setTouch(x, y);
        }

        @Override
        public float[] getWindowSize() throws RemoteException {
            return native_getWindowSize();
        }

        @Override
        public void surfaceCreated(Surface surface, int width, int height) throws RemoteException {
            native_setSurface(surface, width, height);
        }

        @Override
        public void surfaceDestroyed() throws RemoteException {
            native_shutDown();
        }

    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new Stub();
    }

    List<Integer> getUserIds() {
        List<Integer> result = new ArrayList<>();
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        List<UserHandle> userProfiles = um.getUserProfiles();
        for (UserHandle userProfile : userProfiles) {
            int userId = userProfile.hashCode();
            result.add(userProfile.hashCode());
        }
        return result;
    }

    ArrayList<PackageInfo> getInstalledPackagesAll(int flags) {
        ArrayList<PackageInfo> packages = new ArrayList<>();
        for (Integer userId : getUserIds()) {
            Log.i(TAG, "getInstalledPackagesAll: " + userId);
            packages.addAll(getInstalledPackagesAsUser(flags, userId));
        }
        return packages;
    }

    List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        try {
            PackageManager pm = getPackageManager();
            Method getInstalledPackagesAsUser = pm.getClass().getDeclaredMethod("getInstalledPackagesAsUser", int.class, int.class);
            return (List<PackageInfo>) getInstalledPackagesAsUser.invoke(pm, flags, userId);
        } catch (Throwable e) {
            Log.e(TAG, "err", e);
        }

        return new ArrayList<>();
    }
}