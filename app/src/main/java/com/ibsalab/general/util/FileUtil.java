package com.ibsalab.general.util;

import android.annotation.SuppressLint;

import com.ibsalab.general.Record;
import com.mitac.ble.ECGRawInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.ibsalab.general.Const.backupPath;
import static com.ibsalab.general.Const.uploadPath;

public class FileUtil {
    private static final String TAG = "FileUtil";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createFolders(String rawDataPath, String uploadPath, String backupPath) {
        File file = new File(rawDataPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(uploadPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(backupPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(backupPath + "/RAW");
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void processFile(
            String deviceName, ECGRawInfo ecgRawInfo, RecordDataStorage recordDataStorage,
            String id, String birthday
    ) {
        processFile(
                deviceName, ecgRawInfo, recordDataStorage, id, birthday, "", "", "", "", "", "", "");
    }

    /**
     * Check if the folder has raw data file from wrist which haven't translate to z2b file.
     * Raw data file name example:
     * <p>
     * 2018_04_13_17_57_02_ECG_RAW.txt
     * 2018_04_13_17_57_02_ECG_RESULT.txt
     * 2018_04_13_17_57_02_ECG_RESULT_OPT.txt
     */
    @SuppressLint("SimpleDateFormat")
    public static void processFile(
            String deviceName, ECGRawInfo ecgRawInfo, RecordDataStorage recordDataStorage,
            String id, String birthday, String name, String sex, String analysisMode,
            String measureDuration, String serialNumber, String startDate, String site
    ) {
        ArrayList<Record> recordList = recordDataStorage.getList();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        try {
            Date date = new Date(ecgRawInfo.mEcgStartTime);
            String timeString = dateFormat.format(date);
            String resultFilename = deviceName + "_" + timeString + ".z2b";
            String rawFilename = deviceName + "_" + timeString + "_ECG_RAW.txt";

            // type: RESULT, RAW, OPT
            File resultFile = new File(ecgRawInfo.mEcgResultFileName);
            File rawFile = new File(ecgRawInfo.mEcgRawFileName);

            // RESULT
            if (!haveSameFile(backupPath, resultFilename)) {
                copyFile(resultFile, new File(backupPath + resultFilename));
                copyFile(resultFile, new File(uploadPath + resultFilename));
            }

            // RAW
            if (!haveSameFile(backupPath + "RAW", rawFilename)) {
                copyFile(rawFile, new File(backupPath + "RAW/" + rawFilename));
            }

            // Add to record list
            Record record =
                    new Record(
                            id, birthday, name, sex, analysisMode, measureDuration,
                            serialNumber, startDate, site, resultFilename, rawFilename, ecgRawInfo);
            recordList.add(record);

            // Save record list
            recordDataStorage.saveList(recordList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private static Boolean haveSameFile(String path, String fileName) {
        File file = new File(path + "/" + fileName);
        return file.exists();
    }

    public static boolean isAllZeroFiles(String path) {
        File uploadDataDir = new File(path);
        File[] files = uploadDataDir.listFiles();
        String line;
        int zeroFileCount = 0;

        for (File file : files) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                double sum = 0;
                while ((line = br.readLine()) != null) {
                    sum += Math.abs(Double.valueOf(line));
                }

                if (sum == 0) {
                    zeroFileCount++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return (zeroFileCount == files.length) && (files.length != 0);
    }
}
