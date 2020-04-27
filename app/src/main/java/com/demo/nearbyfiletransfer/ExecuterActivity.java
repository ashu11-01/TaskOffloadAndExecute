 package com.demo.nearbyfiletransfer;

 import android.animation.Animator;
 import android.content.DialogInterface;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.Environment;
 import android.util.Log;
 import android.view.View;
 import android.widget.Button;
 import android.widget.ImageView;
 import android.widget.TextView;
 import android.widget.Toast;

 import androidx.annotation.NonNull;
 import androidx.annotation.RequiresApi;
 import androidx.appcompat.app.AlertDialog;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.collection.ArrayMap;

 import com.demo.nearbyfiletransfer.BrokerUtility.AdvertiserRating;
 import com.demo.nearbyfiletransfer.Logger.InstantSystemParameters;
 import com.demo.nearbyfiletransfer.Operations.CompressImage;
 import com.demo.nearbyfiletransfer.Utility.Constants;
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
    TextView tvCodename,status;
    Button btnStartAdvertise, btnStopAdvertise;

    //executeraction panel
     View executeractionPanel;
    Button btnExecute,btnSendBack;
    TextView tvOffloader,tvTask,tvOperation;
    ImageView ivRecieve,ivSend;
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

         tvOffloader = findViewById(R.id.tv_exec_conn);
         tvTask = findViewById(R.id.tv_exec_task);
         tvOperation = findViewById(R.id.tv_exec_op);
         ivSend = findViewById(R.id.iv_exec_send);
         ivRecieve = findViewById(R.id.iv_exec_recieved);
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
                client.startAdvertising(getAdvertisingMessage(),getApplicationContext().getPackageName(), connectionLifecycleCallback,options).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
//                        Toast.makeText(ExecuterActivity.this,"Advertising",Toast.LENGTH_SHORT).show();
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
                performOperation();
                break;

            case R.id.btn_send:
                try{Payload send = Payload.fromFile(result);client.sendPayload(cnnectedOffloader.getEndpointId(),send);}
                catch (FileNotFoundException e ) {e.printStackTrace();}

                break;
        }
     }

     private void performOperation() {
         if(opCode == Constants.OperationCodes.compressOpCode){
             result = CompressImage.compress(this,recievedFile,40);
             Toast.makeText(this,result.getName()+"",Toast.LENGTH_SHORT).show();
             Log.d(TAG,"inside: "+opCode+" "+(result==null));
         }
         Log.d(TAG,opCode+" "+(result==null));
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
                 String[] parts = getFilenameAndOperation(message);
                 tvOperation.setText(getString(R.string.operation)+parts[0]);
                 opCode = Constants.OperationCodes.getOpCodeForOperation(parts[0]);
                 break;

             case Payload.Type.FILE:
                 try {
                     File file = payload.asFile().asJavaFile();
                     File location = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"Nearby");
//                   String name = new String(incomingPayloads.get(payload.getId()).asBytes(),StandardCharsets.UTF_8);
                     String filename="rec.jpg";
                     boolean is = file.renameTo(new File(file.getParentFile(),payloadFilenameMap.get(payload.getId())));
                     recievedFile = file;
                     Log.d(TAG,"renamed: "+is);
                 }
                 catch (Exception e){
                     e.printStackTrace();
                 }
                 break;
         }
     }
     private String[] getFilenameAndOperation(String message){
         String[] parts = message.split(":");

         long payloadId = Long.parseLong(parts[1]);
         String filename=parts[2];
         payloadFilenameMap.put(payloadId,filename);
         return parts;
     }
 }
