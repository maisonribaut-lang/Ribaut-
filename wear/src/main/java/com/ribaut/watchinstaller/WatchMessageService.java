package com.ribaut.watchinstaller;

import android.content.Intent;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WatchMessageService extends WearableListenerService {
    public static final String PATH = "/ribaut/watchdesigner/ping";
    public static final String INSTALL_PATH = "/ribaut/watchdesigner/install-default";
    public static final String PACKAGE_PATH = "/ribaut/watchdesigner/watchface-package";
    public static final String TOKEN_PATH = "/ribaut/watchdesigner/watchface-token";
    public static final String CHANNEL_PATH = "/ribaut/watchdesigner/watchface-apk";
    public static final String ACTION_LINK_OK = "com.ribaut.watchinstaller.LINK_OK";
    public static final String ACTION_WFF_RECEIVED = "com.ribaut.watchinstaller.WFF_RECEIVED";
    private static final String PACKAGE_PREFIX = "com.ribaut.watchdesigner.watchfacepush.";

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (PATH.equals(event.getPath())) {
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putLong("last_ping", System.currentTimeMillis()).apply();
            Intent i = new Intent(ACTION_LINK_OK);
            i.setPackage(getPackageName());
            sendBroadcast(i);
            return;
        }
        if (PACKAGE_PATH.equals(event.getPath())) {
            String packageName = new String(event.getData(), StandardCharsets.UTF_8).trim();
            if (packageName.startsWith(PACKAGE_PREFIX)) {
                getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                    .putString("incoming_wff_package", packageName)
                    .putLong("incoming_wff_package_time", System.currentTimeMillis())
                    .apply();
            }
            return;
        }
        if (TOKEN_PATH.equals(event.getPath())) {
            String token = new String(event.getData(), StandardCharsets.UTF_8).trim();
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putString("incoming_wff_token", token)
                .putLong("incoming_wff_token_time", System.currentTimeMillis())
                .apply();
            return;
        }
        if (INSTALL_PATH.equals(event.getPath())) {
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putLong("last_install_request", System.currentTimeMillis()).apply();
            installDefault();
        }
    }

    private void installDefault() {
        WatchFaceActivator.activate(this, resultCallback());
    }

    private WatchFaceActivator.Callback resultCallback() {
        return new WatchFaceActivator.Callback() {
            @Override
            public void onActivated(String packageName, String slotId) {
                getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                    .putString("last_install_status", "activated")
                    .putString("last_watchface_package", packageName)
                    .putString("last_watchface_slot", slotId)
                    .remove("last_install_error")
                    .apply();
            }

            @Override
            public void onError(String message) {
                getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                    .putString("last_install_status", "error")
                    .putString("last_install_error", message)
                    .apply();
            }
        };
    }

    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        if (!CHANNEL_PATH.equals(channel.getPath())) return;
        Wearable.getChannelClient(this).getInputStream(channel)
            .addOnSuccessListener(input -> new Thread(() -> receiveWatchFace(channel, input)).start())
            .addOnFailureListener(e -> {
                getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                    .putString("incoming_wff_error", "Ouverture canal : " + e).apply();
                Wearable.getChannelClient(this).close(channel);
            });
    }

    private void receiveWatchFace(ChannelClient.Channel channel, InputStream input) {
        File target = new File(getFilesDir(), "incoming_watchface.apk");
        try (InputStream in = input; FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            out.flush();
            if (target.length() == 0L) throw new IllegalStateException("APK reçu vide");

            String packageName = getSharedPreferences("ribaut_watch_link", MODE_PRIVATE)
                .getString("incoming_wff_package", "");
            if (packageName == null || !packageName.startsWith(PACKAGE_PREFIX)) {
                throw new IllegalStateException("Nom du package du cadran non reçu ou invalide");
            }
            String token = getSharedPreferences("ribaut_watch_link", MODE_PRIVATE)
                .getString("incoming_wff_token", "");
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalStateException("Jeton de validation du cadran non reçu");
            }

            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putString("incoming_wff", target.getAbsolutePath())
                .putLong("incoming_wff_time", System.currentTimeMillis())
                .putString("last_install_status", "installing_received")
                .remove("incoming_wff_error")
                .apply();

            WatchFaceActivator.installReceived(this, target, packageName, token, resultCallback());

            Intent i = new Intent(ACTION_WFF_RECEIVED);
            i.setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Exception e) {
            getSharedPreferences("ribaut_watch_link", MODE_PRIVATE).edit()
                .putString("last_install_status", "error")
                .putString("incoming_wff_error", String.valueOf(e))
                .putString("last_install_error", String.valueOf(e))
                .apply();
        } finally {
            Wearable.getChannelClient(this).close(channel);
        }
    }
}
