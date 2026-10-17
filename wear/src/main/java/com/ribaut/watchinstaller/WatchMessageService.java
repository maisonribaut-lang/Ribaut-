package com.ribaut.watchinstaller;

import android.content.Intent;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WatchMessageService extends WearableListenerService {
    public static final String PATH = "/ribaut/watchdesigner/ping";
    public static final String CHANNEL_PATH = "/ribaut/watchdesigner/watchface-apk";
    public static final String ACTION_LINK_OK = "com.ribaut.watchinstaller.LINK_OK";
    public static final String ACTION_WFF_RECEIVED = "com.ribaut.watchinstaller.WFF_RECEIVED";

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

    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        if (!CHANNEL_PATH.equals(channel.getPath())) return;
        Wearable.getChannelClient(this).getInputStream(channel).addOnSuccessListener(input -> {
            new Thread(() -> receiveWatchFace(channel, input)).start();
        });
    }

    private void receiveWatchFace(ChannelClient.Channel channel, InputStream input) {
        File target = new File(getFilesDir(), "incoming_watchface.apk");
        try (InputStream in = input; FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            out.flush();
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putString("incoming_wff", target.getAbsolutePath())
                .putLong("incoming_wff_time", System.currentTimeMillis()).apply();
            Intent i = new Intent(ACTION_WFF_RECEIVED);
            i.setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Exception e) {
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putString("incoming_wff_error", String.valueOf(e)).apply();
        } finally {
            Wearable.getChannelClient(this).close(channel);
        }
    }
}
