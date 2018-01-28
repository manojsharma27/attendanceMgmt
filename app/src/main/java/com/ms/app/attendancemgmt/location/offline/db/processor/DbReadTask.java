package com.ms.app.attendancemgmt.location.offline.db.processor;

import com.ms.app.attendancemgmt.location.offline.ModelEntry;

import java.util.List;

/**
 * Defines a read task with actual query and response
 */
public interface DbReadTask {

    List<ModelEntry> process();

}
