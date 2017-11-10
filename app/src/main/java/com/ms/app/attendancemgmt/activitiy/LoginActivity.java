package com.ms.app.attendancemgmt.activitiy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int LOGIN_DELAY = 1000;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText txtPin;
    private View mProgressView;
    private View mLoginFormView;
    private String empName;
    private String empId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtPin = findViewById(R.id.pin);
        txtPin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.btnLogin || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.full_login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        txtPin.setError(null);

        // Store values at the time of the login attempt.
        String pin = txtPin.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(pin)) {
            txtPin.setError(getString(R.string.error_short_pin));
            focusView = txtPin;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            if (StringUtils.isEmpty(Utility.getServiceUrl(getApplicationContext()))) {
                showProgress(false);
                Utility.showMessageDialog(LoginActivity.this, "Service url not configured!", R.mipmap.wrong);
                txtPin.setError(getString(R.string.error_invalid_pin));
                txtPin.requestFocus();
                return;
            }

            if (checkInternetConnected()) {
                mAuthTask = new UserLoginTask(pin);
                mAuthTask.execute((Void) null);
            }
        }
    }

    private boolean isPasswordValid(String pin) {
        return !TextUtils.isEmpty(pin) && pin.length() >= 4;
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

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String pin;
        private String errorMsg;

        UserLoginTask(String pin) {
            this.pin = pin;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            //TODO : remove me for sure *********************************************************************************
            if (true)
                return testDoInBkg();

            Response response = null;
            try {
                OkHttpClient client = new OkHttpClient();
                String finalUrl = Utility.getServiceUrl(getApplicationContext()) + String.format(Constants.AUTHENTICATE_PIN_ENDPOINT, pin);
                Request request = new Request.Builder()
                        .url(finalUrl)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();
                response = client.newCall(request).execute();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Exception while authenticating pin. ", e);
            }
            if (null == response || !response.isSuccessful()) {
                errorMsg = "Failed to connect to internet.";
                return false;
            }

            //TODO : replace parsing logic with actual one for sure *********************************************************************************
//            String respStr = "{\"Status\":\"Success\",\"Message\":\"David Patterson\"}";
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String respStr = response.body().string();
                JsonNode respNode = objectMapper.readTree(respStr);
                JsonNode statusNode = respNode.get("status");
                if (null != statusNode && statusNode.asText().equals("Success")) {
                    empName = respNode.get("name").asText();
                    empId = respNode.get("empid").asText();
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(Constants.EMP_ID, empId).apply();
                    return true;
                }
            } catch (IOException e) {
                Log.e(Constants.TAG, "Exception while parsing response :" + response.body(), e);
            }
            errorMsg = "Pin not registered.";
            return false;
        }

        @Nullable
        private Boolean testDoInBkg() {
            try {
                Thread.sleep(1000);
                empId = "9898";
                empName = "manoj";
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(Constants.EMP_ID, empId).apply();
                return true;
            } catch (InterruptedException e) {
            }

            if (true) {
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                loadRegisterAttendanceActivity();
            } else {
                Utility.showMessageDialog(LoginActivity.this, errorMsg, R.mipmap.wrong);
                txtPin.setError(getString(R.string.error_invalid_pin));
                txtPin.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void loadRegisterAttendanceActivity() {
        Intent registerAttendanceIntent = new Intent(LoginActivity.this, RegisterAttendanceActivity.class);
        registerAttendanceIntent.putExtra(Constants.EMP_ID, empId);
        registerAttendanceIntent.putExtra(Constants.EMP_NAME, empName);
        startActivity(registerAttendanceIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mitemSetService:
                loadGetMasterPinDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkInternetConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (null == activeNetworkInfo || !activeNetworkInfo.isConnected()) {
            Utility.showMessageDialog(LoginActivity.this, "No Internet Connection !", R.mipmap.img_sad_smiley);

            return false;
        }
        return Utility.ableToAccessInternet(2 * 1000);
    }


    private void loadGetMasterPinDialog() {
        AlertDialog.Builder dialogMasterPin = new AlertDialog.Builder(
                LoginActivity.this);
        dialogMasterPin.setTitle("Master Pin");
        final EditText txtMasterPin = new EditText(LoginActivity.this);
        txtMasterPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        dialogMasterPin.setView(txtMasterPin);
        dialogMasterPin.setPositiveButton("Next",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = txtMasterPin.getText().toString().trim();
                        if (value.isEmpty()
                                || !value.equals(Constants.MASTER_PIN)) {
                            Utility.toastMsg(getApplicationContext(),
                                    "Invalid Master Pin.");
                            Utility.showMessageDialog(LoginActivity.this,
                                    "Invalid Master Pin.", R.mipmap.wrong);
                            return;
                        }
                        dialog.cancel();
                        LoginActivity.this.loadSetServiceUrlDialog();
                    }
                });

        dialogMasterPin.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        dialogMasterPin.show();
    }

    protected void loadSetServiceUrlDialog() {
        AlertDialog.Builder dialogSetService = new AlertDialog.Builder(
                LoginActivity.this);
        dialogSetService.setTitle("Service Address");
        final EditText txtUrl = new EditText(LoginActivity.this);
        String prevServiceUrl = Utility.readFromSharedPref(LoginActivity.this, Constants.SERVICE_URL_PREF_KEY);
        txtUrl.setText(prevServiceUrl);
        dialogSetService.setView(txtUrl);
        dialogSetService.setPositiveButton("Update",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = txtUrl.getText().toString().trim();
                        if (value.isEmpty() || !value.startsWith("http")) {
                            Utility.toastMsg(getApplicationContext(),
                                    "Invalid URL entered.");
                            Utility.showMessageDialog(LoginActivity.this,
                                    "Invalid URL entered.", R.mipmap.wrong);
                            return;
                        }
                        Utility.saveSharedPref(getApplicationContext(),
                                Constants.SERVICE_URL_PREF_KEY, value);
                        Utility.toastMsg(
                                getApplicationContext(),
                                "Service address updated.\n"
                                        + Utility.getServiceUrl(getApplicationContext()));
                        Utility.showMessageDialog(
                                LoginActivity.this,
                                "Service address updated.\n"
                                        + Utility.getServiceUrl(getApplicationContext()),
                                R.mipmap.right);
                    }
                });

        dialogSetService.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        dialogSetService.show();
    }
}

