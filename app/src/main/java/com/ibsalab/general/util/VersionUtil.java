package com.ibsalab.general.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.exampleble.R;

public class VersionUtil {
    private Context _context;
    private SharedPreferences _preferences;
    private SharedPreferences.Editor _editor;

    public VersionUtil(Context context) {
        _context = context;
        _preferences =
                _context.getSharedPreferences(
                        _context.getString(R.string.preference_app_version), Context.MODE_PRIVATE);
        _editor = _preferences.edit();
    }

    private String getVersion() {
        return _preferences.getString(_context.getString(R.string.app_version), "");
    }

    public boolean isVersionSet() {
        return !getVersion().equals("");
    }

    public boolean isProVersion() {
        return getVersion().equals("professional");
    }

    public boolean isPersonalVersion() {
        return getVersion().equals("personal");
    }

    public void setProVersion() {
        _editor.putString(_context.getString(R.string.app_version), "professional").apply();
    }

    public void setPersonalVersion() {
        _editor.putString(_context.getString(R.string.app_version), "personal").apply();
    }

    public void resetVersion() {
        _editor.remove(_context.getString(R.string.app_version)).apply();
    }
}
