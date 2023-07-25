package com.ibsalab.general.util;

import android.content.Context;
import android.util.Log;

import com.ibsalab.general.Const;
import com.mitac.ble.ECGRawInfo;
import com.mitac.ble.MitacApi;
import com.mitac.ble.MitacBleDevice;
import com.mitac.ble.SampleGattAttributes;
import com.mitac.callback.MitacBLEStateChangeCallback;
import com.mitac.callback.MitacDidConnectCallback;
import com.mitac.callback.MitacDidDisconnectCallback;
import com.mitac.callback.MitacGetECGRawDataCallback;
import com.mitac.callback.MitacSetInfoCallback;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class BluetoothUtil {
    private static final String TAG = "Bluetooth";

    private MitacApi mApi;
    private int syncTotalCount = 0;
    private AtomicInteger syncedCount = new AtomicInteger();

    private Date mDate;

    private BluetoothUtilListener listener;

    public interface BluetoothUtilListener {
        void onConnect();

        void onSyncBegin(int syncTotalCount);

        void onSyncProgress(int syncedCount, int syncTotalCount, int progress, ECGRawInfo ecgRawInfo);

        void onSyncFinish(int syncedCount, int syncTotalCount);

        void onDisconnect();

        void onError(Error error);
    }

    public BluetoothUtil(Context context, BluetoothUtilListener listener) {
        mApi = MitacApi.getSharedInstance(context);
        if (mApi == null) {
            Log.e(TAG, "#####mApi==null####");
        }

        this.listener = listener;
        mDate = new Date();

        mApi.setFolderPath(Const.rawDataPath, Const.recordPath);
    }

    public boolean sync(String address) {
        if (mApi.getConnectStatus() == SampleGattAttributes.EConnectStatus.STATE_CONNECTED) {
            Log.d(TAG, "Already connected, abort");
            return false;
        }

        Log.d(TAG, "Connect to " + address);

        mApi.connectToDevice(
                address,
                new MitacDidConnectCallback() {
                    @Override
                    public void didConnectToWristBand(
                            final MitacBleDevice device, final Error error
                    ) {
                        Log.d(TAG, "didConnectToWristBand, error: " + error);
                        listener.onConnect();

                        mApi.registerBLEStatusChangeReceiver(new MitacBLEStateChangeCallback() {
                            @Override
                            public void onConnectionStateChange(
                                    final SampleGattAttributes.EConnectStatus state,
                                    final Error error
                            ) {
                                if (error != null) {
                                    listener.onError(error);
                                    return;
                                }

                                if (state == SampleGattAttributes.EConnectStatus.STATE_DISCONNECTED) {
                                    Log.d(TAG, "Disconnected");
                                    listener.onError(new Error("BAND_DISCONNECTED"));
                                }
                            }
                        });

                        updateTime();
                    }
                });
        return true;
    }

    private void updateTime() {
        TimeZone timeZone = TimeZone.getDefault();
        timeZone.setRawOffset(28800 * 1000);
        mDate = GregorianCalendar.getInstance().getTime();
        mApi.updateDateTimeForGoldenEye(mDate, timeZone, false, false, true, new MitacSetInfoCallback() {
            @Override
            public void didReceiveSetInfoFeedback(final Error error) {
                if (error != null) {
                    listener.onError(error);
                    return;
                }

                syncEcgData();
            }
        });
    }

    private void syncEcgData() {
        syncedCount.set(0);

        mApi.requestECGRaw(new MitacGetECGRawDataCallback() {
            @Override
            public void didGetECGRawDataBegin(final int totalCount, final Error error) {
                syncTotalCount = totalCount;
                Log.d(TAG, "didGetECGRawDataBegin totalNumber: " + String.valueOf(totalCount));
                if (error != null) {
                    Log.d(TAG, "didGetECGRawDataBegin error: " + error.toString());
                    listener.onError(error);
                }

                listener.onSyncBegin(syncTotalCount);
            }

            @Override
            public void didGetECGRawDataReceived(
                    final int lastCallbackIndex, final ECGRawInfo ecgRawInfo, final Error error
            ) {
                Log.d(TAG, "didGetECGRawDataReceived");
                syncedCount.addAndGet(1);

                if (error != null) {
                    listener.onError(error);
                } else if (ecgRawInfo == null) {
                    listener.onError(new Error("ECG_RAW_INFO_NULL"));
                }

                int progress = (int) (syncedCount.doubleValue() / syncTotalCount * 100);
                listener.onSyncProgress(syncedCount.get(), syncTotalCount, progress, ecgRawInfo);
                Log.d(TAG, "syncProgress: " + progress);
            }

            @Override
            public void didGetECGRawDataFinished(final Error error) {
                Log.d(TAG, "didGetECGRawDataFinished");
                if (error != null)
                    Log.d(TAG, "didGetECGRawDataFinished error: " + error.toString());

                listener.onSyncFinish(syncedCount.get(), syncTotalCount);
                Log.e(TAG, "sync data finish");

                // Finish data transfer
                disconnect();
            }
        });
    }

    public void disconnect() {
        if (mApi.getConnectStatus() != SampleGattAttributes.EConnectStatus.STATE_CONNECTED) {
            return;
        }

        mApi.disconnect(new MitacDidDisconnectCallback() {
            @Override
            public void didDisconnectFromWristBand(final MitacBleDevice device, final Error error) {
                listener.onDisconnect();

                if (error != null) {
                    listener.onError(error);
                }
            }
        });
    }

    public boolean isBusy() {
        return mApi.getConnectStatus() != SampleGattAttributes.EConnectStatus.STATE_DISCONNECTED;
    }
}
