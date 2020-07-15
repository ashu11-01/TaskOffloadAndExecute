package com.demo.nearbyfiletransfer.Utility;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

public class SharedPreference {

    public static float[] getWeights(Context context) {
        final float[] weightsArray = new float[4];
        SharedPreferences preferences = new ContextWrapper(context).
                getSharedPreferences("WeightPreference", Context.MODE_PRIVATE);
        weightsArray[0]= preferences.getFloat(Constants.SharedPreferenceKeys.BATTERY_WEIGHT,0.25f);
        weightsArray[1]= preferences.getFloat(Constants.SharedPreferenceKeys.RAM_WEIGHT,0.25f);
        weightsArray[2]= preferences.getFloat(Constants.SharedPreferenceKeys.CPU_WEIGHT,0.25f);
        weightsArray[3]= preferences.getFloat(Constants.SharedPreferenceKeys.STORAGE_WEIGHT,0.25f);
        return weightsArray;
    }
}
