package com.ribaut.watchdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class WatchLink {
    private static final String INSTALL_PATH = "/ribaut/watchdesigner/install-default";
    private static final String PACKAGE_PATH = "/ribaut/watchdesigner/watchface-package";
    private static final String TOKEN_PATH = "/ribaut/watchdesigner/watchface-token";
    private static final String CHANNEL_PATH = "/ribaut/watchdesigner/watchface-apk";
    private WatchLink() {}

    public static void test(Activity activity) {
        Wearable.getNodeClient(activity).getConnectedNodes()
            .addOnSuccessListener(nodes -> {
                if (nodes.isEmpty()) {
                    show(activity, "Montre non détectée", "La Watch 8 n'est pas joignable par Wear OS. Vérifie qu'elle est connectée au téléphone.");
                    return;
                }
                Node node = nodes.get(0);
                byte[] data = "INSTALL_RIBAUT_WFF".getBytes(StandardCharsets.UTF_8);
                Wearable.getMessageClient(activity).sendMessage(node.getId(), INSTALL_PATH, data)
                    .addOnSuccessListener(id -> show(activity, "Envoi vers la Watch 8", "La demande d'installation du cadran Ribaut a été envoyée à « " + node.getDisplayName() + " »."))
                    .addOnFailureListener(e -> show(activity, "Échec d'envoi", "La montre est détectée mais la demande d'installation n'a pas pu être envoyée.\n\n" + e.getMessage()));
            })
            .addOnFailureListener(e -> show(activity, "Échec Wear OS", "Impossible d'interroger la montre.\n\n" + e.getMessage()));
    }

    /** Sends a freshly generated and validated WFF APK to the watch. */
    public static void installGenerated(Activity activity, File apk, String packageName, String validationToken) {
        if (apk == null || !apk.isFile() || apk.length() == 0L) {
            show(activity, "Cadran non généré", "Le fichier du cadran est absent ou vide.");
            return;
        }
        if (packageName == null || !packageName.startsWith(activity.getPackageName() + ".watchfacepush.")) {
            show(activity, "Cadran invalide", "Le nom du package WFF est absent ou incorrect.");
            return;
        }
        if (validationToken == null || validationToken.trim().isEmpty()) {
            show(activity, "Cadran non validé", "Le jeton de validation du cadran est absent.");
            return;
        }

        Wearable.getNodeClient(activity).getConnectedNodes()
            .addOnSuccessListener(nodes -> {
                if (nodes.isEmpty()) {
                    show(activity, "Montre non détectée", "La Watch 8 n'est pas joignable par Wear OS.");
                    return;
                }
                Node node = nodes.get(0);
                byte[] pkg = packageName.trim().getBytes(StandardCharsets.UTF_8);
                byte[] token = validationToken.trim().getBytes(StandardCharsets.UTF_8);
                Wearable.getMessageClient(activity).sendMessage(node.getId(), PACKAGE_PATH, pkg)
                    .addOnSuccessListener(pkgId -> Wearable.getMessageClient(activity).sendMessage(node.getId(), TOKEN_PATH, token)
                        .addOnSuccessListener(tokenId -> sendApk(activity, node, apk))
                        .addOnFailureListener(e -> show(activity, "Échec validation", "Le jeton du cadran n'a pas pu être envoyé.\n\n" + e.getMessage())))
                    .addOnFailureListener(e -> show(activity, "Échec transfert", "Le nom du cadran n'a pas pu être envoyé.\n\n" + e.getMessage()));
            })
            .addOnFailureListener(e -> show(activity, "Échec Wear OS", "Impossible d'interroger la montre.\n\n" + e.getMessage()));
    }

    private static void sendApk(Activity activity, Node node, File apk) {
        Wearable.getChannelClient(activity).openChannel(node.getId(), CHANNEL_PATH)
            .addOnSuccessListener(channel -> Wearable.getChannelClient(activity).getOutputStream(channel)
                .addOnSuccessListener(output -> new Thread(() -> copyAndClose(activity, channel, output, apk, node.getDisplayName())).start())
                .addOnFailureListener(e -> {
                    Wearable.getChannelClient(activity).close(channel);
                    show(activity, "Échec transfert", "Impossible d'ouvrir le flux vers la montre.\n\n" + e.getMessage());
                }))
            .addOnFailureListener(e -> show(activity, "Échec transfert", "Impossible d'ouvrir le canal vers la montre.\n\n" + e.getMessage()));
    }

    private static void copyAndClose(Activity activity, ChannelClient.Channel channel, OutputStream output, File apk, String watchName) {
        try (FileInputStream in = new FileInputStream(apk); OutputStream out = output) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            out.flush();
            show(activity, "Cadran envoyé", "Le cadran validé a été transmis à « " + watchName + " ». La montre va maintenant l'installer et l'activer.");
        } catch (Exception e) {
            show(activity, "Échec transfert", "Le transfert du cadran a échoué.\n\n" + e.getMessage());
        } finally {
            Wearable.getChannelClient(activity).close(channel);
        }
    }

    private static void show(Activity activity, String title, String message) {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
            .setTitle(title).setMessage(message).setPositiveButton("OK", null).show());
    }
}
