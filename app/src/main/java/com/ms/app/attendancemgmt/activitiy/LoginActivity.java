package com.ms.app.attendancemgmt.activitiy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.model.LoginResponse;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.MasterPinValidateCallback;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A login screen that offers login via pin
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * TODO:
     * set mac id - on hold
     * 5min punch interval, by default - done
     * handle background location sync in bkg
     * close app on punch out
     * punch interval to be configured by login response
     */

    private static final int LOGIN_DELAY = 1000;
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

        // as per client need
        Utility.writePref(this.getApplicationContext(), Constants.SERVICE_URL_PREF_KEY, Constants.TEST_SERVICE_URL);

        // Open register attendance activity directly if user already punched in
        if (Utility.isPunchedIn(getApplicationContext())) {
            empId = Utility.readPref(this.getApplicationContext(), Constants.EMP_ID);
            empName = Utility.readPref(this.getApplicationContext(), Constants.EMP_NAME);
            loadRegisterAttendanceActivity();
        }

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

            if (Utility.checkInternetConnected(LoginActivity.this)) {
                mAuthTask = new UserLoginTask(pin);
                mAuthTask.execute((Void) null);
            } else {
                showProgress(false);
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

    public void showTestDialog(View view) {
        Utility.showCustomMessageDialog(this, String.format("%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s","This is test message... \n"), R.mipmap.right);
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
//            if (true)
//                return testDoInBkg();

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
                    empId = loginResp.getEmpid();
                    empName = loginResp.getEmpname();
                    Utility.writePref(getApplicationContext(), Constants.EMP_ID, empId);

                    // write punch interval to preferences
                    if (0 != loginResp.getInterval()) {
                        long punchIntervalMillis = TimeUnit.SECONDS.toMillis(loginResp.getInterval());
                        Utility.writePref(getApplicationContext(), Constants.PUNCHING_INTERVAL_KEY, String.valueOf(punchIntervalMillis));
                    }
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
                Utility.writePref(getApplicationContext(), Constants.EMP_ID, empId);
                return true;
            } catch (InterruptedException e) {
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
        finish();
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
                MasterPinValidateCallback callback = new MasterPinValidateCallback() {
                    @Override
                    public void processMasterPinCallback(Activity activity) {
                        loadSetServiceUrlDialog();
                    }
                };
                Utility.loadGetMasterPinDialog(LoginActivity.this, callback);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void loadSetServiceUrlDialog() {
        AlertDialog.Builder dialogSetService = new AlertDialog.Builder(
                LoginActivity.this);
        dialogSetService.setTitle("Service Url");
        final EditText txtUrl = new EditText(LoginActivity.this);
        String prevServiceUrl = Utility.getServiceUrl(LoginActivity.this); //readPref(LoginActivity.this, Constants.SERVICE_URL_PREF_KEY);
        txtUrl.setText(StringUtils.isBlank(prevServiceUrl) ? "" : prevServiceUrl);
        txtUrl.setLayoutParams(Utility.getLayoutParamsForDialogMsgText());
        dialogSetService.setView(txtUrl);
        dialogSetService.setPositiveButton("Update",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = txtUrl.getText().toString().trim();
                        if (value.isEmpty() || !value.startsWith("http")) {
                            Utility.showMessageDialog(LoginActivity.this,
                                    "Invalid URL entered.", R.mipmap.wrong);
                            return;
                        }
                        if (value.endsWith("/") || value.endsWith("\\")) {
                            Utility.showMessageDialog(LoginActivity.this,
                                    "URL should not end with '/' or '\\'.", R.mipmap.wrong);
                            return;
                        }

                        Utility.writePref(getApplicationContext(),
                                Constants.SERVICE_URL_PREF_KEY, value);
                        Utility.toastMsg(
                                getApplicationContext(),
                                "Service URL updated.");
                        Utility.showMessageDialog(
                                LoginActivity.this,
                                "Service URL updated.\n"
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

