package com.demo.nearbyfiletransfer.SelectionCriteria;

import android.content.Context;
import android.util.Log;

import com.demo.nearbyfiletransfer.ExecuterModel;
import java.util.Collections;
import java.util.List;

public class TopsisSelection {


    public static List<ExecuterModel> getTopsisBestExecuters(Context context, List<ExecuterModel> executerList){
        double[][] parameterMatrix = new double[executerList.size()+3][7];
        final float[] weightsArray = com.demo.nearbyfiletransfer.Utility.SharedPreference.getWeights(context);
        final int ROOT_SQUARED_SUM_INDEX = executerList.size();
        final int IDEAL_POSITIVE_INDEX = ROOT_SQUARED_SUM_INDEX +1;
        final int IDEAL_NEGATIVE_INDEX = IDEAL_POSITIVE_INDEX + 1;
        
        final int POSITIVE_IDEAL_DISTANCE_INDEX = 4;
        final int NEGATIVE_IDEAL_DISTANCE_INDEX = 5;
        final int SUM_IDEAL_DISTANCE_INDEX = 6;
        //initialize paramter matrix
        for(int i=0 ; i<executerList.size();i++){
            parameterMatrix[i][0] = Double.parseDouble(executerList.get(i).getBattery());
            parameterMatrix[i][1] = Double.parseDouble(executerList.get(i).getRAM());
            parameterMatrix[i][2] = Double.parseDouble(executerList.get(i).getCpu());
            parameterMatrix[i][3] = Double.parseDouble(executerList.get(i).getStorage());
        }
        //calculate rooted squared sum for each parameter for each alternative
        for(int j=0 ; j<4;j++){
            float sum=0;
            for(int i=0;i<executerList.size();i++){
                sum+=(parameterMatrix[i][j]* parameterMatrix[i][j]);
            }
            parameterMatrix[ROOT_SQUARED_SUM_INDEX][j] = Math.sqrt(sum);
        }
        //normalize the matrix
        for(int j=0 ; j<4;j++){
            parameterMatrix[IDEAL_POSITIVE_INDEX][j] = Double.MIN_VALUE;
            parameterMatrix[IDEAL_NEGATIVE_INDEX][j] = Double.MAX_VALUE;
            for(int i=0;i<executerList.size();i++){
                parameterMatrix[i][j] = parameterMatrix[i][j] / parameterMatrix[ROOT_SQUARED_SUM_INDEX][j];
                parameterMatrix[i][j] = parameterMatrix[i][j] * weightsArray[j];    //weight normalization
                //calculate ideal +ve and ideal -ve solution
                parameterMatrix[IDEAL_POSITIVE_INDEX][j] = Math.max(parameterMatrix[i][j], parameterMatrix[IDEAL_POSITIVE_INDEX][j]);
                parameterMatrix[IDEAL_NEGATIVE_INDEX][j] = Math.min(parameterMatrix[i][j], parameterMatrix[IDEAL_NEGATIVE_INDEX][j]);
            }
        }
        //calculate distance of each alternative from ideal +ve and ideal -ve solution
        for(int i=0;i<executerList.size();i++){
            double sum_positive=0.0,sum_negative=0.0;
            for(int j=0;j<4;j++){
                sum_positive += (Math.pow((parameterMatrix[i][j] - parameterMatrix[IDEAL_POSITIVE_INDEX][j]),2.0));
                sum_negative += (Math.pow((parameterMatrix[i][j] - parameterMatrix[IDEAL_NEGATIVE_INDEX][j]),2.0));
            }
            parameterMatrix[i][POSITIVE_IDEAL_DISTANCE_INDEX] = Math.sqrt(sum_positive);
            parameterMatrix[i][NEGATIVE_IDEAL_DISTANCE_INDEX] = Math.sqrt(sum_negative);
            executerList.get(i).setUtility(parameterMatrix[i][NEGATIVE_IDEAL_DISTANCE_INDEX] / (parameterMatrix[i][POSITIVE_IDEAL_DISTANCE_INDEX] + parameterMatrix[i][NEGATIVE_IDEAL_DISTANCE_INDEX]));
        }
        for(int i=0;i<executerList.size()+3;i++)
            for(int j=0;j<7;j++)
                Log.d("topsis",parameterMatrix[i][j]+"");
        Collections.sort(executerList,Collections.<ExecuterModel>reverseOrder());
        return executerList;
    }
}
