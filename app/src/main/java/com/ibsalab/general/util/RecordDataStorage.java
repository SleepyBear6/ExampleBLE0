package com.ibsalab.general.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibsalab.general.Record;

import java.util.ArrayList;

public class RecordDataStorage {
    private String key;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Gson GSON = new Gson();

    @SuppressLint("CommitPrefEdits")
    public RecordDataStorage(Context context, String version) {
        key = version.equals("professional") ? "RecordProfessional" : "RecordPersonal";
        sharedPreferences = context.getSharedPreferences("Record", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public ArrayList<Record> getList() {
        String gson = sharedPreferences.getString(key, null);

        if (gson == null) {
            return new ArrayList<>();
        }

        try {
            return GSON.fromJson(gson, new TypeToken<ArrayList<Record>>(){}.getType());
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Object stored with key " + key +
                    " is instance of other class");
        }
    }

    public int getNotUploadedCount() {
        int count = 0;

        ArrayList<Record> list = getList();

        for (Record record : list) {
            if (!record.isUploaded) {
                count++;
            }
        }

        return count;
    }

    public void saveList(ArrayList<Record> list) {
        editor.putString(key, GSON.toJson(list)).commit();
    }

    public void clearList() {
        saveList(new ArrayList<Record>());
    }

    public boolean hasNoteNeedUpdateRecord() {
        ArrayList<Record> list = getList();

        for (Record record : list) {
            if (record.isNoteNeedUpdate) {
                return true;
            }
        }

        return false;
    }

    public boolean hasNotUploadedFile() {
        ArrayList<Record> list = getList();

        for (Record record : list) {
            if (!record.isUploaded) {
                return true;
            }
        }

        return false;
    }
}
