package com.ibsalab.general;

import android.os.Environment;

public class Const {
    public static String appDirectory = Environment.getExternalStorageDirectory() + "/hr_an/";
    public static String rawDataPath = appDirectory + "RAWDATA/";
    public static String recordPath = appDirectory + "RECORD/";
    public static String uploadPath = appDirectory + "UPLOAD/";
    public static String backupPath = appDirectory + "BACKUP/";
    public static String ocrPicPath = appDirectory + "PIC/";
    public static String logcatDirectory = appDirectory + "LOGCAT/";
}
