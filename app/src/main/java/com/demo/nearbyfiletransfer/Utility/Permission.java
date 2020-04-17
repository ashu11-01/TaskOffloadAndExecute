package com.demo.nearbyfiletransfer.Utility;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class Permission  {
    private static final String TAG = "Permission";


    public static  final String[] permissionList ={Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,
                                                    Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static boolean hasPermission(Context context, String... permissions){
        for (String permission : permissions){
            if(ContextCompat.checkSelfPermission(context,permission)
                    != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

}
