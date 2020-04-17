package com.demo.nearbyfiletransfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.demo.nearbyfiletransfer.NearbyConnections.FileTransfer;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements FileTransfer.UpdateStatus {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_REQUEST_PERMISSION = 111;
    TextView imageSize,result,tvCodename;
    Button chooseImage, btnAdvertise,btnDiscover;
    ImageView image;
    Uri imageUri;
    ConnectionsClient client;
    FileTransfer fileTransfer;
    Payload fileTosend;
    public  static String codename = com.google.location.nearby.apps.rockpaperscissors.CodenameGenerator.generate();
    private final int CHOOSE_IMAGE = 100;

    private static  final String[] permissionList ={"Manifest.permission.BLUETOOTH",
            "Manifest.permission.BLUETOOTH_ADMIN",
            "Manifest.permission.ACCESS_WIFI_STATE" ,
            "Manifest.permission.CHANGE_WIFI_STATE",
            "Manifest.permission.ACCESS_COARSE_LOCATION"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //inintviews
        initviews();
        client = Nearby.getConnectionsClient(getApplicationContext());
        fileTransfer = new FileTransfer(MainActivity.this,client);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();
        if(!hasPermissions(permissionList)){
            requestPermissions(permissionList,REQUEST_CODE_REQUEST_PERMISSION);
        }

    }

    private boolean hasPermissions(String[] permissionList) {
        for (String permission : permissionList){
            if(ContextCompat.checkSelfPermission(getApplicationContext(),permission)
                    != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode!=REQUEST_CODE_REQUEST_PERMISSION)
            return;

        for(int grantResult : grantResults){
            if(grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Missing permission", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }


    private void initviews() {
       // imageSize=(TextView)findViewById(R.id.tvImageSize);
        tvCodename=(TextView)findViewById(R.id.tvCodeName);
        tvCodename.setText(codename);
        result=(TextView)findViewById(R.id.tvResult);
        //result.setText(fileTransfer.rsuper.recreate();eciever);
        //chooseImage=(Button)findViewById(R.id.btnChooseImage);
        btnAdvertise=(Button)findViewById(R.id.btnAdvertise);
        btnDiscover=(Button)findViewById(R.id.btnDiscover);
        image=(ImageView)findViewById(R.id.ivImage);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent,CHOOSE_IMAGE);
            }
        });
    }

    public void advertise(View view)
    {
        fileTransfer.connectionsClient.startAdvertising(codename,
                getPackageName(),
                fileTransfer.connectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_CLUSTER))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this,"Advertising",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Advertising failed",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"start advertising: "+e.getMessage());
            }
        });
        btnAdvertise.setEnabled(false);
        btnDiscover.setEnabled(false);
    }

    public void discover(View view)
    {

        fileTransfer.connectionsClient.startDiscovery(getPackageName()
                ,fileTransfer.endpointDiscoveryCallback
                ,new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this,"Discovering",Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Discovery failed",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"start discovery"+e.getMessage());
            }
        });

        btnAdvertise.setEnabled(false);
        btnDiscover.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==CHOOSE_IMAGE && resultCode==RESULT_OK && data!=null)
        {
            imageUri=data.getData();
            Glide.with(getApplicationContext()).load(imageUri).into(image);
           // imageSize.setText(imageUri.getPath());
            try{
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(imageUri,"r");
                fileTosend=Payload.fromFile(pfd);
            }catch (FileNotFoundException e){
                e.getMessage();
            }
        }
    }

    public void sendImage(View view)
    {
        fileTransfer.connectionsClient.sendPayload(fileTransfer.connectedToEndpointId,fileTosend);
    }

    @Override
    public void setStatusText(String s) {
        //result.setText("");
        result.setText("Status: "+s);
    }
}
