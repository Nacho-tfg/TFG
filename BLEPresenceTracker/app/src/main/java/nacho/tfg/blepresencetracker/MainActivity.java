package nacho.tfg.blepresencetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_END_CONNECTION_REQUEST = MainActivity.class.getName() + "EndConnectionRequest";

    public static TextView mainTextView;
    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;
    BroadcastReceiver wifiScanReceiverNodeMCU;
    BroadcastReceiver endConnectionRequestReceiver;
    ConnectivityManager connManager;
    ConnectivityManager.NetworkCallback networkCallback;

    private ListView listView;
    private ArrayList<NotificationItem> notificationsArray = new ArrayList<NotificationItem>();;
    private NotificationItemAdapter notificationItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifiNetworks();
            }
        });

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        initializeWifiScanReceiver();

        mainTextView  = (TextView) findViewById(R.id.tv_main);
        listView = (ListView) findViewById(R.id.lv_notifications);
        initializeListView();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String token = intent.getStringExtra("token");
                        mainTextView.setText(token);
                    }
                }, new IntentFilter(MyFirebaseInstanceIDService.ACTION_SHOW_TOKEN)
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String text = intent.getStringExtra("text");
                        String date = intent.getStringExtra("date");
                        String time = intent.getStringExtra("time");
                        NotificationItem item = new NotificationItem(text, date, time);

                        notificationsArray.add(0, item);
                        notificationItemAdapter.notifyDataSetChanged();
                        storeList(context, notificationItemAdapter.data);
                    }
                }, new IntentFilter(MyFirebaseMessagingService.ACTION_UPDATE_LIST)
        );

        endConnectionRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                connManager.unregisterNetworkCallback(networkCallback);
            }
        };
        registerReceiver(endConnectionRequestReceiver, new IntentFilter(ACTION_END_CONNECTION_REQUEST));

        if(notificationsArray.isEmpty())
            mainTextView.setText("You have no notifications.");
        else
            mainTextView.setVisibility(View.GONE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        switch (id){
            case R.id.action_registration_id:
                Intent intent = new Intent(this, RegistrationIdActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationsArray = loadList(this);
        notificationItemAdapter.notifyDataSetChanged();
        initializeListView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeList(this, notificationItemAdapter.data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)   {
        if (requestCode == 10 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            List<ScanResult> scanResults = wifiManager.getScanResults();
            showWifiListDialog(scanResults);
        }
    }

    private String getRegistrationKey(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return settings.getString(getString(R.string.registration_id), "");
    }

    public void sendData(final String ssid, final String password, boolean secure){
        NetworkRequest.Builder netBuilder = new NetworkRequest.Builder();
        netBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        NetworkRequest netRequest = netBuilder.build();
        networkCallback =  new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                wifiManager = null;
                wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                String connectedSsid = wifiInfo.getSSID();

                final Network aux = network;
                AsyncTask<String, Void, Boolean> asyncTask = new AsyncTask<String, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(String... params) {
                        String data = params[0];
                        String ssid = params[1];
                        String password = params[2];

                        String ip = "192.168.4.1";
                        int port = 7777;
                        try {
                            InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                            Socket socket = new Socket();
                            aux.bindSocket(socket);
                            socket.connect(socketAddress);

                            while(!socket.isConnected()) {

                            }

                            OutputStream out = socket.getOutputStream();
                            PrintWriter output = new PrintWriter(out);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            output.println("reg"+data);
                            output.flush();
                            String response = in.readLine();
                            if(response.equals("200")) {
                                output.println("net" + ssid);
                                output.flush();
                                response = in.readLine();
                                if(response.equals("200")) {
                                    boolean success = false;
                                    
                                    if(!success)
                                        output.println("pwd" + password);
                                    output.flush();
                                    response = in.readLine();
                                    if(response.equals("200")) {
                                        output.println("200");
                                        output.flush();
                                        out.close();
                                        output.close();
                                        socket.close();
                                    }
                                }
                            }
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
                    protected void onPostExecute(Boolean aBoolean) {
                        super.onPostExecute(aBoolean);
                        Intent intent = new Intent(MainActivity.ACTION_END_CONNECTION_REQUEST);
                        if(aBoolean)
                            Toast.makeText(MainActivity.this, "NodeMCU successfully configured", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Couldn't configure NodeMCU, please try again", Toast.LENGTH_SHORT).show();
                        sendBroadcast(intent);
                    }
                };
                String token = getRegistrationKey();
                asyncTask.execute(token, ssid, password);
            }
        };
        connManager.requestNetwork(netRequest, networkCallback);

    }

    private void showConnectWifiDialog(final ScanResult connectAp){

        final boolean connectSecure = connectAp.capabilities.contains("WEP") || connectAp.capabilities.contains("WPA");
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

        builderSingle.setTitle(connectAp.SSID);

        LayoutInflater inflater = getLayoutInflater();
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_connect_wifi, null);
        if(connectSecure) {
            builderSingle.setView(layout);
        }

        builderSingle.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setPositiveButton(getString(R.string.connect),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String connectPassword = "";
                        if(connectSecure) {
                            EditText etPassword = (EditText) layout.findViewById(R.id.et_dialog_pasword);
                            connectPassword = etPassword.getText().toString();
                            scanWifiNetworksNodeMCU(connectAp, connectPassword, true);
                        }else{
                            scanWifiNetworksNodeMCU(connectAp, connectPassword, false);
                        }
                    }
                });

        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    private void showWifiListDialog(List<ScanResult> results) {
        Collections.sort(results, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return rhs.level > lhs.level ? 1 : rhs.level < lhs.level ? -1 : 0;
            }
        });
        final WifiAdapter wifiAdapter = new WifiAdapter(this, R.layout.wifi_item, results);
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);



        builderSingle.setTitle("Select a network");
        builderSingle.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        unregisterWifiScanReceiver();
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(wifiAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = wifiAdapter.getItem(which).SSID;

                        showConnectWifiDialog(wifiAdapter.getItem(which));

                        unregisterWifiScanReceiver();
                    }
                });
        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    private void scanWifiNetworks(){

        if(!wifiManager.isWifiEnabled()){
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                    this);
            builderSingle.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            return;
                        }
                    });
            builderSingle.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            wifiManager.setWifiEnabled(true);
                        }
                    });
            builderSingle.setMessage("Wi-Fi is disabled. Do you want to enable it?");

            AlertDialog dialog = builderSingle.create();
            dialog.show();
        }


        registerWifiScanReceiver();
        boolean  started = wifiManager.startScan();
    }

    private void registerWifiScanReceiver(){
        registerReceiver(wifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void unregisterWifiScanReceiver(){
        unregisterReceiver(wifiScanReceiver);
    }

    private void initializeWifiScanReceiver(){
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},10);
                        //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

                    }else {
                        //do something, permission was previously granted; or legacy device
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        showWifiListDialog(scanResults);
                    }
                }
            }
        };
    }

    private void registerWifiScanReceiverNodeMCU(){
        registerReceiver(wifiScanReceiverNodeMCU,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void unregisterWifiScanReceiverNodeMCU(){
        unregisterReceiver(wifiScanReceiverNodeMCU);
    }

    private void initializeWifiScanReceiverNodeMCU(final ScanResult connectAp, final String connectPassword, final boolean connectSecure){
        wifiScanReceiverNodeMCU = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                    List<ScanResult> scanResults = wifiManager.getScanResults();
                    showWifiListDialogNodeMCU(scanResults, connectAp, connectPassword, connectSecure);
                }
            }
        };
    }

    private void showConnectWifiDialogNodeMCU(final ScanResult ap, final ScanResult connectAp, final String connectPassword, final boolean connectSecure){

        final boolean secure = ap.capabilities.contains("WEP") || ap.capabilities.contains("WPA");
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

        builderSingle.setTitle(ap.SSID);

        LayoutInflater inflater = getLayoutInflater();
        final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_connect_wifi, null);
        if(secure) {
            builderSingle.setView(layout);
        }

        builderSingle.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setPositiveButton(getString(R.string.connect),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = "";
                        if(secure) {
                            EditText etPassword = (EditText) layout.findViewById(R.id.et_dialog_pasword);
                            password = etPassword.getText().toString();
                            connectToWifi(connectAp, connectPassword, connectSecure, ap, password, true);
                        }else{
                            connectToWifi(connectAp, connectPassword, connectSecure, ap, password, false);
                        }
                    }
                });

        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    private void showWifiListDialogNodeMCU(List<ScanResult> results, final ScanResult connectAp, final String connectPassword, final boolean connnectSecure) {
        Collections.sort(results, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return rhs.level > lhs.level ? 1 : rhs.level < lhs.level ? -1 : 0;
            }
        });
        final WifiAdapter wifiAdapter = new WifiAdapter(this, R.layout.wifi_item, results);
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

        builderSingle.setTitle("Select a network to connect NodeMCU to");
        builderSingle.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(wifiAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = wifiAdapter.getItem(which).SSID;

                showConnectWifiDialogNodeMCU(wifiAdapter.getItem(which), connectAp, connectPassword, connnectSecure);

                unregisterWifiScanReceiverNodeMCU();
            }
        });
        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    private void scanWifiNetworksNodeMCU(ScanResult connectAp, String connectPassword, boolean connectSecure){
        if(!wifiManager.isWifiEnabled()){
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                    this);
            builderSingle.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            return;
                        }
                    });
            builderSingle.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            wifiManager.setWifiEnabled(true);
                        }
                    });
            builderSingle.setMessage("Wi-Fi is disabled. Do you want to enable it?");

            AlertDialog dialog = builderSingle.create();
            dialog.show();
        }

        initializeWifiScanReceiverNodeMCU(connectAp, connectPassword, connectSecure);
        registerWifiScanReceiverNodeMCU();
        boolean  started = wifiManager.startScan();
    }

    private void connectToWifi(ScanResult connectAp, String connectPassword, boolean connectSecure, ScanResult ap, String password, boolean secure){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", connectAp.SSID);

        if(connectSecure) {
            if (connectAp.capabilities.contains("WPA")) {
                wifiConfig.preSharedKey = String.format("\"%s\"", connectPassword);
            } else if (connectAp.capabilities.contains("WEP")) {
                wifiConfig.wepKeys[0] = String.format("\"%s\"", connectPassword);
                wifiConfig.wepTxKeyIndex = 0;
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            }
        }
        else{
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        boolean aaa = wifiManager.reconnect();

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration conf : list) {
            if(conf.networkId == netId){
                Log.d("Main", "arapol conf SSID: " + conf.SSID);
            }
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String connectedSsid = wifiInfo.getSSID();
        if(connectedSsid.equals("\"NodeMCU WiFi\"")){
            sendData(ap.SSID, password, secure);
        }
    }

    private void initializeListView(){
        notificationItemAdapter = new NotificationItemAdapter(this, R.layout.notification_list_item, notificationsArray);

        listView.setAdapter(notificationItemAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_selected, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode actionMode, MenuItem item) {
                final ActionMode mode = actionMode;
                switch (item.getItemId()) {
                    case R.id.delete:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        // Calls getSelectedIds method from ListViewAdapter Class
                                        SparseBooleanArray selected = notificationItemAdapter
                                                .getSelectedIds();
                                        // Captures all selected ids with a loop
                                        for (int i = (selected.size() - 1); i >= 0; i--) {
                                            if (selected.valueAt(i)) {
                                                NotificationItem selectedItem = notificationItemAdapter.getItem(selected.keyAt(i));

                                                // Remove selected items following the ids
                                                notificationItemAdapter.remove(selectedItem);
                                            }
                                        }

                                        notificationItemAdapter.notifyDataSetChanged();
                                        Log.d("Main", "dakitu delete items notificationsArray size: " + notificationsArray.size());
                                        storeList(MainActivity.this, notificationItemAdapter.data);
                                        // Close CAB
                                        if(notificationsArray.isEmpty())
                                            mainTextView.setText("You have no notifications.");
                                        else
                                            mainTextView.setVisibility(View.GONE);
                                        mode.finish();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Are you sure you want to delete " + notificationItemAdapter.getSelectedCount() + " notifications?")
                                .setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                notificationItemAdapter.removeSelection();
            }

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // Capture total checked items
                final int checkedCount = listView.getCheckedItemCount();
                // Set the CAB title according to total checked items
                mode.setTitle(checkedCount + " Selected");
                // Calls toggleSelection method from ListViewAdapter Class
                notificationItemAdapter.toggleSelection(position);
            }

        });

        notificationsArray = loadList(this);
        if(!notificationsArray.isEmpty())
            notificationItemAdapter.notifyDataSetChanged();
    }

    public void storeList(Context context, ArrayList data) {
        // used for storing arrayList in json format
        SharedPreferences settings;
        SharedPreferences.Editor editor;
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        editor = settings.edit();
        Gson gson = new Gson();
        String jsonList = gson.toJson(data);
        editor.putString("notificationsList", jsonList);
        editor.commit();
    }

    public ArrayList loadList(Context context) {
        // used for retrieving arraylist from json formatted string
        Type listOfObjects = new TypeToken<List<NotificationItem>>(){}.getType();
        SharedPreferences settings;
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        String json = settings.getString("notificationsList", "");
        if(!json.equals("")) {
            List<NotificationItem> list2 = new Gson().fromJson(json, listOfObjects);
            ArrayList<NotificationItem> result = new ArrayList<NotificationItem>(list2);
            return result;
        }
        return null;
    }
}
