package com.demo.nearbyfiletransfer;

import android.animation.Animator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.demo.nearbyfiletransfer.MenuManager.ServiceRequestManager;
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
import java.util.List;
import java.util.Map;

public class OffloaderActivity extends AppCompatActivity implements ExecutersListAdapter.ItemClicked {
    private static final String TAG = "OffloaderActivity";

    //discoveryPanel views
    TextView tvCodename,tvStatus;
    Button btnStartDiscover,btnStopDiscover,btnProceed;
    View offloadAction;
    ConstraintLayout discoveryPanel;
    RecyclerView recyclerExecutersList;
    RecyclerView.Adapter executersListAdapter;
    RecyclerView.LayoutManager layoutManager;

    //offloaderActionPanel views
    Spinner spExecuter,spOperation;
    Button btnOffload;
    List<ExecuterModel> connectedExecuters = new ArrayList<>();
    TextView codeFilename,inputFilename;
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
        codeFilename=findViewById(R.id.tv_code_file_display);
        inputFilename = findViewById(R.id.tv_input_file_display);
        spExecuter = findViewById(R.id.sp_choose_exec_);
        btnOffload = findViewById(R.id.btn_offload);
       /* List<String> extensionList = new ArrayList<>();
        extensionList.add("(.py) Python");
        extensionList.add("(.cpp) C++");
        ArrayAdapter<String> operationAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, extensionList);
        operationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        FileTypeSelectedListener opListener = new FileTypeSelectedListener();
        spOperation.setOnItemSelectedListener(opListener);
        spOperation.setAdapter(operationAdapter);*/
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

                SharedPreferences preferences = getSharedPreferences("WeightPreference",MODE_PRIVATE);
                float v = preferences.getFloat(Constants.SharedPreferenceKeys.RATING_WEIGHT,0.5f);
                Toast.makeText(this,String.valueOf(v),Toast.LENGTH_SHORT).show();
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

            case R.id.btn_code:
                selectFile(REQUEST_CODE_CHOOSE);
                break;

            case R.id.btn_input:
                selectFile(REQUEST_INPUT_CHOOSE);
                break;
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean result=false;
        switch (item.getItemId()){
            case R.id.weights:
                PreferencesMenuManager manager = new PreferencesMenuManager(OffloaderActivity.this);
                manager.setPreferenceWeights();
                result= true;
            case R.id.service_type:
                ServiceRequestManager.setServiceRequest(getApplicationContext());
                result= true;
            default:
                result =super.onOptionsItemSelected(item);
        }
        return result;
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

    private class FileTypeSelectedListener implements AdapterView.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

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

        }
    }

    private class OffloaderEndpointDiscoveryCallback extends EndpointDiscoveryCallback{
        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            if(discoveredEndpointInfo.getServiceId().equals(SERVICE_ID)){

                ExecuterModel executer = splitAndStoreAdvertisingMessage(discoveredEndpointInfo.getEndpointName());
                executer.setEndpointId(s);
                executerList.add(executer);
                executersListAdapter.notifyItemInserted(executerList.indexOf(executer));

            }

//                Toast.makeText(OffloaderActivity.this,discoveredEndpointInfo.getEndpointName()+" found",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEndpointLost(String s) {

        }
    }

    private ExecuterModel splitAndStoreAdvertisingMessage(String s) {
        String parts[] = s.split("/");
        Log.d(TAG,"split and store: "+parts[0]);
        ExecuterModel executer = new ExecuterModel(parts[0],parts[1],parts[2],parts[3],parts[4],parts[5],parts[6],parts[7]);
        return executer;
    }

    private class OffloaderConnectionLifecycleCallback extends ConnectionLifecycleCallback{
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
            if(!connectionInfo.isIncomingConnection()){
                AlertDialog.Builder showAuthToken = new AlertDialog.Builder(OffloaderActivity.this);
                showAuthToken.setTitle("New Connection Initiated");
                showAuthToken.setMessage("Executer Codename: "+connectionInfo.getEndpointName().split("/")[0] +"\n"+"Authentication Token: "+connectionInfo.getAuthenticationToken());
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