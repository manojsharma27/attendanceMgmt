package com.ms.app.attendancemgmt.location.offline;

public enum UploadStatus {

    PENDING(0), UPLOADED(1);

    private int code;

    UploadStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
