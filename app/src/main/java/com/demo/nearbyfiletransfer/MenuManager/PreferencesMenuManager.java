package com.demo.nearbyfiletransfer.MenuManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.demo.nearbyfiletransfer.R;
import com.demo.nearbyfiletransfer.Utility.Constants;

public class PreferencesMenuManager {
    private Context context;
    private int wSystem,wRating;
    private TextView tvSystemWeight,tvRatingWeight;

    public PreferencesMenuManager(Context context){
        this.context= context;
    }
    public void setPreferenceWeights(){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences preferences = new ContextWrapper(context).
                        getSharedPreferences("WeightPreference",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat(Constants.SharedPreferenceKeys.SYSTEM_WEIGHT,wSystem*0.01f);
                editor.putFloat(Constants.SharedPreferenceKeys.RATING_WEIGHT,wRating*0.01f);
                editor.apply();
            }
        });
        AlertDialog weightsDialog = builder.create();

        //Alert dialog custom layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.preference_weights_dialog,null);
        weightsDialog.setView(view);
        SeekBar seekBar = view.findViewById(R.id.weights_seek_bar);
        tvSystemWeight=view.findViewById(R.id.tv_system_pref_value);
        tvRatingWeight = view.findViewById(R.id.tv_rating_pref_value);
        showWeights(seekBar.getProgress());
        SeekBarListener seekBarListener = new SeekBarListener();
        seekBar.setOnSeekBarChangeListener(seekBarListener);

        weightsDialog.setCancelable(false);
        weightsDialog.create();
        weightsDialog.show();
    }

    private void showWeights(int value){
        wSystem = value; wRating = 100-value;
        tvSystemWeight.setText(String.valueOf(value));
        tvRatingWeight.setText(String.valueOf(100-value));
    }


    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
           if(b){
              showWeights(i);
           }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
