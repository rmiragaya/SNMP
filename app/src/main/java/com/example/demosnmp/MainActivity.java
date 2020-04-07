package com.example.demosnmp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SNMP CLIENT";
    // TODO change ip address :)
    static String ipAddress = "localhost";
    private static final String port = "161";

    // command to request from Server
    // TODO change the oid value

    private static final String OIDVALUE = "1.3.6.1.4.1.12619.1.1.2";
    // TODO change de Snmp Version
    private static final int SNMP_VERSION = SnmpConstants.version1;
    static String community = "public";

    //
    public static Snmp snmp;
    public static CommunityTarget comtarget;
    static PDU pdu;
    static OID oid;
    static VariableBinding req;

    // UI
    private Button sendBtn;
    private EditText console;
    private ProgressBar mSpinner;
    private StringBuffer logResult = new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        iniUI();

        // set onClick listener
        sendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mAsyncTask.execute();
            }
        });

    }


    private void iniUI() {
        sendBtn = findViewById(R.id.sendBtn);
        console = findViewById(R.id.console);
        mSpinner = findViewById(R.id.progressBar);
    }

    private void sendSnmpRequest(String cmd) throws Exception {
        // Create TransportMapping and Listen
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();

        Log.d(TAG, "Create Target Address object");
        logResult.append("Create Target Address object\n");
        // Create Target Address object
        CommunityTarget comtarget = new CommunityTarget();
        comtarget.setCommunity(new OctetString(community));
        comtarget.setVersion(SNMP_VERSION);

        Log.d(TAG, "-address: " + ipAddress + "/" + port);
        logResult.append("-address: " + ipAddress + "/" + port + "\n");

        comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
        comtarget.setRetries(2);
        comtarget.setTimeout(1000);

        Log.d(TAG, "Prepare PDU");
        logResult.append("Prepare PDU\n");
        // create the PDU
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(cmd)));
        pdu.setType(PDU.GETNEXT);

        Snmp snmp = new Snmp(transport);
        Log.d(TAG, "Sending Request to Agent...");
        logResult.append("Sending Request to Agent...\n");

        // send the PDU
        ResponseEvent response = snmp.send(pdu, comtarget);

        // Process Agent Response
        if (response != null) {
            // extract the response PDU (could be null if timed out)
//            Log.d(TAG, "***********************");
            Log.d(TAG, "response.getError(): " + response.getError().toString());
            Log.d(TAG, "response.getPeerAddress(): " + response.getPeerAddress().toString());
            Log.d(TAG, "response.getRequest(): " + response.getRequest().toString());
            Log.d(TAG, "response.getSource(): " + response.getSource().toString());
//            Log.d(TAG, "***********************");


            PDU responsePDU = response.getResponse();
            // extract the address used by the agent to send the response:
            Address peerAddress = response.getPeerAddress();
            Log.d(TAG, "peerAddress " + peerAddress);
            if (responsePDU != null) {
                int errorStatus = responsePDU.getErrorStatus();
                int errorIndex = responsePDU.getErrorIndex();
                String errorStatusText = responsePDU.getErrorStatusText();

                if (errorStatus == PDU.noError) {
                    Log.d(TAG,
                            "Snmp Get Response = "
                                    + responsePDU.getVariableBindings());
                    logResult.append("Snmp Get Response = "
                            + responsePDU.getVariableBindings() + "\n");
                } else {
                    Log.d(TAG, "Error: Request Failed");
                    Log.d(TAG, "Error Status = " + errorStatus);
                    Log.d(TAG, "Error Index = " + errorIndex);
                    Log.d(TAG, "Error Status Text = " + errorStatusText);

                    logResult.append("Error: Request Failed"
                            + "Error Status = " + errorStatus
                            + "Error Index = " + errorIndex
                            + "Error Status Text = " + errorStatusText + "\n");
                }
            } else {
                Log.d(TAG, "Error: Response PDU is null");
                logResult.append("Error: Response PDU is null \n");
            }
        } else {
            Log.d(TAG, "Error: Agent Timeout... \n");
            logResult.append("Error: Agent Timeout... \n");
        }
        snmp.close();
    }

    // AsyncTask to do job in background
    AsyncTask<Void, Void, Void> mAsyncTask = new AsyncTask<Void, Void, Void>() {

        protected void onPreExecute() {
            mSpinner.setVisibility(View.VISIBLE);
        };

        @Override
        protected Void doInBackground(Void... params) {
            try {
                sendSnmpRequest(OIDVALUE);
            } catch (Exception e) {
                Log.d(TAG,
                        "Error sending snmp request - Error: " + e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            console.setText("");
            console.append(logResult);
            mSpinner.setVisibility(View.GONE);
        };

    };

}