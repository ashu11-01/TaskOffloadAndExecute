package com.demo.nearbyfiletransfer.Logger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.ACTIVITY_SERVICE;

public class InstantSystemParameters {
    
    private Context context;

    public InstantSystemParameters(Context context) {
        this.context = context;
    }

    private String getCurrentTime()
    {
        Calendar time=Calendar.getInstance();
        String day,month,year,hour24,minute,second;
        day=String.valueOf(time.get(Calendar.DAY_OF_MONTH));
        month=String.valueOf(time.get(Calendar.MONTH)+1);
        year=String.valueOf(time.get(Calendar.YEAR));
        hour24=String.valueOf(time.get(Calendar.HOUR_OF_DAY));
        minute=String.valueOf(time.get(Calendar.MINUTE));
        second=String.valueOf(time.get(Calendar.SECOND));
        String DateSeparater="-",TimeSeparater=":";
        String dateAndTime=year+DateSeparater+month+DateSeparater+day+" "+hour24+TimeSeparater+minute+TimeSeparater+second;
        return dateAndTime;
    }

    private String getBatteryLevel()
    {
        IntentFilter ifilter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery=context.registerReceiver(null,ifilter);

        int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        float batteryP=level/(float)scale;
        return String.valueOf((int)(batteryP*100));
    }

    private String getCurrentCPUFreqMHz()
    {
        String currfreq="-1";
        try {

            RandomAccessFile reader = new RandomAccessFile( "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r" );
            float temp=Float.parseFloat(reader.readLine())/1000;        //converting KHz value to MHz
            currfreq=String.valueOf(temp);

        } catch ( IOException ex ) {
            ex.printStackTrace();
        }

        return currfreq;
    }

    private String getCurrentRamUsage()
    {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager=(ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double avail=mi.availMem;
        double total=mi.totalMem;
        double usage=total-avail;
        double per=usage/(double)total * 100.0;
        DecimalFormat df=new DecimalFormat(".####");
        String res=df.format(per);
        return res;
    }

    private String getStorageInfo()
    {
        StatFs stat = new StatFs((Environment.getExternalStorageDirectory().getPath()));
        long bytesAvailable=stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long availableInKB=bytesAvailable /(1024);
        return String.valueOf(availableInKB);
    }

    public Map<String,String> getInstantParameters(){
        Map<String,String> parameterMap = new HashMap<>();
        parameterMap.put("Timestamp",getCurrentTime());
        parameterMap.put("Battery",getBatteryLevel());  //current available battery
        parameterMap.put("RAM",getCurrentRamUsage());   //current ram usage
        parameterMap.put("CPUFrequency",getCurrentCPUFreqMHz()); //current CPU Frequency MHz
        parameterMap.put("Storage",getStorageInfo());  //availabel storage in KB
        Toast.makeText(context,parameterMap.toString(),Toast.LENGTH_LONG).show();
        Log.d("parameter",parameterMap.toString());
        return parameterMap;
    }
}
