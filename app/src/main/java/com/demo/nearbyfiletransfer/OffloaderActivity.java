package com.demo.nearbyfiletransfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.demo.nearbyfiletransfer.Utility.Constants;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OffloaderActivity extends AppCompatActivity implements ExecutersListAdapter.ItemClicked {
    private static final String TAG = "OffloaderActivity";
    //discoveryPanel
    TextView tvCodename,tvStatus;
    Button btnStartDiscover,btnStopDiscover,btnProceed;
    View offloadAction;
    ConstraintLayout discoveryPanel;
    RecyclerView recyclerExecutersList;
    RecyclerView.Adapter executersListAdapter;
    RecyclerView.LayoutManager layoutManager;
    //offloaderActionPanel
    Spinner spExecuter,spOperation;
    ImageView ivSend,ivRecieved;
    Button btnOffload;
    List<ExecuterModel> connectedExecuters = new ArrayList<>();
    TextView tvSizeSend,tvSizeRecieved;

    boolean isInAction=false, isNoExecuterSelected, isNoOperationSelected, isDiscovering,isSending=false;
    String codename, SERVICE_ID, selectedOperation;
    DiscoveryOptions options;
    List<ExecuterModel> executerList = new ArrayList<>();
    ExecuterModel  selectedExecuter;
    private final int REQUEST_IMAGE_CHOOSE = 11;
    Payload fileToSend,byteToSend;
    //Nearby client and callbacks
    ConnectionsClient client;
    EndpointDiscoveryCallback endpointDiscoveryCallback;
    ConnectionLifecycleCallback connectionLifecycleCallback;
    OffloaderPayloadCallback payloadCallback;
    Map<Long,Payload> incomingPayloads = new ArrayMap<>();
    Map<Long,String> payloadFilenameMap = new ArrayMap<>();
    String filename;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offloader);

        codename = com.google.location.nearby.apps.rockpaperscissors.CodenameGenerator.generate();
        client = Nearby.getConnectionsClient(OffloaderActivity.this);
        options = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        SERVICE_ID=getApplicationContext().getPackageName();
        initViews();
    }

    private void initViews() {
        offloadAction = findViewById(R.id.offloadAction);
        offloadAction.setVisibility(View.GONE);
        tvCodename = findViewById(R.id.tv_off_codename);
        String codeNameText = getString(R.string.codename)+codename;
        tvCodename.setText(codeNameText);
        tvStatus = findViewById(R.id.tv_off_status);
        btnStartDiscover = findViewById(R.id.btn_startDiscover);
        btnStopDiscover = findViewById(R.id.btn_stopDiscover);
        btnProceed = findViewById(R.id.btn_proceed);
        recyclerExecutersList = findViewById(R.id.recyclerView2);
        layoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.HORIZONTAL,false);
        recyclerExecutersList.setLayoutManager(layoutManager);
        executersListAdapter = new ExecutersListAdapter(executerList,OffloaderActivity.this);
        recyclerExecutersList.setAdapter(executersListAdapter);
        discoveryPanel =findViewById(R.id.discoveryPanel);
    }

    private void initActionViews(){
        //offloaderAction
        spExecuter = findViewById(R.id.sp_choose_exec_);
        spOperation = findViewById(R.id.drop_down_operation);
        btnOffload = findViewById(R.id.btn_offload);
        ivSend = findViewById(R.id.iv_off_task_data);
        ivRecieved = findViewById(R.id.iv_off_result);
        tvSizeSend = findViewById(R.id.tv_off_task_size);
        tvSizeRecieved = findViewById(R.id.tv_off_result_size);
        List<String> operationList = new ArrayList<>();
        operationList.add("Compress Image");
        operationList.add("Grayscale Image");
        ArrayAdapter<String> operationAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, operationList);
        operationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        OperationSelectedListener opListener = new OperationSelectedListener();
        spOperation.setOnItemSelectedListener(opListener);
        spOperation.setAdapter(operationAdapter);
        ArrayAdapter<ExecuterModel> executerAdapter = new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,connectedExecuters);
        executerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        ExecuterSelectedListener exListener = new ExecuterSelectedListener();
        spExecuter.setOnItemSelectedListener(exListener);
        spExecuter.setAdapter(executerAdapter);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_startDiscover:
                if(isDiscovering){
                    Toast.makeText(this,"Already Discovering",Toast.LENGTH_SHORT).show();
                    return;
                }
                endpointDiscoveryCallback = new OffloaderEndpointDiscoveryCallback();
                client.startDiscovery(SERVICE_ID,endpointDiscoveryCallback,options).
                        addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(OffloaderActivity.this,"Discovering",Toast.LENGTH_SHORT).show();
                                setStatusText("Discovering Nearby Executors");
                                isDiscovering = true;
                            }
                        }).
                        addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(OffloaderActivity.this,"Discovery failed",Toast.LENGTH_SHORT).show();
                                setStatusText("Discovery Failed");
                                isDiscovering = false;
                            }
                        });
                break;

            case R.id.btn_stopDiscover:
                stopDiscovery();
                break;

            case R.id.btn_proceed:
                goToAction();
                break;

            case R.id.btn_offload:
                sendTaskData();
                break;

            case R.id.iv_off_task_data:
                selectTaskData();
                break;
        }
    }

    private void stopDiscovery(){
        if(!isDiscovering){
            Toast.makeText(this,"Currently Not Discovering",Toast.LENGTH_SHORT).show();
            return;
        }
        client.stopDiscovery();
        setStatusText("Discovery Stopped");
        isDiscovering = false;
    }
    private void goToAction() {
        stopDiscovery();
        discoveryPanel.animate().alpha(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                discoveryPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        offloadAction.animate().alpha(1.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                offloadAction.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        isInAction = true;
        initActionViews();
    }

    private void setStatusText(String message){
        String statusText = getString(R.string.status)+message;
        tvStatus.setText(statusText);
    }

    @Override
    public void onItemClicked(int position) {
        final ExecuterModel executer = executerList.get(position);
        connectionLifecycleCallback = new OffloaderConnectionLifecycleCallback();
        client.requestConnection(codename,executer.getEndpointId(),connectionLifecycleCallback)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                setStatusText("Requested connection to: "+executer.getCodename());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(OffloaderActivity.this,"Could not request connection",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int searchExecuterById(String endpointId){
        int position=-1;
        for(ExecuterModel m : executerList){
            if(m.getEndpointId().equals(endpointId)){
                position = executerList.indexOf(m);
                break;
            }
        }
        return position;
    }

    private void selectTaskData(){
        Intent chooseImageFromGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(chooseImageFromGallery,REQUEST_IMAGE_CHOOSE);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CHOOSE) {
            if (resultCode == RESULT_OK) {
                try{
                    assert data != null;
                    Uri uri = data.getData();
                    Glide.with(OffloaderActivity.this).load(uri).into(ivSend);
                    filename = uri.getLastPathSegment();
                    assert uri != null;
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                    assert pfd != null;
                    fileToSend = Payload.fromFile(pfd);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG,"FileNotFound:"+e.getMessage());
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                    Log.e(TAG,"NullPointerException: "+e.getMessage());
                }
            }
        }
    }



    private void sendTaskData() {

        String messageToSend = selectedOperation + ":" + fileToSend.getId() + ":" + filename + ".jpg";
        byteToSend = Payload.fromBytes(messageToSend.getBytes(StandardCharsets.UTF_8));
        client.sendPayload(selectedExecuter.getEndpointId(),byteToSend);

       client.sendPayload(selectedExecuter.getEndpointId(),fileToSend);

    }

    private class OperationSelectedListener implements AdapterView.OnItemSelectedListener{


        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            selectedOperation = (String) adapterView.getItemAtPosition(i);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            isNoOperationSelected = true;
        }
    }

    private class ExecuterSelectedListener implements AdapterView.OnItemSelectedListener{


        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            selectedExecuter = (ExecuterModel) adapterView.getItemAtPosition(i);
            Log.d(TAG,selectedExecuter.getCodename());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            isNoExecuterSelected = true;
        }
    }

    private class OffloaderEndpointDiscoveryCallback extends EndpointDiscoveryCallback{
        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            if(discoveredEndpointInfo.getServiceId().equals(SERVICE_ID)){
                ExecuterModel executer = new ExecuterModel(discoveredEndpointInfo.getEndpointName(),"5",s, Constants.ConnectionStatus.NEUTRAL);
                executerList.add(executer);
                executersListAdapter.notifyItemInserted(executerList.indexOf(executer));

            }

//                Toast.makeText(OffloaderActivity.this,discoveredEndpointInfo.getEndpointName()+" found",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEndpointLost(String s) {

        }
    }

    private class OffloaderConnectionLifecycleCallback extends ConnectionLifecycleCallback{
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
            if(!connectionInfo.isIncomingConnection()){
                AlertDialog.Builder showAuthToken = new AlertDialog.Builder(OffloaderActivity.this);
                showAuthToken.setTitle("New Connection Initiated");
                showAuthToken.setMessage("Executer Codename: "+connectionInfo.getEndpointName() +"\n"+"Authentication Token: "+connectionInfo.getAuthenticationToken());
                showAuthToken.setCancelable(true);
                showAuthToken.create();
                showAuthToken.show();
                payloadCallback = new OffloaderPayloadCallback();
                client.acceptConnection(s,payloadCallback);
            }
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            int position = searchExecuterById(s);
            if(connectionResolution.getStatus().isSuccess()){
                Toast.makeText(OffloaderActivity.this,"Connected successfully to: "+executerList.get(position).getCodename(),Toast.LENGTH_SHORT).show();

                executerList.get(position).setStatus(Constants.ConnectionStatus.CONNECTED);
                executersListAdapter.notifyItemChanged(position);
                connectedExecuters.add(executerList.get(position));
            }
            else if(connectionResolution.getStatus().isCanceled()){
                Toast.makeText(OffloaderActivity.this,s+" rejected your connection request",Toast.LENGTH_SHORT).show();
                executerList.get(position).setStatus(Constants.ConnectionStatus.REJECTED);
                executersListAdapter.notifyItemChanged(position);
            }
        }

        @Override
        public void onDisconnected(String s) {

        }
    }

    private class OffloaderPayloadCallback extends PayloadCallback{
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
                    Toast.makeText(OffloaderActivity.this,"File Sending complete",Toast.LENGTH_SHORT).show();
            }
        }



    private void processRecievedPayload(Payload payload) {

        switch (payload.getType()){
            case Payload.Type.BYTES:
                String message= new String(payload.asBytes(),StandardCharsets.UTF_8);
                String[] parts = getFilename(message);
                break;

            case Payload.Type.FILE:
                try {
                    File file = payload.asFile().asJavaFile();
                    File location = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"Nearby");
//                   String name = new String(incomingPayloads.get(payload.getId()).asBytes(),StandardCharsets.UTF_8);
                    String filename="rec.jpg";
                    boolean is = file.renameTo(new File(file.getParentFile(),payloadFilenameMap.get(payload.getId())));
                    Log.d(TAG,"renamed: "+is);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
    }
    private String[] getFilename(String message){
        String[] parts = message.split(":");

        long payloadId = Long.parseLong(parts[0]);
        String filename=parts[1];
        payloadFilenameMap.put(payloadId,filename);
        return parts;
    }
    }
}