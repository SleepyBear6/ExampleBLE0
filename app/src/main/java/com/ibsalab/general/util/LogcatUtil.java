package com.ibsalab.general.util;

import android.annotation.SuppressLint;

import com.ibsalab.general.Const;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogcatUtil {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void start() {
        @SuppressLint("SimpleDateFormat")
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        String timeStr = dateFormat.format(date);

        File appDirectory = new File(Const.appDirectory);
        File logDirectory = new File(Const.logcatDirectory);
        File logFile = new File(logDirectory, "logcat_general_" + timeStr + ".txt");

        // create folders
        if (!appDirectory.exists()) { appDirectory.mkdir(); }
        if (!logDirectory.exists()) { logDirectory.mkdir(); }

        // clear the previous logcat and then write the new one to the file
        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("logcat -f " + logFile);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
