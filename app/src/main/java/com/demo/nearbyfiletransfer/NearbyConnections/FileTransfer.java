package com.demo.nearbyfiletransfer.NearbyConnections;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.demo.nearbyfiletransfer.MainActivity;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public class FileTransfer {
    private final String TAG ="File Transfer";

    public Payload.File  recievedFile;
    public String connectedToEndpointId;
    public ConnectionsClient connectionsClient;
    Context context;
    UpdateStatus status;
    public interface UpdateStatus{
        void setStatusText(String s);
    }
    public FileTransfer(Context context, ConnectionsClient connectionsClient) {
        //this.sendingFile = sendingFile;
        this.connectionsClient = connectionsClient;
        this.context = context;
        status = (UpdateStatus)context;
    }

    public final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String s, Payload payload) {
                    //Log.d(TAG,"onPayloadReceived: called");
                    recievedFile = payload.asFile();
                   // Log.d(TAG,"onPayloadReceived: success "+payload.getType());
                    status.setStatusText("Started Recieving file from: ");
                }

                @Override
                public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                    //Log.d(TAG,"onPayloadTransferUpdate: called");
                   // Log.d(TAG,"onPayloadTransferUpdate: success "+payloadTransferUpdate.getTotalBytes());
                    if(PayloadTransferUpdate.Status.SUCCESS==payloadTransferUpdate.getStatus()){
                        status.setStatusText("Recieved file size: "+payloadTransferUpdate.getTotalBytes()+"(Bytes");
                    }
                }
            };

    //callbacks for finding other devices
    public final EndpointDiscoveryCallback endpointDiscoveryCallback
            =new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            //request connection
            //Log.d(TAG,"onEndpointRecieved: called");
            status.setStatusText("Nearby discovered: "+s);
            connectionsClient.requestConnection(MainActivity.codename,s,connectionLifecycleCallback);
           // Log.d(TAG,"onEndpointRecieved: s= "+s);

        }

        @Override
        public void onEndpointLost(String s) {

        }
    };

    //callbacks for connections to other devices
    public final ConnectionLifecycleCallback connectionLifecycleCallback=
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                   // Log.d(TAG,"onConnectionInitiated: called");
                    status.setStatusText("Nearby discovered: "+s+"\nAuthentication code: "+connectionInfo.getAuthenticationToken());
                    connectionsClient.acceptConnection(s,payloadCallback);
                   // Log.d(TAG,"onConnectionInitiated: s= "+s);
                }

                @Override
                public void onConnectionResult(String s, ConnectionResolution connectionResolution) {

                    //Log.d(TAG,"onConnectionResult: called");
                    if (connectionResolution.getStatus().isSuccess()) {
                       // Log.d(TAG,"onConnectionResult: success s= "+s);
                        connectedToEndpointId =s;
                        status.setStatusText("Connected to: "+s);
                        Toast.makeText(context,"successfully connected to "+ connectedToEndpointId,Toast.LENGTH_SHORT).show();
                        connectionsClient.stopAdvertising();
                        connectionsClient.stopDiscovery();
                      //  reciever = s;
                    }
                }

                @Override
                public void onDisconnected(String s) {

                }


            };

}
