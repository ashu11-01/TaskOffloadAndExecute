package com.demo.nearbyfiletransfer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.nearbyfiletransfer.MenuManager.PreferencesMenuManager;
import com.demo.nearbyfiletransfer.SelectionCriteria.TopsisSelection;
import com.demo.nearbyfiletransfer.SelectionCriteria.WeightedSumSelection;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OffloaderActivity extends AppCompatActivity implements ExecutersListAdapter.ItemClicked,PreferencesMenuManager.PreferencesSetListener {
    private static final String TAG = "OffloaderActivity";

    //discoveryPanel views
    TextView tvCodename,tvStatus;
    View offloadAction;
    ConstraintLayout discoveryPanel;
    RecyclerView recyclerExecutersList;
    RecyclerView.Adapter executersListAdapter;
    RecyclerView.LayoutManager layoutManager;

    //offloaderActionPanel views
    Spinner spExecuter,spOperation;
    Button btnOffload;
    Chronometer mChronometer;
    List<ExecuterModel> connectedExecuters = new ArrayList<>();
    TextView codeFilename,inputFilename,tvCountdown;
    boolean isInAction=false, isDiscovering, isSending=false;
    String codename, SERVICE_ID;

    DiscoveryOptions options;       //strategy

    List<ExecuterModel> executerList = new ArrayList<>(); // list of nearby executers
    ExecuterModel  selectedExecuter;        // executer selected to offload
    private final int REQUEST_INPUT_CHOOSE = 11;    //request code for input file choose
    private final int REQUEST_CODE_CHOOSE = 12;    // request code for code file choose
    Payload codeFileToSend,inputFiletoSend,byteToSend;                 // payload to send

    //Nearby client and callbacks
    ConnectionsClient client;
    EndpointDiscoveryCallback endpointDiscoveryCallback;
    ConnectionLifecycleCallback connectionLifecycleCallback;
    OffloaderPayloadCallback payloadCallback;
    Map<Long,Payload> incomingPayloads = new ArrayMap<>();
    Map<Long,String> payloadFilenameMap = new ArrayMap<>();
    Map<Long,String>payloadSenderMap = new ArrayMap<>();
    private float[] weightsArray = new float[4];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offloader);

        codename = com.google.location.nearby.apps.rockpaperscissors.CodenameGenerator.generate();
        client = Nearby.getConnectionsClient(OffloaderActivity.this);
        options = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        SERVICE_ID=getApplicationContext().getPackageName();
        initViews();
        //show offloading preference dialoge
        PreferencesMenuManager manager = new PreferencesMenuManager(OffloaderActivity.this);
        manager.setPreferenceWeights();

    }

    private void initViews() {
        offloadAction = findViewById(R.id.offloadAction);
        offloadAction.setVisibility(View.GONE);
        tvCodename = findViewById(R.id.tv_off_codename);
        String codeNameText = getString(R.string.codename)+codename;
        tvCodename.setText(codeNameText);
        tvStatus = findViewById(R.id.tv_off_status);
        tvCountdown=findViewById(R.id.tv_countdown);
        recyclerExecutersList = findViewById(R.id.recyclerView2);
        layoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.HORIZONTAL,false);
        recyclerExecutersList.setLayoutManager(layoutManager);
        Collections.sort(executerList,Collections.<ExecuterModel>reverseOrder());
        executersListAdapter = new ExecutersListAdapter(executerList,OffloaderActivity.this);
        recyclerExecutersList.setAdapter(executersListAdapter);
        discoveryPanel =findViewById(R.id.discoveryPanel);
    }

    private void initActionViews(){
        //offloaderAction
        mChronometer=(Chronometer) findViewById(R.id.chronometer);
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsedMillis =  SystemClock.elapsedRealtime() - chronometer.getBase();
                if(elapsedMillis>3600000L)
                    mChronometer.setFormat("0%s");
                else
                    mChronometer.setFormat("00:%s");
            }
        });

        codeFilename=findViewById(R.id.tv_code_file_display);
        inputFilename = findViewById(R.id.tv_input_file_display);
        spExecuter = findViewById(R.id.sp_choose_exec_);
        btnOffload = findViewById(R.id.btn_offload);
        ArrayAdapter<ExecuterModel> executerAdapter = new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,connectedExecuters);
        executerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        ExecuterSelectedListener exListener = new ExecuterSelectedListener();
        spExecuter.setOnItemSelectedListener(exListener);
        spExecuter.setAdapter(executerAdapter);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_offload:
                sendTaskData();
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
                break;

            case R.id.btn_code:
                selectFile(REQUEST_CODE_CHOOSE);
                break;

            case R.id.btn_input:
                selectFile(REQUEST_INPUT_CHOOSE);
                break;
        }
    }

    private void startDiscovery(){
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
    }

    private void selectFile(int requestCode) {
        Intent filechooser = new Intent(Intent.ACTION_GET_CONTENT);
        filechooser.setType("file/*");
        startActivityForResult(filechooser,requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_CHOOSE:
                if(resultCode==RESULT_OK && data!=null){
                    try {
                        Uri uri = data.getData();
                        ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri,"r");
                        codeFileToSend = Payload.fromFile(pfd);
                        String filename = uri.getPath().substring(uri.getPath().lastIndexOf("/")+1);
                        codeFilename.setText(uri.getLastPathSegment());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_INPUT_CHOOSE:
                if(resultCode==RESULT_OK && data!=null){
                    try {
                        Uri uri = data.getData();
                        ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri,"r");
                        inputFiletoSend = Payload.fromFile(pfd);
                        String filename = uri.getPath().substring(uri.getPath().lastIndexOf("/")+1);
                        inputFilename.setText(filename);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
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

    private void setStatusText(String message){
        String statusText = getString(R.string.status)+message;
        tvStatus.setText(statusText);
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


    private void sendTaskData() {

        String codeFile = codeFilename.getText().toString();
        long codePayloadId = codeFileToSend.getId();
        String message = String.valueOf(codePayloadId) + ":" + codeFile;
        byteToSend = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        isSending = true;

        String inputFile = inputFilename.getText().toString();
        long inputPayloadId = inputFiletoSend.getId();
        message = String.valueOf(inputPayloadId) + ":" + inputFile;
        Payload byteToSend2 = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));

        client.sendPayload(selectedExecuter.getEndpointId(),byteToSend);            //source code filename message payload
        client.sendPayload(selectedExecuter.getEndpointId(),byteToSend2);           //input filename message payload
        client.sendPayload(selectedExecuter.getEndpointId(),codeFileToSend);        //source code file payload
        client.sendPayload(selectedExecuter.getEndpointId(),inputFiletoSend);       // inpute file payload
        isSending = false;
    }

    @Override
    public void onPreferencesSetListener(int seconds, final Constants.SelectionMethod selectionMethod) {
        startDiscovery();
        new CountDownTimer(seconds*1000, 1) {
            @Override
            public void onTick(long l) {
                long seconds=l/1000;
                String show_min = (seconds/60 < 10 ? "0"+seconds/60 : ""+seconds/60);
                String show_sec = (seconds%60 < 10 ? "0"+seconds%60 : ""+seconds%60);
                tvCountdown.setText(show_min+":"+show_sec);
            }

            @Override
            public void onFinish() {
                stopDiscovery();
                if(executerList.size()>0)
                    requestConnectionToBestExecuter(selectionMethod);
                else
                    Toast.makeText(OffloaderActivity.this,"No executers found. Try Again.",Toast.LENGTH_SHORT).show();
                executersListAdapter.notifyDataSetChanged();
            }
        }.start();

    }

    private void requestConnectionToBestExecuter(Constants.SelectionMethod selectionMethod) {
        if (executerList.size() > 1) {
            switch (selectionMethod) {
                case WEIGHTED_SUM:
                    executerList = WeightedSumSelection.weightedSumBestExecuters(executerList, getApplicationContext());
                    break;
                case TOPSIS:
                    executerList = TopsisSelection.getTopsisBestExecuters(getApplicationContext(), executerList);
                    break;
            }
        }

//        Collections.sort(executerList, Collections.<ExecuterModel>reverseOrder());
        //executersListAdapter.n
        /*for(int i=0;i<executerList.size();i++){
            Log.d(TAG,(i+1) + " " +executerList.get(i).getCodename() + " " + executerList.get(i).getUtility());
        }*/
        try{
            final ExecuterModel bestExecuter = executerList.get(0);
            connectionLifecycleCallback = new OffloaderConnectionLifecycleCallback();
            client.requestConnection(codename,bestExecuter.getEndpointId(),connectionLifecycleCallback).
                    addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    setStatusText("Requested connection to: "+bestExecuter.getCodename());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    setStatusText("Could not request connection to: "+bestExecuter.getCodename());
                }
            });
        }catch (IndexOutOfBoundsException e ){}
    }

    @Override
    public void onItemClicked(int position) {

    }

    private class ExecuterSelectedListener implements AdapterView.OnItemSelectedListener{


        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            selectedExecuter = (ExecuterModel) adapterView.getItemAtPosition(i);
//            Log.d(TAG,selectedExecuter.getCodename());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }



    private class OffloaderEndpointDiscoveryCallback extends EndpointDiscoveryCallback{

        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            if(discoveredEndpointInfo.getServiceId().equals(SERVICE_ID)){
                Toast.makeText(getApplicationContext(),"New Executer found. Swipe the card to view",Toast.LENGTH_SHORT).show();

                ExecuterModel executer = splitAndStoreAdvertisingMessage(discoveredEndpointInfo.getEndpointName());
                executer.setEndpointId(s);
                executerList.add(executer);
                executersListAdapter.notifyItemInserted(executerList.indexOf(executer));
            }
        }

        @Override
        public void onEndpointLost(String s) {

        }
    }

    private ExecuterModel splitAndStoreAdvertisingMessage(String s) {
        String parts[] = s.split("/");
//        Log.d(TAG,"split and store: "+parts[0]);
        ExecuterModel executer = new ExecuterModel(parts[0],parts[1],parts[2],parts[3],parts[4],parts[5],parts[6],parts[7]);
        return executer;
    }

    private class OffloaderConnectionLifecycleCallback extends ConnectionLifecycleCallback{
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
            if(!connectionInfo.isIncomingConnection()){
                AlertDialog.Builder showAuthToken = new AlertDialog.Builder(OffloaderActivity.this);
                showAuthToken.setTitle("New Connection Initiated");
                showAuthToken.setMessage("Executer Codename: "+connectionInfo.getEndpointName().split("/")[0] +"\n"
                        +"Authentication Token: "+connectionInfo.getAuthenticationToken());
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
                setStatusText("Connected to: "+executerList.get(position).getCodename());
                executerList.get(position).setStatus(Constants.ConnectionStatus.CONNECTED);
                executersListAdapter.notifyItemChanged(position);
                connectedExecuters.add(executerList.get(position));
                //connection estblished, proceed to choosing file and offloading task
                stopDiscovery();
                discoveryPanel.setVisibility(View.GONE);
                findViewById(R.id.offloadAction).setVisibility(View.VISIBLE);
                initActionViews();
                setStatusText("Ready to offload. Please choose files below.");
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
            int position = searchExecuterById(s);
            incomingPayloads.put(payload.getId(),payload);
            if(position!=-1)
                payloadSenderMap.put(payload.getId(),executerList.get(position).getCodename());
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
            if(payloadTransferUpdate.getStatus()==PayloadTransferUpdate.Status.SUCCESS){
                if(incomingPayloads.containsKey(payloadTransferUpdate.getPayloadId())) {           //update for incoming payload
                    processRecievedPayload(incomingPayloads.get(payloadTransferUpdate.getPayloadId()));
                    mChronometer.stop();
                   /* AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());
                    final AlertDialog dialog = alertDialog.create();
                    dialog.setTitle("Result Recieved");
                    dialog.setMessage("Output file recieved in: "+mChronometer.getText().toString());
                    dialog.setCancelable(true);*/
//                    dialog.show();
                }
                else           //update for outgoing payload
                    Toast.makeText(OffloaderActivity.this,"sent successfully",Toast.LENGTH_SHORT).show();
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
                    setStatusText("Result file recieved");
//                    Log.d(TAG,"renamed: "+is);
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