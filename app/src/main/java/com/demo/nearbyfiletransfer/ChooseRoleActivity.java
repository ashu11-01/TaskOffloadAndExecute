package com.demo.nearbyfiletransfer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.demo.nearbyfiletransfer.Utility.Permission;

public class ChooseRoleActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 21;
    ImageView imgExec,imgOffloader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

        initviews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
        if(!Permission.hasPermission(this,Permission.permissionList)){
            requestPermissions(Permission.permissionList,PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode!=PERMISSION_REQUEST_CODE){
            return;
        }
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
    }

    private void initviews() {
        imgExec = findViewById(R.id.img_Executer);
        imgOffloader = findViewById(R.id.img_Offloader);

    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.tv_exec:
            case R.id.img_Executer:
                goToExecuterScreen();
                break;
            case R.id.tv_off:
            case R.id.img_Offloader:
                goToOffloadderScreen();
                break;
        }
    }

    private void goToOffloadderScreen() {
          Intent offloader = new Intent(ChooseRoleActivity.this,OffloaderActivity.class);
          startActivity(offloader);
    }

    private void goToExecuterScreen() {
        Intent executer = new Intent(ChooseRoleActivity.this,ExecuterActivity.class);
        startActivity(executer);
    }
}
