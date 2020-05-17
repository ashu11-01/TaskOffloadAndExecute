package com.demo.nearbyfiletransfer.SelectionCriteria;

import android.content.Context;

import com.demo.nearbyfiletransfer.ExecuterModel;
import com.demo.nearbyfiletransfer.Utility.SharedPreference;

import java.util.Collections;
import java.util.List;

public class WeightedSumSelection {

    static float[] weightsArray = new float[4];
    static float[][] parmeterMatrix;


    public static List<ExecuterModel> weightedSumBestExecuters(List<ExecuterModel> executerList, Context context){
        if(executerList==null)
            throw new NullPointerException();
        else if(executerList.size()<1)
            throw new IllegalArgumentException();
        weightsArray=SharedPreference.getWeights(context);
        for(ExecuterModel m : executerList){
            double weightedSum = weightsArray[0] * Integer.parseInt(m.getBattery()) +
                                weightsArray[1] * Double.parseDouble(m.getRAM()) +
                                weightsArray[2] * Float.parseFloat(m.getCpu()) +
                                weightsArray[3] * Long.parseLong(m.getStorage())/1024.0;
            m.setUtility(weightedSum);
        }
        Collections.sort(executerList,Collections.<ExecuterModel>reverseOrder());
        return executerList;
    }
}
