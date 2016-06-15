package com.example.xyzreader.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class XYZReaderSyncService extends Service {
    public final String LOG_TAG = XYZReaderSyncService.class.getSimpleName();
    private static final Object sSyncAdapterLock = new Object();
    private static XYZReaderSyncAdapter sXYZReaderSyncAdapter = null;

    @Override
    public void onCreate() {
        //Log.d(LOG_TAG, "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sXYZReaderSyncAdapter == null) {
                sXYZReaderSyncAdapter = new XYZReaderSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sXYZReaderSyncAdapter.getSyncAdapterBinder();
    }
}