package com.ribaut.watchinstaller;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String SET_ACTIVE_PERMISSION = "com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE";
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.BLACK);

        TextView badge = new TextView(this);
        badge.setText("PR");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(22);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.rgb(40, 86, 135));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        badgeParams.bottomMargin = dp(14);
        root.addView(badge, badgeParams);

        TextView title = new TextView(this);
        title.setText("Ribaut Watch\nInstaller");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Compagnon Watch Face Push prêt");
        status.setTextColor(Color.rgb(126, 211, 145));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(12);
        root.addView(status, statusParams);

        TextView info = new TextView(this);
        info.setText("Cette application est la partie montre de Ribaut Watch Designer.\n\nLe transfert depuis le téléphone sera relié au bouton « Installer sur ma Watch 8 ».");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(12);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(12);
        root.addView(info, infoParams);

        setContentView(root);

        if (checkSelfPermission(SET_ACTIVE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{SET_ACTIVE_PERMISSION}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText("Autorisation d’activation accordée");
            } else {
                status.setText("Compagnon installé — activation manuelle possible");
                status.setTextColor(Color.rgb(255, 196, 92));
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
