package com.demo.nearbyfiletransfer.SelectionCriteria;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.demo.nearbyfiletransfer.ExecuterModel;
import com.demo.nearbyfiletransfer.Utility.Constants;

import java.util.Collections;
import java.util.List;

public class WeightedSumSelection {

    static float[] weightsArray = new float[4];
    static float[][] parmeterMatrix;
    private static void getWeights(Context context) {
        SharedPreferences preferences = new ContextWrapper(context).
                getSharedPreferences("WeightPreference", Context.MODE_PRIVATE);
        weightsArray[0]= preferences.getFloat(Constants.SharedPreferenceKeys.BATTERY_WEIGHT,0.25f);
        weightsArray[1]= preferences.getFloat(Constants.SharedPreferenceKeys.RAM_WEIGHT,0.25f);
        weightsArray[2]= preferences.getFloat(Constants.SharedPreferenceKeys.CPU_WEIGHT,0.25f);
        weightsArray[3]= preferences.getFloat(Constants.SharedPreferenceKeys.STORAGE_WEIGHT,0.25f);
    }

    public static List<ExecuterModel> weightedSumBestExecuters(List<ExecuterModel> executerList, Context context){
        if(executerList==null)
            throw new NullPointerException();
        else if(executerList.size()<1)
            throw new IllegalArgumentException();
        getWeights(context);
        for(ExecuterModel m : executerList){
            double weightedSum = weightsArray[0] * Integer.parseInt(m.getBattery()) +
                                weightsArray[1] + Double.parseDouble(m.getRAM()) +
                                weightsArray[2] + Float.parseFloat(m.getCpu()) +
                                weightsArray[3] + Long.parseLong(m.getStorage())/1024.0;
            m.setUtility(weightedSum);
        }
        Collections.sort(executerList,Collections.<ExecuterModel>reverseOrder());
        return executerList;
    }
}
