package com.ribaut.watchinstaller;

import android.content.Intent;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WatchMessageService extends WearableListenerService {
    public static final String PATH = "/ribaut/watchdesigner/ping";
    public static final String ACTION_LINK_OK = "com.ribaut.watchinstaller.LINK_OK";

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (PATH.equals(event.getPath())) {
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putLong("last_ping", System.currentTimeMillis()).apply();
            Intent i = new Intent(ACTION_LINK_OK);
            i.setPackage(getPackageName());
            sendBroadcast(i);
        }
    }
}
