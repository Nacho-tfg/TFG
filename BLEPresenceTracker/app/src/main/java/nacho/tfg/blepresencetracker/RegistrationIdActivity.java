package nacho.tfg.blepresencetracker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class RegistrationIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_id);
        TextView tvRegistrationId = (TextView) findViewById(R.id.tv_registration_id);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String registrationId = settings.getString(getString(R.string.registration_id), "");
        if(!registrationId.equals(""))
            tvRegistrationId.setText("Your Registration ID is:\n"+registrationId);
        else
            tvRegistrationId.setText("You still don't have a Registration ID");
    }
}
