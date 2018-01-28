package com.ms.app.attendancemgmt.location.storedupload;

import android.content.Context;

import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;

/**
 * Checks if there are any stored locations when internet is connected
 * If locations are present, uploads them to server one at a time
 */
public abstract class StoredLocationUploader implements ServerUpdateResponseHandler {

    protected Context context;

    public StoredLocationUploader(Context context) {
        this.context = context;
    }

    public abstract void checkLocationsAndUpload();

}
