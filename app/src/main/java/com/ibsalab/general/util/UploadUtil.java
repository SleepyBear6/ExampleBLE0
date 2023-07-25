package com.ibsalab.general.util;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.ibsalab.general.Record;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadUtil {
    private String TAG = "UploadUtil";
    private static final String host = "sagecloud.ibsalab.com";

    private VersionUtil versionUtil;
    private RecordDataStorage recordDataStorage;

    private OkHttpClient client;

    private int uploadTotalCount = 0;
    private AtomicInteger uploadedCount = new AtomicInteger();

    private int updateNoteTotalCount = 0;
    private AtomicInteger updatedNoteCount = new AtomicInteger();

    private boolean clientIsBusy = false;

    private UploadUtilListener uploadUtilListener;
    private UpdateNoteListener updateNoteListener;

    public interface UploadUtilListener {
        void onUploadProgress(int uploadedCount, int uploadTotalCount, int progress);

        void onUploadFinish(int uploadedCount, int uploadTotalCount);

        void onError();
    }

    public interface UpdateNoteListener {
        void onUpdateProgress(int updatedCount, int updateTotalCount);

        void onUpdateFinish(int updatedCount, int updateTotalCount);

        void onError();
    }

    public UploadUtil(Context context, UploadUtilListener uploadUtilListener) {
        this(context, uploadUtilListener, null);
    }

    @SuppressLint("CommitPrefEdits")
    public UploadUtil(Context context, UploadUtilListener uploadUtilListener, UpdateNoteListener updateNoteListener) {
        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.connectTimeout(5, TimeUnit.SECONDS);
        b.readTimeout(30, TimeUnit.SECONDS);
        b.writeTimeout(30, TimeUnit.SECONDS);
        client = b.build();

        versionUtil = new VersionUtil(context);
        String version = versionUtil.isProVersion() ? "professional" : "personal";
        recordDataStorage = new RecordDataStorage(context, version);

        this.uploadUtilListener = uploadUtilListener;
        this.updateNoteListener = updateNoteListener;
    }

    public void uploadFiles(final String accessToken, final String uploadPath) {
        final ArrayList<Record> recordList = recordDataStorage.getList();

        uploadTotalCount = 0;
        for (Record record : recordList) {
            if (!record.isUploaded) {
                uploadTotalCount++;
            }
        }

        uploadedCount.set(0);
        clientIsBusy = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final Record record : recordList) {
                    if (record.isUploaded) {
                        continue;
                    }

                    final File file = new File(uploadPath + record.resultFilename);

                    MultipartBody.Builder builder = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("files", record.resultFilename, RequestBody.create(MediaType.parse(""), file))
                            .addFormDataPart("patient_IDNumber", record.idNumber)
                            .addFormDataPart("birthday", record.birthday);

                    if (versionUtil.isPersonalVersion()) {
                        builder = builder.addFormDataPart("sex", record.sex)
                                .addFormDataPart("patient_name", record.name)
                                .addFormDataPart("priority", record.analysisMode)
                                .addFormDataPart("duration", record.measureDuration)
                                .addFormDataPart("service_SN", record.serialNumber)
                                .addFormDataPart("start_date", record.startDate)
                                .addFormDataPart("site", record.site);
                    }

                    RequestBody postBody = builder.build();

                    Request request = new Request.Builder()
                            .url("https://" + host + "/api/user/ecgs")
                            .header("Accept", "application/json")
                            .header("Authorization", "Bearer " + accessToken)
                            .post(postBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.d(TAG, "onFailure");
                            uploadUtilListener.onError();
                            e.printStackTrace();

                            if (uploadedCount.addAndGet(1) == uploadTotalCount) {
                                clientIsBusy = false;
                                uploadUtilListener.onUploadFinish(uploadedCount.get(), uploadTotalCount);
                            }
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            synchronized (recordList) {
                                onUploadResponse(response, recordList, record);
                            }

                            if (uploadedCount.addAndGet(1) == uploadTotalCount) {
                                clientIsBusy = false;
                                uploadUtilListener.onUploadFinish(uploadedCount.get(), uploadTotalCount);
                            }
                        }
                    });

                    // Sleep 100ms to avoid exceeding 600 API calls per minute
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void onUploadResponse(
            @NonNull Response response, ArrayList<Record> recordList, Record record
    ) throws IOException {
        int progress = (int) ((uploadedCount.doubleValue() + 1) / uploadTotalCount * 100);
        Log.d(TAG, "uploadProgress: " + progress);

        uploadUtilListener.onUploadProgress(uploadedCount.get(), uploadTotalCount, progress);

        try {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful()) {
                Log.d(TAG, "onResponse: " + response.toString());
                uploadUtilListener.onError();
                throw new IOException("Unexpected code " + response);
            }

            assert responseBody != null;
            String str = responseBody.string();
            JSONObject jObject = new JSONObject(str);

            if (jObject.getString("status").equals("success")) {
                record.ecgID = jObject.getJSONObject("data").getInt("id");
                record.isUploaded = true;
            } else {
                uploadUtilListener.onError();
            }

            recordDataStorage.saveList(recordList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateNote(final String accessToken) {
        final ArrayList<Record> recordList = recordDataStorage.getList();

        updateNoteTotalCount = 0;
        for (Record record : recordList) {
            if (record.isNoteNeedUpdate) {
                updateNoteTotalCount++;
            }
        }

        updatedNoteCount.set(0);
        clientIsBusy = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final Record record : recordList) {
                    if (record.ecgID == -1 || !record.isNoteNeedUpdate) {
                        continue;
                    }

                    HashMap<String, String> map = new HashMap<>();
                    map.put("description", record.note);

                    JSONObject jsonObject = new JSONObject(map);
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    RequestBody body = RequestBody.create(JSON, jsonObject.toString());

                    Request request = new Request.Builder()
                            .url("https://" + host + "/api/user/ecgs/" + record.ecgID)
                            .header("Accept", "application/json")
                            .header("Authorization", "Bearer " + accessToken)
                            .put(body)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.d(TAG, "updateNote onFailure");
                            updateNoteListener.onError();
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            int progress =
                                    (int) ((updatedNoteCount.get() + 1) / updateNoteTotalCount * 100.0);
                            Log.d(TAG, "updateNoteProgress: " + progress);

                            updateNoteListener.onUpdateProgress(updatedNoteCount.get(), updateNoteTotalCount);

                            try {
                                ResponseBody responseBody = response.body();
                                if (!response.isSuccessful()) {
                                    Log.d(TAG, "updateNote onResponse: " + response.toString());

                                    updateNoteListener.onError();
                                    throw new IOException("Unexpected code " + response);
                                }

                                assert responseBody != null;
                                String str = responseBody.string();
                                JSONObject jObject = new JSONObject(str);

                                if (jObject.getString("status").equals("success")) {
                                    record.isNoteNeedUpdate = false;
                                } else {
                                    updateNoteListener.onError();
                                }

                                if (updatedNoteCount.addAndGet(1) == updateNoteTotalCount) {
                                    clientIsBusy = false;
                                    updateNoteListener.onUpdateFinish(
                                            updatedNoteCount.get(), updateNoteTotalCount
                                    );
                                }
                            } catch (JSONException e) {
                                updateNoteListener.onError();
                                e.printStackTrace();
                            }
                        }
                    });

                    // Sleep 100ms to avoid exceeding 600 API calls per minute
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public boolean isBusy() {
        return clientIsBusy;
    }
}
