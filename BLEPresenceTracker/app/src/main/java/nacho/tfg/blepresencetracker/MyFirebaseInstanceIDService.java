/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nacho.tfg.blepresencetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.BooleanResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    public static final String ACTION_SHOW_TOKEN = MyFirebaseInstanceIDService.class.getName() + "InstanceIDBroadcast";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        // Set registration_id_sent to false
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MyFirebaseInstanceIDService.this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getString(R.string.registration_id_sent), false);
        editor.commit();

        storeRegistrationId(refreshedToken);

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        Intent intent = new Intent(ACTION_SHOW_TOKEN);
        intent.putExtra("token", token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        AsyncTask<String, Void, Boolean> asyncTask = new AsyncTask<String, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

            }

            @Override
            protected Boolean doInBackground(String... params) {
                String data = params[0];
                String ip = "192.168.4.1";
                int port = 7777;
                try {
                    Socket socket = new Socket(ip, port);
                    while(!socket.isConnected()) {

                    }

                    OutputStream out = socket.getOutputStream();
                    PrintWriter output = new PrintWriter(out);

                    output.println(data);
                    output.flush();
                    out.close();
                    output.close();

                    socket.close();
                } catch (SocketException e){
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(result) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MyFirebaseInstanceIDService.this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.registration_id_sent), true);
                    editor.commit();
                }
                else{
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MyFirebaseInstanceIDService.this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.registration_id_sent), false);
                    editor.commit();
                }
            }
        };

        // Change wifi network to "NodeMCU WiFi"

        String ssid = "";
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()) == NetworkInfo.DetailedState.CONNECTED) {
            ssid = wifiInfo.getSSID();
        }

        if (!ssid.equals("NodeMCU WiFi")){
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_connect_NodeMCU), Toast.LENGTH_LONG).show();

                }
            });

        }
    }

    // Store the new registration id in the sharedpreferences
    private void storeRegistrationId(String token){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MyFirebaseInstanceIDService.this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.registration_id), token);
        editor.commit();
    }
}