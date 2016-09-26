package nacho.tfg.blepresencetracker;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Nacho on 08/06/2016.
 */
public class WifiAdapter  extends ArrayAdapter<ScanResult> {

    Context context;

    public WifiAdapter(Context context, int resource, List<ScanResult> objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) context).getLayoutInflater().inflate(R.layout.wifi_item, parent, false);
        }
        ScanResult result = getItem(position);
        ((TextView) convertView.findViewById(R.id.wifi_name)).setText(formatSSID(result));
        boolean protectedWifi = result.capabilities.contains("WEP") || result.capabilities.contains("WPA");
        ((WifiImageView) convertView.findViewById(R.id.wifi_img)).setStateLocked(protectedWifi);
        ((ImageView) convertView.findViewById(R.id.wifi_img)).setImageLevel(getNormalizedLevel(result));
        return convertView;
    }

    private int getNormalizedLevel(ScanResult r) {
        int level = WifiManager.calculateSignalLevel(r.level,
                5);
        Log.e(getClass().getSimpleName(), "level " + level);
        return level;
    }

    private String formatSSID(ScanResult r) {
        if (r == null || r.SSID == null || "".equalsIgnoreCase(r.SSID.trim())) {
            return "no data";
        }
        return r.SSID.replace("\"", "");
    }
}
