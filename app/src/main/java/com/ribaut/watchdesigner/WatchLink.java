package com.ribaut.watchdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

public final class WatchLink {
    private static final String INSTALL_PATH = "/ribaut/watchdesigner/install-default";
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
                    .addOnSuccessListener(id -> show(activity, "Envoi vers la Watch 8", "La demande d'installation du cadran Ribaut a été envoyée à « " + node.getDisplayName() + " ».\n\nRegarde maintenant la montre : le compagnon va rechercher le cadran WFF validé et tenter de l'activer."))
                    .addOnFailureListener(e -> show(activity, "Échec d'envoi", "La montre est détectée mais la demande d'installation n'a pas pu être envoyée.\n\n" + e.getMessage()));
            })
            .addOnFailureListener(e -> show(activity, "Échec Wear OS", "Impossible d'interroger la montre.\n\n" + e.getMessage()));
    }

    private static void show(Activity activity, String title, String message) {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
            .setTitle(title).setMessage(message).setPositiveButton("OK", null).show());
    }
}
