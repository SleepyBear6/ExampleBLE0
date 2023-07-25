package com.ibsalab.general.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.exampleble.R;
import com.ibsalab.general.util.AuthUtil;

/**
 * Created by linktseng on 2018/7/4.
 */

public class LoginActivityPersonal extends AppCompatActivity {
    private final static String TAG = LoginActivityPersonal.class.getSimpleName();

    private final int PERMISSION_REQUEST = 0xa01;
    private String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private EditText accountEditText;
    private EditText pwdEditText;
    private CheckBox saveAccountCheckBox;
    private Button loginButton;

    private AuthUtil authUtil;

    private Dialog progressDialog;
    private TextView progressMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_personal);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        initComponent();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startSettingActivity();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initComponent() {
        authUtil = new AuthUtil(this, "personal", authUtilsListener);

        progressDialog = new Dialog(LoginActivityPersonal.this, R.style.progress_dialog);
        progressDialog.setContentView(R.layout.dialog);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressMsg = progressDialog.findViewById(R.id.id_tv_loadingmsg);

        // setup ui component
        accountEditText = findViewById(R.id.accountEditText);
        accountEditText.setText(authUtil.getEmail());
        pwdEditText = findViewById(R.id.pwdEditText);
        //pwdEditText.setText(authUtil.getPwd());
        saveAccountCheckBox = findViewById(R.id.saveAccountCheckBox);

        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (accountEditText.getText().toString().equals(""))
                    Toast.makeText(LoginActivityPersonal.this, "Please input your email", Toast.LENGTH_LONG).show();
                else if (pwdEditText.getText().toString().equals(""))
                    Toast.makeText(LoginActivityPersonal.this, "Please input your password", Toast.LENGTH_LONG).show();
                else {
                    showSyncDialog("Log in ...");
                    authUtil.login(accountEditText.getText().toString(), pwdEditText.getText().toString(), saveAccountCheckBox.isChecked());
                }
            }
        });

        findViewById(R.id.loginButton).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return false;
            }
        });
    }

    public void startSettingActivity() {
        cancelSyncDialog();
        /*
        Intent intent = new Intent(LoginActivityPersonal.this, MainActivity.class);
        startActivity(intent);
        */
        finish();
    }

    public void showSyncDialog(String text) {
        progressMsg.setText(text);
        progressDialog.show();
    }

    public void cancelSyncDialog() {
        progressDialog.cancel();
    }

    AuthUtil.AuthUtilsListener authUtilsListener = new AuthUtil.AuthUtilsListener() {
        @Override
        public void onLoginSuccess() {
            startSettingActivity();
        }

        @Override
        public void onLoginFailed(final String message) {
            Log.d(TAG, message);

            runOnUiThread(new Runnable() {
                public void run() {
                    if (message.equals("internet error")) {
                        new AlertDialog.Builder(LoginActivityPersonal.this)
                                .setMessage("未連接網路或伺服器錯誤，請檢查網路連線是否可用。")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(LoginActivityPersonal.this)
                                .setMessage("帳號或密碼錯誤，請檢查後重試。")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                    
                    cancelSyncDialog();
                }
            });
        }
    };
}
