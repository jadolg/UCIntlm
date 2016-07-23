package uci.ucintlm.ui.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ToggleButton;

import uci.ucintlm.R;
import uci.ucintlm.service.service.NTLMProxyService;
import uci.ucintlm.service.service.ServiceUtils;
import uci.ucintlm.service.wifi_configuration.WifiAutoConfig;
import uci.ucintlm.ui.Security.Encripter;

public class UCIntlmDialog extends Activity {

    public static String notif1, notif2;
    private ToggleButton startButton;
    private EditText user;
    private EditText pass;
    private EditText domain;
    private EditText server;
    private EditText inputport;
    private EditText outputport;
    private EditText bypass;
    private CheckBox global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_antlm);
        startButton = (ToggleButton) findViewById(R.id.button1);
        user = (EditText) findViewById(R.id.euser);
        pass = (EditText) findViewById(R.id.epass);
        domain = (EditText) findViewById(R.id.edomain);
        server = (EditText) findViewById(R.id.eserver);
        inputport = (EditText) findViewById(R.id.einputport);
        outputport = (EditText) findViewById(R.id.eoutputport);
        bypass = (EditText) findViewById(R.id.ebypass);
        global = (CheckBox)findViewById(R.id.glob_proxy);

        notif1 = getString(R.string.notif1);
        notif2 = getString(R.string.notif2);

        loadConf();

        if (ServiceUtils.isMyServiceRunning(this)) {
            disableAll();
        } else {
            enableAll();
        }

    }

    private void loadConf() {
        SharedPreferences settings = getSharedPreferences("UCIntlm.conf",
                Context.MODE_PRIVATE);

        user.setText(settings.getString("user", ""));
        pass.setText(Encripter.decrypt(settings.getString("password", "")));
        domain.setText(settings.getString("domain", "uci.cu"));
        server.setText(settings.getString("server", "10.0.0.1"));
        inputport.setText(settings.getString("inputport", "8080"));
        outputport.setText(settings.getString("outputport", "8080"));
        bypass.setText(settings.getString("bypass", "127.0.0.1, localhost, *.uci.cu"));
        global.setChecked(settings.getBoolean("global_proxy",true));
        if (user.getText().toString().equals("")) {
            user.requestFocus();
        } else {
            pass.requestFocus();
        }
    }

    private void saveConf() {
        SharedPreferences settings = getSharedPreferences("UCIntlm.conf",
                Context.MODE_PRIVATE);
        Editor editor = settings.edit();
        editor.putString("user", user.getText().toString());
        editor.putString("password",
                Encripter.encrypt(pass.getText().toString()));
        editor.putString("domain", domain.getText().toString());
        editor.putString("server", server.getText().toString());
        editor.putString("inputport", inputport.getText().toString());
        editor.putString("outputport", outputport.getText().toString());
        editor.putString("bypass", bypass.getText().toString());
        editor.putBoolean("global_proxy",global.isChecked());
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //no menu needed at this time
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        //used to configure the form when it is restarted
        //if closed by the system
        super.onResume();
        if (ServiceUtils.isMyServiceRunning(this)) {
            disableAll();
        } else {
            enableAll();
        }
    }

    @SuppressLint("NewApi")
    private void disableAll() {
        //set the form to disable all fields and change the button to stop the service
        user.setEnabled(false);
        pass.setEnabled(false);
        domain.setEnabled(false);
        server.setEnabled(false);
        inputport.setEnabled(false);
        outputport.setEnabled(false);
        bypass.setEnabled(false);
        global.setEnabled(false);
        startButton.setChecked(true);
        startButton.setText(getString(R.string.stop));
    }

    @SuppressLint("NewApi")
    private void enableAll() {
        //set the form to introduce data and start the service
        user.setEnabled(true);
        pass.setEnabled(true);
        domain.setEnabled(true);
        server.setEnabled(true);
        inputport.setEnabled(true);
        outputport.setEnabled(true);
        bypass.setEnabled(true);
        global.setEnabled(true);
        startButton.setChecked(false);
        startButton.setText(getString(R.string.run));
    }

    public void clickRun(View arg0) {
        //run or stop the service
        Intent intent = new Intent(this, NTLMProxyService.class);

        if (domain.getText().toString().equals("")
                || server.getText().toString().equals("")
                || inputport.getText().toString().equals("")
                || outputport.getText().toString().equals("")) {

            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nodata),
                    Toast.LENGTH_SHORT).show();
            startButton.setChecked(false);
            return;

        }

        if (!ServiceUtils.isMyServiceRunning(this)) {
            saveConf();
            intent.putExtra("user", user.getText().toString());
            intent.putExtra("pass", pass.getText().toString());
            intent.putExtra("domain", domain.getText().toString());
            intent.putExtra("server", server.getText().toString());
            intent.putExtra("inputport", inputport.getText().toString());
            intent.putExtra("outputport", outputport.getText().toString());
            intent.putExtra("bypass", bypass.getText().toString());
            intent.putExtra("set_global_proxy", global.isChecked());

            startService(intent);
            UCIntlmWidget.actualizarWidget(this.getApplicationContext(),
                    AppWidgetManager.getInstance(this.getApplicationContext()),
                    "on");
            disableAll();

        } else {
            stopService(intent);
            enableAll();
            UCIntlmWidget.actualizarWidget(this.getApplicationContext(),
                    AppWidgetManager.getInstance(this.getApplicationContext()),
                    "off");
        }
    }

    public void clickAdv(View arg0) {
        //show advanced options
        ScrollView scroll = (ScrollView) findViewById(R.id.ascroll);
        CheckBox ch = (CheckBox) findViewById(R.id.checkBox1);
        if (ch.isChecked()) {
            scroll.setVisibility(View.VISIBLE);
        } else {
            scroll.setVisibility(View.GONE);
        }
    }

    public void clickShowPassword(View arg0) {
        //show password
        CheckBox ch = (CheckBox) findViewById(R.id.checkBoxPass);
        if (ch.isChecked()) {
            pass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        pass.setSelection(pass.length());
    }
}
