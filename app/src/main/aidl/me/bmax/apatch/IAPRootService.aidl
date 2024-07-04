// IAPRootService.aidl
package me.bmax.apatch;

import android.content.pm.PackageInfo;
import rikka.parcelablelist.ParcelableListSlice;

interface IAPRootService {
    ParcelableListSlice<PackageInfo> getPackages(int flags);

    int init(String card,String config, boolean isHide, boolean isSecure, boolean isAppTouch);

    void setTouch(float x,float y);

    float[] getWindowSize();

    void surfaceCreated(in Surface surface,int screenWidth,int screenHeight);

    void surfaceDestroyed();
}