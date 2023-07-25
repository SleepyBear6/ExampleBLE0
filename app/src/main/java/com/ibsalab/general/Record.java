package com.ibsalab.general;

import com.mitac.ble.ECGRawInfo;

import java.util.Date;

public class Record {
    // User input information
    public String idNumber;
    public String birthday;
    public String name;
    public String sex;
    public String analysisMode;
    public String measureDuration;
    public String serialNumber;
    public String startDate;
    public String site;

    // File information
    public String resultFilename;
    public String rawFilename;
    public Date startTime;
    public Date endTime;
    public int meanHR;
    public String note = "";
    public boolean isUploaded = false;
    public boolean isNoteNeedUpdate = false;
    public boolean isDecompressSuccess;
    public int ecgID = -1;

    public Record(
            String id, String birthday, String name, String sex, String analysisMode,
            String measureDuration, String serialNumber, String startDate, String site,
            String resultFilename, String rawFilename, ECGRawInfo ecgRawInfo
    ) {
        idNumber = id;
        this.birthday = birthday;
        this.name = name;
        this.sex = sex;
        this.analysisMode = analysisMode;
        this.measureDuration = measureDuration;
        this.serialNumber = serialNumber;
        this.startDate = startDate;
        this.site = site;
        this.resultFilename = resultFilename;
        this.rawFilename = rawFilename;
        this.startTime = new Date(ecgRawInfo.mEcgStartTime);
        this.endTime = new Date(ecgRawInfo.mEcgEndTime);
        this.meanHR = ecgRawInfo.mEcgMeanHR;
        this.isDecompressSuccess = ecgRawInfo.mEcgDecompressError == 0;
    }
}
