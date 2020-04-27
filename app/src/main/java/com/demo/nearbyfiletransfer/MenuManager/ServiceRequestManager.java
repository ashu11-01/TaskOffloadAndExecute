package com.demo.nearbyfiletransfer.MenuManager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.demo.nearbyfiletransfer.Utility.Constants;

public class ServiceRequestManager {

    public static void setServiceRequest(Context context){
        SharedPreferences preferences = new ContextWrapper(context).getSharedPreferences("ServieRequest",Context.MODE_PRIVATE);
        SharedPreferences.Editor  editor = preferences.edit();
        editor.putString(Constants.SharedPreferenceKeys.SERVICE_REQUEST,"Python Script Execution");
        editor.apply();
    }
}
