package com.ibsalab.general.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ibsalab.general.AFService;
import com.exampleble.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by linktseng on 2018/3/22.
 */

public class AuthUtil {
    private static final String TAG = AuthUtil.class.getSimpleName();
    private static final String host = "sagecloud.ibsalab.com";
    private SharedPreferences preferences;
    private Context context;
    private OkHttpClient client;
    private AuthUtilsListener listener;

    public interface AuthUtilsListener {
        void onLoginSuccess();

        void onLoginFailed(String message);
    }

    public interface VerifyAccountCallback {
        void onResult(boolean isValid);

        void onError(String message);
    }

    public AuthUtil(Context context, String version, AuthUtilsListener listener) {
        this.context = context;
        this.listener = listener;

        int key =
                version.equals("professional") ?
                        R.string.preference_oauth_professional : R.string.preference_oauth_personal;
        preferences =
                context.getSharedPreferences(context.getString(key), Context.MODE_PRIVATE);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
        clientBuilder.readTimeout(5, TimeUnit.SECONDS);
        clientBuilder.writeTimeout(5, TimeUnit.SECONDS);
        client = clientBuilder.build();
    }

    public void login(final String email, String password) {
        login(email, password, true);
    }

    public void login(final String email, String password, boolean isSaveAccount) {
        if (listener == null) {
            return;
        }

        if (isSaveAccount) {
            setEmail(email);
        } else {
            clearEmail();
        }

        RequestBody postBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url("https://" + host + "/api/auth/token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "onFailure");
                e.printStackTrace();
                listener.onLoginFailed("internet error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                Log.d(TAG, response.toString());

                try {
                    if (!response.isSuccessful()) {
                        listener.onLoginFailed("response error");
                        throw new IOException("Unexpected code " + response);
                    }

                    ResponseBody responseBody = response.body();
                    assert responseBody != null;
                    JSONObject jObject = new JSONObject(responseBody.string());

                    if (jObject.getString("status").equals("error")) {
                        // Wrong pwd
                        listener.onLoginFailed("status error");
                        return;
                    }

                    String accessToken = jObject.getJSONObject("data").getString("access_token");
                    String refreshToken = jObject.getJSONObject("data").getString("refresh_token");

                    setAccessToken(accessToken);
                    setRefreshToken(refreshToken);
                    setCurrentAccount(email);

                    String department = getDepartmentFromServer(accessToken);
                    setDepartment(department);

                    listener.onLoginSuccess();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Boolean logout() {
        if (preferences != null) {
            setAccessToken("");
            removeCurrentAccount();

            return true;
        } else {
            return false;
        }
    }

    private void setEmail(String email) {
        preferences.edit().putString(context.getString(R.string.account), email).apply();
    }

    public String getEmail() {
        return preferences.getString(context.getString(R.string.account), "");
    }

    private void clearEmail() {
        preferences.edit().putString(context.getString(R.string.account), "").apply();
    }

    public void setAccessToken(String token) {
        preferences.edit().putString(context.getString(R.string.access_token), token).apply();
    }

    public String getAccessToken() {
        return preferences.getString(context.getString(R.string.access_token), "");
    }

    private void setRefreshToken(String token) {
        preferences.edit().putString(context.getString(R.string.refresh_token), token).apply();
    }

    public String getRefreshToken() {
        return preferences.getString(context.getString(R.string.refresh_token), "");
    }

    private String getDepartmentFromServer(String accessToken) {
        try {
            Request request = new Request.Builder()
                    .url("https://" + host + "/api/user/profile/")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            JSONObject jObject = new JSONObject(response.body().string());

            return jObject.getJSONObject("data").getJSONObject("department").getString("name");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void setDepartment(String department) {
        preferences.edit().putString(context.getString(R.string.department), department).apply();
    }

    public String getDepartment() {
        return preferences.getString(context.getString(R.string.department), "");
    }

    public void verifyAccount(
            String account, String password, final AuthUtil.VerifyAccountCallback callback
    ) {
        RequestBody postBody = new FormBody.Builder()
                .add("email", account)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url("https://" + host + "/api/auth/check")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    ResponseBody responseBody = response.body();
                    assert responseBody != null;

                    String str = responseBody.string();
                    JSONObject jObject = new JSONObject(str);

                    if (jObject.getString("status").equals("success")) {
                        boolean isValid = jObject.getJSONObject("data").getInt("check") == 1;
                        callback.onResult(isValid);
                    } else {
                        JSONArray jsonArray = jObject.getJSONObject("error").getJSONArray("message");

                        StringBuilder errorMsg = new StringBuilder();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            errorMsg.append(jsonArray.getString(i)).append("\n");
                        }

                        callback.onError(errorMsg.toString().trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setCurrentAccount(String account) {
        preferences.edit()
                .putString(context.getString(R.string.current_account), account)
                .apply();
    }

    public String getCurrentAccount() {
        return preferences.getString(context.getString(R.string.current_account), "");
    }

    private void removeCurrentAccount() {
        preferences.edit()
                .remove(context.getString(R.string.current_account))
                .apply();
    }

    public AFService getService(String accessToken) throws Exception {
        Request request = new Request.Builder()
                .url("https://" + host + "/api/user/services/")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        assert response.body() != null;
        JSONObject responseObject = new JSONObject(response.body().string());

        if (responseObject.optJSONObject("data") == null) {
            return null;
        }

        JSONObject jObject = responseObject.getJSONObject("data");

        return new AFService(jObject);
    }
}
