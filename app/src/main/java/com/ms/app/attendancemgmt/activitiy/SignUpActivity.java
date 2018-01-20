package com.ms.app.attendancemgmt.activitiy;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.location.StoredLocationUploader;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LoginResponse;
import com.ms.app.attendancemgmt.service.FileHandler;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.MasterPinValidateCallback;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 192;
    private EmpSignUpTask signUpTask = null;

    // UI references.
    private EditText txtEmpId;
    private View mProgressView;
    private View mSignUpFormView;
    private String deviceId;
    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);


        txtEmpId = findViewById(R.id.txtEmpId);
        txtEmpId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.btnSignUp || id == EditorInfo.IME_NULL) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });

        Button btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });

        mSignUpFormView = findViewById(R.id.full_signup_form);
        mProgressView = findViewById(R.id.signup_progress);

        deviceId = Utility.readPref(SignUpActivity.this.getApplicationContext(), Constants.DEVICE_ID);
        if (StringUtils.isEmpty(deviceId)) {
            if (checkAndRequestDeviceIdPermission()) {
                populateDeviceId();
            }
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    // Code for fetching deviceId
    private void configureTelephonyManager() {
        if (null == telephonyManager) {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        }
    }

    private void populateDeviceId() {
        configureTelephonyManager();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            deviceId = telephonyManager.getDeviceId();
        }

        if (null == deviceId || deviceId.contains("000000")) {
            deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        if (!StringUtils.isEmpty(deviceId)) {
            Utility.writePref(SignUpActivity.this.getApplicationContext(), Constants.DEVICE_ID, deviceId);
        }
        Log.d(Constants.TAG, "DeviceId: " + deviceId);
    }

    private boolean checkAndRequestDeviceIdPermission() {
        if (ActivityCompat.checkSelfPermission(SignUpActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) SignUpActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sign_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        loadLoginActivityAndFinish();
    }

    private void loadLoginActivityAndFinish() {
        Intent loginIntent = new Intent(this.getApplicationContext(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignUp() {
        if (signUpTask != null) {
            return;
        }

        txtEmpId.setError(null);
        String empId = txtEmpId.getText().toString();
        boolean cancel = false;
        View focusView = null;

        if (!isEmpIdValid(empId)) {
            txtEmpId.setError(getString(R.string.error_short_pin));
            focusView = txtEmpId;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            if (StringUtils.isEmpty(Utility.getServiceUrl(getApplicationContext()))) {
                showProgress(false);
                Utility.showMessageDialog(SignUpActivity.this, "Service url not configured!", R.mipmap.wrong);
                txtEmpId.setError(getString(R.string.error_invalid_pin));
                txtEmpId.requestFocus();
                return;
            }

            if (Utility.checkInternetConnected(SignUpActivity.this)) {
                signUpTask = new EmpSignUpTask(empId);
                signUpTask.execute((Void) null);
            } else {
                showProgress(false);
            }
        }
    }

    private boolean isEmpIdValid(String empId) {
        return !TextUtils.isEmpty(empId);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSignUpFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous registration task used to signup
     * the user.
     */
    public class EmpSignUpTask extends AsyncTask<Void, Void, Boolean> {

        private final String empId;
        private String errorMsg;

        EmpSignUpTask(String empId) {
            this.empId = empId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }

            Response response = null;
            try {
                OkHttpClient client = new OkHttpClient();
                String finalUrl = Utility.getServiceUrl(getApplicationContext()) + String.format(Constants.SIGN_UP_ENDPOINT, empId);
                Request request = new Request.Builder()
                        .url(finalUrl)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();
                response = client.newCall(request).execute();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Exception while authenticating empId. ", e);
            }
            if (null == response || !response.isSuccessful()) {
                errorMsg = "Failed to connect to service.";
                return false;
            }

            return validateResponse(response);
        }

        @NonNull
        private Boolean validateResponse(Response response) {
            try {
                String respStr = response.body().string();
                LoginResponse loginResp = Utility.getObjectMapper().readValue(respStr, LoginResponse.class);
                if (null != loginResp &&
                        "Success".equals(loginResp.getStatus()) &&
                        !StringUtils.isEmpty(loginResp.getEmpid()) &&
                        !loginResp.getEmpid().equals("0")) {
                    return true;
                }
            } catch (IOException e) {
                Log.e(Constants.TAG, "Exception while parsing response :" + response.body(), e);
            }
            errorMsg = "Employee ID not registered.";
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            signUpTask = null;
            showProgress(false);

            if (success) {
                showSignUpSuccessDialog();
            } else {
                Utility.showMessageDialog(SignUpActivity.this, errorMsg, R.mipmap.wrong);
                txtEmpId.setError(getString(R.string.error_invalid_pin));
                txtEmpId.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            signUpTask = null;
            showProgress(false);
        }
    }

    private void showSignUpSuccessDialog() {
        Utility.toastMsg(getApplicationContext(), "Not supported yet");
//        Utility.showMessageDialog(SignUpActivity.this, "Thanks for registering on '" + getString(R.string.app_name) + "'.\n"
//                + "Please reach out to system administrator for login pin.", R.mipmap.right);
        txtEmpId.setText("");
    }

}
