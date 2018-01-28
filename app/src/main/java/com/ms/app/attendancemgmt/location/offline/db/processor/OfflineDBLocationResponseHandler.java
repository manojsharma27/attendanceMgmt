package com.ms.app.attendancemgmt.location.offline.db.processor;

import com.ms.app.attendancemgmt.location.offline.ModelEntry;

import java.util.List;

/**
 * Defines contract to handle the model entries fetched from offline DB
 */
public interface OfflineDBLocationResponseHandler {

    void handleDbLocationResponse(List<ModelEntry> entries);
}
