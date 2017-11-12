package com.ms.app.attendancemgmt.activitiy;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.ms.app.attendancemgmt.anotherservice.AnotherServiceHandlerManager;
import com.ms.app.attendancemgmt.location.LocationUtil.PermissionUtils;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

public class LocationRegisterer implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback, com.google.android.gms.location.LocationListener {

    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private Location mLastLocation;

    // Google client to interact with Google API

    private GoogleApiClient mGoogleApiClient;

    double latitude;
    double longitude;

    // list of permissions

    ArrayList<String> permissions = new ArrayList<>();
    PermissionUtils permissionUtils;

    boolean isPermissionGranted;

    private Context context;

    private boolean googleApiClientConnected;

    private int connectTries = 3;
    private AnotherServiceHandlerManager.MyCallback serviceCallback;

    public LocationRegisterer(Context context) {
        this.context = context;
    }

    public void doRegistration(String deviceId, String empId) {
        if (checkPlayServices()) {
            buildGoogleApiClient();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            while (!googleApiClientConnected && connectTries > 0) {
                try {
                    connectTries--;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            LocationRequest mLocationRequest = getLocationRequest();
            PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            pendingResult.await(1000, TimeUnit.MILLISECONDS);
        }


        getLocation();
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
//            populateAddress();
        } else {
            Log.e("LocationRegisterer", "Couldn't get the location. Make sure location is enabled on the device");
            return;
        }
        Log.i("LocationRegisterer", String.format("Located at (%s,%s)", longitude, latitude));
        registerAttendance(deviceId, empId);
    }

    private void registerAttendance(String deviceId, String empId) {
        Attendance attendance = new Attendance(empId);
        attendance.setTime(new Date());
        // TODO : get uniqueId for app installation
        attendance.setLat(latitude);
        attendance.setLon(longitude);
        attendance.setDevId(deviceId);
//        UpdateAttendance updateAttendance = new UpdateAttendance(this, attendance);
//        updateAttendance.register();
    }

    private void getLocation() {
        if (isPermissionGranted) {
            try {
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

//    public Address getAddress(double latitude, double longitude) {
//        Geocoder geocoder;
//        List<Address> addresses;
//        geocoder = new Geocoder(context, Locale.getDefault());
//
//        try {
//            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
//            return addresses.get(0);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

//    public void populateAddress() {
//
//        Address locationAddress = getAddress(latitude, longitude);
//
//        if (locationAddress != null) {
//            String address = locationAddress.getAddressLine(0);
//            String address1 = locationAddress.getAddressLine(1);
//            String city = locationAddress.getLocality();
//            String state = locationAddress.getAdminArea();
//            String country = locationAddress.getCountryName();
//            String postalCode = locationAddress.getPostalCode();
//
//            String currentLocation;
//
//            if (!TextUtils.isEmpty(address)) {
//                currentLocation = address;
//
//                if (!TextUtils.isEmpty(address1))
//                    currentLocation += "\n" + address1;
//
//                if (!TextUtils.isEmpty(city)) {
//                    currentLocation += "\n" + city;
//
//                    if (!TextUtils.isEmpty(postalCode))
//                        currentLocation += " - " + postalCode;
//                } else {
//                    if (!TextUtils.isEmpty(postalCode))
//                        currentLocation += "\n" + postalCode;
//                }
//
//                if (!TextUtils.isEmpty(state))
//                    currentLocation += "\n" + state;
//
//                if (!TextUtils.isEmpty(country))
//                    currentLocation += "\n" + country;
//
//                Log.i("Your Location: ", currentLocation);
//            }
//        }
//    }

    protected synchronized void buildGoogleApiClient() {

        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = getLocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        try {
//                            // Show the dialog by calling startResolutionForResult(),
//                            // and check the result in onActivityResult().
//                            status.startResolutionForResult(context, REQUEST_CHECK_SETTINGS);
//
//                        } catch (IntentSender.SendIntentException e) {
//                            // Ignore the error.
//                        }
                        Log.e(Constants.TAG, "LocationRegisterer.buildGoogleApiClient : This device is not supported.");
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });


    }

    @NonNull
    private LocationRequest getLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }


    /**
     * Method to verify google play services on the device
     */

    private boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(Constants.TAG, "LocationRegisterer.checkPlayServices() : This device is not supported.");
            return false;
        }
        return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(Constants.TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        googleApiClientConnected = true;
        getLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        googleApiClientConnected = false;
        mGoogleApiClient.connect();
    }


    // Permission check functions


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // redirects to utils
//        permissionUtils.onRequestPermissionsResult(, requestCode, permissions, grantResults);

    }

    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION", "GRANTED");
        isPermissionGranted = true;
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {
        Log.i("PERMISSION PARTIALLY", "GRANTED");
    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION", "DENIED");
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION", "NEVER ASK AGAIN");
    }

    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        String time = Utility.getTime();
        String successMsg = String.format(Constants.ATTENDANCE_REGISTERED_MSG, time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        Log.i(Constants.TAG, "Registration Response: " + (isSuccess ? successMsg : failedMsg));
        serviceCallback.processCallback();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.mLastLocation = location;
    }


    public void setServiceCallback(AnotherServiceHandlerManager.MyCallback serviceCallback) {
        this.serviceCallback = serviceCallback;
    }
}
