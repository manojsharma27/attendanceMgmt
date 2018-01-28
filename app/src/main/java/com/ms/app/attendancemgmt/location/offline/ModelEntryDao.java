package com.ms.app.attendancemgmt.location.offline;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ModelEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ModelEntry modelEntry);

    @Update
    int update(ModelEntry modelEntry);

    @Delete
    int delete(ModelEntry modelEntry);

    @Query("SELECT * FROM model_entry WHERE entry_id = :entryId")
    ModelEntry getById(String entryId);

    @Query("SELECT * FROM model_entry")
    List<ModelEntry> getAllEntries();

    @Query("SELECT * FROM model_entry WHERE upload_timestamp > :uploadTimestamp")
    List<ModelEntry> entriesAfterTimestamp(Long uploadTimestamp);

    @Query("SELECT * FROM model_entry WHERE upload_status = :uploadStatus order by upload_timestamp")
    List<ModelEntry> entriesWithStatus(int uploadStatus);
}
