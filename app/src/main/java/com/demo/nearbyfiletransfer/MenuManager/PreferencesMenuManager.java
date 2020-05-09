package com.demo.nearbyfiletransfer.MenuManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.demo.nearbyfiletransfer.R;
import com.demo.nearbyfiletransfer.Utility.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesMenuManager {
    private Context context;
    private EditText etBattery,etRam,etCpu,etStorage;
    private Map<String,Float> weightMap = new HashMap<>();
    public PreferencesMenuManager(Context context){
        this.context= context;
    }
    
    public void setPreferenceWeights(){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        final AlertDialog weightsDialog = builder.create();

        //Alert dialog custom layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.preference_weights_dialog,null);
        weightsDialog.setView(view);
        initDialogViews(view);
        Button btnSubmit = view.findViewById(R.id.btn_wtSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeWeightsToSharedPreference(weightsDialog);
            }
        });
        
        weightsDialog.setCancelable(false);
        weightsDialog.create();
        weightsDialog.show();
    }

    private void writeWeightsToSharedPreference(AlertDialog weightsDialog) {
        if(!validateWeights()){
            Toast.makeText(context, "Please set proper weight values", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences preferences = new ContextWrapper(context).
                getSharedPreferences("WeightPreference",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        try {
            editor.putFloat(Constants.SharedPreferenceKeys.BATTERY_WEIGHT,weightMap.get("Battery"));
            editor.putFloat(Constants.SharedPreferenceKeys.RAM_WEIGHT,weightMap.get("RAM"));
            editor.putFloat(Constants.SharedPreferenceKeys.CPU_WEIGHT,weightMap.get("CPU"));
            editor.putFloat(Constants.SharedPreferenceKeys.STORAGE_WEIGHT,weightMap.get("Storage"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        editor.apply();
        Toast.makeText(context,"Weights set successfully",Toast.LENGTH_SHORT).show();
        weightsDialog.cancel();
    }

    private boolean validateWeights() {
        final EditText[] etId = {etBattery,etRam,etCpu,etStorage};
        final String[] label = {"Battery","RAM","CPU","Storage"};
        boolean isValid=true;
        for(int i=0;i<etId.length;i++){
            if(etId[i].getText().toString().isEmpty()){
                etId[i].setError("Field cannot be empty");
                isValid = false;
            }
            else {
                try {
                    float d = Float.parseFloat(etId[i].getText().toString());
                    weightMap.put(label[i], d);
                } catch (NumberFormatException nfe) {
                    etId[i].setError("Must be decimal value");
                    isValid = false;
                }
            }
        }

        if (isValid) {
            List<Float> weights = new ArrayList<>();
            for (String s : label){
                weights.add(weightMap.get(s));
            }
            float sum =0.0f;
            for(Float d : weights) sum+=d;
            if(Double.compare(1.0,sum)!=0)  {
                isValid = false;
                etId[0].setError("Weights must sum to 1");
            }
        }
        return isValid;
    }

    private void initDialogViews(View view) {
        etBattery=view.findViewById(R.id.et_battry_wt);
        etRam=view.findViewById(R.id.et_ram_wt);
        etCpu = view.findViewById(R.id.et_cpu_wt);
        etStorage=view.findViewById(R.id.et_storage_wt);

    }
    
}
