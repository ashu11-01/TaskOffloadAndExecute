 package com.demo.nearbyfiletransfer;

 import android.animation.Animator;
 import android.content.ClipData;
 import android.content.ClipboardManager;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.ParcelFileDescriptor;
 import android.util.Log;
 import android.view.View;
 import android.widget.Button;
 import android.widget.TextView;
 import android.widget.Toast;

 import androidx.annotation.NonNull;
 import androidx.annotation.Nullable;
 import androidx.annotation.RequiresApi;
 import androidx.appcompat.app.AlertDialog;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.collection.ArrayMap;

 import com.demo.nearbyfiletransfer.BrokerUtility.AdvertiserRating;
 import com.demo.nearbyfiletransfer.Logger.InstantSystemParameters;
 import com.google.android.gms.nearby.Nearby;
 import com.google.android.gms.nearby.connection.AdvertisingOptions;
 import com.google.android.gms.nearby.connection.ConnectionInfo;
 import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
 import com.google.android.gms.nearby.connection.ConnectionResolution;
 import com.google.android.gms.nearby.connection.ConnectionsClient;
 import com.google.android.gms.nearby.connection.Payload;
 import com.google.android.gms.nearby.connection.PayloadCallback;
 import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
 import com.google.android.gms.nearby.connection.Strategy;
 import com.google.android.gms.tasks.OnFailureListener;
 import com.google.android.gms.tasks.OnSuccessListener;

 import java.io.File;
 import java.io.FileNotFoundException;
 import java.nio.charset.StandardCharsets;
 import java.util.Map;

 public class ExecuterActivity extends AppCompatActivity {
    private static final String TAG = "ExecuterActivity";
     private static final int REQUEST_RESULT_CHOOSE = 13 ;
     TextView tvCodename,status;
    Button btnStartAdvertise, btnStopAdvertise;

    //executeraction panel
     View executeractionPanel;
    Button btnExecute,btnSendBack;
    TextView tvOffloader,tvCodeFile,tvInputFile;
    String codename;
     AdvertisingOptions options;
     //nearby client and callbacks
     ConnectionsClient client;
     ConnectionLifecycleCallback connectionLifecycleCallback;
     Map<Long,Payload> incomingPayloads = new ArrayMap<>();
     Map<Long,String> payloadFilenameMap = new ArrayMap<>();
    private boolean isAdvertising;
    ExecuterModel cnnectedOffloader = new ExecuterModel();
    File result,recievedFile;
    int opCode;
    static int count=0,suc=1;
     @RequiresApi(api = Build.VERSION_CODES.M)
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_executer);

        initViews();

        codename = com.google.location.nearby.apps.rockpaperscissors.CodenameGenerator.generate();
        tvCodename.setText("Codename: "+codename);
        client = Nearby.getConnectionsClient(ExecuterActivity.this);
        options = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();

        connectionLifecycleCallback = new ConnectionLifecycleCallback() {

            @Override
            public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                if(connectionInfo.isIncomingConnection()){
                    AlertDialog.Builder confirmConnection = new AlertDialog.Builder(ExecuterActivity.this);
                    confirmConnection.setTitle("New Incoming Connection Request");
                    confirmConnection.setMessage("From: "+connectionInfo.getEndpointName()+"\n"+"Authentication Code: "+connectionInfo.getAuthenticationToken());
                    confirmConnection.setCancelable(false);
                    MyDialogueClickListener clickListener = new MyDialogueClickListener(s,connectionInfo);
                    confirmConnection.setPositiveButton("Accept", clickListener);
                    confirmConnection.setNegativeButton("Reject", clickListener);
                    confirmConnection.create();
                    confirmConnection.show();
                }
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                if(connectionResolution.getStatus().isSuccess()){
                    Toast.makeText(ExecuterActivity.this,"Connected successfully to: "+s,Toast.LENGTH_SHORT).show();
                    cnnectedOffloader.setEndpointId(s);
                    goToAction();
                }
                else if(connectionResolution.getStatus().isCanceled()){
                    Toast.makeText(ExecuterActivity.this,s+" rejected your connection request",Toast.LENGTH_SHORT).show();
                }
                else if(connectionResolution.getStatus().isInterrupted()){
                    Toast.makeText(ExecuterActivity.this,"interrupted",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDisconnected(String s) {

            }
        };
    }

     private String getAdvertisingMessage(){
         StringBuilder message = new StringBuilder();
         String sep ="/";
         message.append(codename);   message.append(sep);
         message.append(AdvertiserRating.getRating()); message.append(sep);
         message.append("Python Script Execution"); message.append(sep);
         InstantSystemParameters instantSystemParameters = new InstantSystemParameters(getApplicationContext());
         Map<String,String> parameters = instantSystemParameters.getInstantParameters();
         message.append(parameters.get("Timestamp")); message.append(sep);
         message.append(parameters.get("Battery")); message.append(sep);
         message.append(parameters.get("RAM")); message.append(sep);
         message.append(parameters.get("CPUFrequency")); message.append(sep);
         message.append(parameters.get("Storage"));  message.append(sep);
         Log.d(TAG,"get msg: "+parameters.toString()+"\n"+message);
         return new String(message);
     }

     @RequiresApi(api = Build.VERSION_CODES.M)
     private void initViews() {
         executeractionPanel = findViewById(R.id.executeraction);
         executeractionPanel.setVisibility(View.GONE);
         tvCodename = (TextView)findViewById(R.id.tv_codename);
         status = (TextView)findViewById(R.id.tv_status);
         btnStartAdvertise = (Button)findViewById(R.id.btnAdvertise);
         btnStopAdvertise = (Button)findViewById(R.id.btn_stopAdvertise);
         /*boolean perm = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED;
         String[] per= new String[1];
         per[0]="android.permission.WRITE_EXTERNAL_STORAGE";
         if(!perm){
             requestPermissions(per,11);
         }*/
     }

     private void initExecuterActionViews(){
         tvCodeFile = findViewById(R.id.tv_code_filename2);
         tvInputFile = findViewById(R.id.tv_input_filename2);
         tvOffloader = findViewById(R.id.tv_offloader);
         tvOffloader.setText(cnnectedOffloader.getCodename());
         btnExecute=findViewById(R.id.btn_execute);
         btnSendBack = findViewById(R.id.btn_send);
     }

     public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_startAdvertise:
                if(isAdvertising){
                    Toast.makeText(ExecuterActivity.this,"Already Advertising",Toast.LENGTH_SHORT).show();
                    return;
                }
                client.startAdvertising(getAdvertisingMessage(),getApplicationContext().getPackageName(),
                        connectionLifecycleCallback,options).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ExecuterActivity.this,"Advertising",Toast.LENGTH_SHORT).show();
                        isAdvertising = true;
                        setStatusText("Advertising");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ExecuterActivity.this,"Advertising failed",Toast.LENGTH_LONG).show();
                        isAdvertising = false;
                        setStatusText("Advertising Failed");
                    }
                });
                break;

            case R.id.btn_stopAdvertise:
                    stopAdvertise();

                break;

            case R.id.btn_execute:
                String codeFile="",inputFile="";
                for(Map.Entry<Long,String> entry : payloadFilenameMap.entrySet()){
                    String filename = entry.getValue();
                    if(filename.charAt(filename.length()-1)=='y')
                        codeFile=filename;
                    else if(filename.charAt(filename.length()-1)=='t')
                        inputFile = filename;
                }
                String command = "python " + codeFile + " < " + inputFile+" > output.txt";
                ClipboardManager manager =(ClipboardManager) getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("command",command);
                manager.setPrimaryClip(clipData);
                Toast.makeText(this,"Command copied. Open Termux and paste command to execute",Toast.LENGTH_SHORT).show();
                break;

            case R.id.btn_send:
                Intent filechooser = new Intent(Intent.ACTION_GET_CONTENT);
                filechooser.setType("file/*");
                startActivityForResult(filechooser,REQUEST_RESULT_CHOOSE);
                break;
        }
     }

     protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         switch (requestCode){
             case REQUEST_RESULT_CHOOSE:
                 if(resultCode==RESULT_OK && data!=null){
                     try {
                         Uri uri = data.getData();
                         ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri,"r");
                         Payload resultFile = Payload.fromFile(pfd);
//                         String filename = uri.getPath().substring(uri.getPath().lastIndexOf("/")+1);
                         String filename = uri.getLastPathSegment();
                         String message = resultFile.getId() + ":" +filename;
                         Payload bytes = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
                         client.sendPayload(cnnectedOffloader.getEndpointId(),bytes);
                         client.sendPayload(cnnectedOffloader.getEndpointId(),resultFile);
                     } catch (FileNotFoundException e) {
                         e.printStackTrace();
                     }
                 }
                 break;
         }
     }
     private void stopAdvertise() {
         if(!isAdvertising){
             Toast.makeText(ExecuterActivity.this,"Not Advertising",Toast.LENGTH_LONG).show();
             return;
         }
         client.stopAdvertising();
         isAdvertising = false;
         setStatusText("Stopped Advertising");
     }

     private void setStatusText(String message){
         status.setText("Status: "+message);
     }

     private class MyDialogueClickListener implements DialogInterface.OnClickListener{

         String endpointId;
         ConnectionInfo connectionInfo;
         public MyDialogueClickListener(String s, ConnectionInfo i){
             this.endpointId = s;
             this.connectionInfo = i;
         }
         @Override
         public void onClick(DialogInterface dialogInterface, int i) {
             if(DialogInterface.BUTTON_POSITIVE == i){
//                 Log.d("hi",endpointId+connectionInfo.getEndpointName());
                 ExecuterPayloadCallback payloadCallback = new ExecuterPayloadCallback();
                 cnnectedOffloader.setCodename(connectionInfo.getEndpointName());
                 client.acceptConnection(endpointId,payloadCallback);
             }
             else if(DialogInterface.BUTTON_NEGATIVE == i){
                 client.rejectConnection(endpointId);
             }
         }
     }

     private void goToAction(){
         //before proceeding further, stop advertising
         stopAdvertise();
         /*btnStartAdvertise.setEnabled(false);
         btnStopAdvertise.setEnabled(false);*/
         initExecuterActionViews();
         executeractionPanel.animate().alpha(1.0f).setListener(new Animator.AnimatorListener() {
             @Override
             public void onAnimationStart(Animator animator) {

                 executeractionPanel.setVisibility(View.VISIBLE);
             }

             @Override
             public void onAnimationEnd(Animator animator) {

             }

             @Override
             public void onAnimationCancel(Animator animator) {

             }

             @Override
             public void onAnimationRepeat(Animator animator) {

             }
         });

     }

     private class ExecuterPayloadCallback extends PayloadCallback{

         @Override
         public void onPayloadReceived(String s, Payload payload) {
             incomingPayloads.put(payload.getId(),payload);
         }

         @Override
         public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
             if(payloadTransferUpdate.getStatus()==PayloadTransferUpdate.Status.SUCCESS){
                 if(incomingPayloads.containsKey(payloadTransferUpdate.getPayloadId()))             //update for incoming payload
                    processRecievedPayload(incomingPayloads.get(payloadTransferUpdate.getPayloadId()));
                 else           //update for outgoing payload
                     Toast.makeText(ExecuterActivity.this,"File Sending complete",Toast.LENGTH_SHORT).show();
             }
         }
     }


     private void processRecievedPayload(Payload payload) {

         switch (payload.getType()){
             case Payload.Type.BYTES:
                 String message= new String(payload.asBytes(),StandardCharsets.UTF_8);
                 String parts[] = message.split(":");
                 payloadFilenameMap.put(Long.parseLong(parts[0]),parts[1]);

                 break;

             case Payload.Type.FILE:
                 try {
                     File file = payload.asFile().asJavaFile();


//                     String fileExtension = payloadFilenameMap.get(payload.getId()).split(".")[1];
//                     String filename = "";
//                     if(fileExtension.equals(".py"))    filename = "source.py";
//                     else if (fileExtension.equals(".txt")) filename = "input.txt";
                     boolean is = file.renameTo(new File(file.getParentFile(),payloadFilenameMap.get(payload.getId())));
                     setStatusText(++count +"file recieved: "+payloadFilenameMap.get(payload.getId()));
                     Log.d(TAG,"renamed: "+is);
                 }
                 catch (Exception e){
                     e.printStackTrace();
                 }
                 break;
         }
     }
 }
