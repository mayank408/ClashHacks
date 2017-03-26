package com.example.mohitkumar.isgw;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.http.HttpClient;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

/**
 * Created by Dell on 1/21/2017.
 */

public class SpeechToText extends AppCompatActivity implements TextToSpeech.OnInitListener{




    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static OutputStream outStream = null;

    // Well known SPP UUID
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "98:D3:32:70:8B:76";


    TextToSpeech tts;
    String sout,action1;
    FloatingActionButton refbut,chatbut;
    CardView cardView;

    private static String TAG = MainActivity.class.getSimpleName();
    String s,jsonResponse;
    String ACCESS_TOKEN="67565cd4b0a34c6c82ec141d969541be";
    private TextView txtSpeechInput,res;
    private FloatingActionButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speechtotext);
        txtSpeechInput=(TextView)findViewById(R.id.txtSpeechInput);
        res=(TextView)findViewById(R.id.response);
        //refbut = (FloatingActionButton)findViewById(R.id.float_butt2);
        chatbut = (FloatingActionButton)findViewById(R.id.float_butt1);
        btnSpeak=(FloatingActionButton)findViewById(R.id.btnSpeak);
        //cardView =(CardView)findViewById(R.id.card_view1);
        tts = new TextToSpeech(this,this);
        btnSpeak=(FloatingActionButton)findViewById(R.id.btnSpeak);
        //cardView =(CardView)findViewById(R.id.card_view1);
        tts = new TextToSpeech(SpeechToText.this,SpeechToText.this);


        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        chatbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SpeechToText.this,ChatBotActivity.class);
                startActivity(intent);
            }
        });

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
//                cardView.setVisibility(View.GONE);
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...In onResume - Attempting client connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting to Remote...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Creating Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast msg = Toast.makeText(getBaseContext(),
                title + " - " + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    public static void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            //errorExit("Fatal Error", msg);
        }
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    //cardView.setVisibility(View.VISIBLE);

                    final ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtSpeechInput.setText(result.get(0));
                   // for(int i=0;i<result.size()-1;i++){

                   //         s=result.get(i)+" ";

                    //}
                    //sout = "";
                    //for(int i=0;i<s.length();i++){
                      //  if(s.charAt(i)==' '){
                      //      sout+="%20";
                      //  }
                       // else{
                     //       sout+=s.charAt(i);
                    //    }
                  //  }
                     //finalStr=url1+sout+url2;


                    s = txtSpeechInput.getText().toString();

                    //URL1 = URL1 + s;

                   // Toast.makeText(getApplicationContext(),URL1,Toast.LENGTH_LONG).show();

                    final AIConfiguration config = new AIConfiguration(ACCESS_TOKEN,
                            AIConfiguration.SupportedLanguages.English,
                            AIConfiguration.RecognitionEngine.System);

                    final AIDataService aiDataService = new AIDataService(config);

                    final AIRequest aiRequest = new AIRequest();
                    aiRequest.setQuery(s);

                    new AsyncTask<AIRequest, Void, AIResponse>() {
                        @Override
                        protected AIResponse doInBackground(AIRequest... requests) {
                            final AIRequest request = requests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                Log.d(TAG, "doInBackground: " + response);
                                return response;
                            } catch (AIServiceException e) {
                            }
                            return null;
                        }
                        @Override
                        protected void onPostExecute(AIResponse aiResponse) {

                            if(aiResponse == null){
                                //Log.d("Result","NULL");
                            }
                            //if (aiResponse != null ) {
                           // Log.d("INHERE","Entered");
                                // process aiResponse here
                                Result result = aiResponse.getResult();

                            if(result != null) {


                                action1 = result.getStringParameter("action");
                                String appliance1 = result.getStringParameter("appliance");
                                String location = result.getStringParameter("location");
                                String s = result.getFulfillment().getSpeech().toString();

                                // Log.d("INHERE1",s);

                                //makeJsonObjectRequest();


                                //MySingleton.getInstance(SpeechToText.this).addToRequestQueue();

                                res.setText(s);


                                if (action1.equals("turn on")) {
                                    action1 = "on";
                                    // URL1 = URL1 + "action=" + action1 + "&appliance="+appliance1+
                                    //         "&location=" + location;
                                    Log.d("ON", action1 + "hello");
                                    sendData("1");


                                } else if (action1.equals("turn off")) {
                                    action1 = "off";
                                    //   URL1 = URL1 + "action=" + action1 + "&appliance="+appliance1+
                                    //           "&location=" + location;
                                    Log.d("OFF", action1);
                                    sendData("0");
                                }
                            }
                        }

                    }.execute(aiRequest);
                }
                break;
            }

        }

    }


//    private void makeJsonObjectRequest() {
//        final String finalStr = URL;
//        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
//                finalStr, (JSONObject) null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                Log.d(TAG, response.toString());
//
//                try {
//                    // Parsing json object response
//                    // response will be a json object
//                    //String name = response.getString("name");
//                    // String email = response.getString("email");
//                    //JSONObject phone = response.getJSONObject("phone");
//                    //String home = phone.getString("home");
//                    //String mobile = phone.getString("mobile");
//
//                    jsonResponse = response.getJSONObject("fulfillment").getString("speech");
//
//                    Log.d("Response", jsonResponse);
//
//                    res.setText(jsonResponse);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(),
//                            "Error: " + e.getMessage(),
//                            Toast.LENGTH_LONG).show();
//                }
//            }
//        });
//    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "This Language is not supported");
            } else {
               // btnspeak.setEnabled(true);
                speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void speakOut() {
       String text = res.getText().toString();
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }
}

