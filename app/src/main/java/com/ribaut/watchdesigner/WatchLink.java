package com.ribaut.watchdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

public final class WatchLink {
    private static final String PATH = "/ribaut/watchdesigner/ping";
    private WatchLink() {}

    public static void test(Activity activity) {
        Wearable.getNodeClient(activity).getConnectedNodes()
            .addOnSuccessListener(nodes -> {
                if (nodes.isEmpty()) {
                    show(activity, "Montre non détectée", "La Watch 8 n'est pas joignable par Wear OS. Vérifie qu'elle est connectée au téléphone.");
                    return;
                }
                Node node = nodes.get(0);
                byte[] data = "RIBAUT_WATCH_DESIGNER_V1".getBytes(StandardCharsets.UTF_8);
                Wearable.getMessageClient(activity).sendMessage(node.getId(), PATH, data)
                    .addOnSuccessListener(id -> show(activity, "Watch 8 détectée", "La liaison téléphone → montre fonctionne avec « " + node.getDisplayName() + " ».\n\nLa prochaine étape est l'envoi du cadran Watch Face Format."))
                    .addOnFailureListener(e -> show(activity, "Échec de liaison", "La montre est détectée mais le message n'a pas pu être envoyé.\n\n" + e.getMessage()));
            })
            .addOnFailureListener(e -> show(activity, "Échec Wear OS", "Impossible d'interroger la montre.\n\n" + e.getMessage()));
    }

    private static void show(Activity activity, String title, String message) {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
            .setTitle(title).setMessage(message).setPositiveButton("OK", null).show());
    }
}
