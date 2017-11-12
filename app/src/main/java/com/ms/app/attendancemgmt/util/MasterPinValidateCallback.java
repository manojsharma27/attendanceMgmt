package com.ms.app.attendancemgmt.util;

import android.app.Activity;

/**
 * Handles contract to processCallback() after validating the master pin from user
 */
public interface MasterPinValidateCallback {
    void processMasterPinCallback(Activity activity);
}
