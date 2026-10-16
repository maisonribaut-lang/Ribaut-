package com.ribaut.watchdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 101;
    private static final int[] HAND_COLORS = {
            Color.WHITE, Color.rgb(255, 120, 40), Color.RED,
            Color.CYAN, Color.rgb(120, 255, 120)
    };
    private static final int[] TEXT_COLORS = {
            Color.WHITE, Color.LTGRAY, Color.rgb(255, 180, 50), Color.CYAN, Color.GREEN
    };
    private static final int[] BG_COLORS = {
            Color.BLACK, Color.rgb(24, 24, 30), Color.rgb(20, 35, 48),
            Color.rgb(48, 24, 24), Color.rgb(25, 45, 28)
    };

    private SharedPreferences prefs;
    private WatchPreview preview;
    private SeekBar bgSeek, handSeek, infoSeek;
    private CheckBox secondsBox, dateBox, batteryBox;
    private Spinner modeSpinner;
    private Button activeBtn, standbyBtn, handColorBtn, textColorBtn, bgColorBtn;
    private boolean standby = false;
    private int handColorIndex = 0, textColorIndex = 0, bgColorIndex = 0;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ribaut_watch_designer", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(24));
        root.setBackgroundColor(Color.rgb(18, 18, 22));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Ribaut Watch Designer", 22, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, matchWrap());

        TextView subtitle = text("Galaxy Watch 8 · éditeur épuré", 14, false);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle, matchWrap());

        preview = new WatchPreview(this);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(320));
        pp.setMargins(0, dp(8), 0, dp(8));
        root.addView(preview, pp);

        LinearLayout profileRow = horizontal();
        activeBtn = button("ACTIF");
        standbyBtn = button("VEILLE");
        profileRow.addView(activeBtn, weighted());
        profileRow.addView(standbyBtn, weighted());
        root.addView(profileRow, matchWrap());

        activeBtn.setOnClickListener(v -> switchProfile(false));
        standbyBtn.setOnClickListener(v -> switchProfile(true));

        root.addView(section("Type de cadran"));
        modeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Analogique", "Numérique"});
        modeSpinner.setAdapter(adapter);
        root.addView(modeSpinner, matchWrap());
        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!loading) {
                    prefs.edit().putInt("mode", position).apply();
                    applyToPreview();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        Button photoBtn = button("Choisir une photo de fond");
        root.addView(photoBtn, matchWrap());
        photoBtn.setOnClickListener(v -> choosePhoto());

        root.addView(section("Apparence du mode sélectionné"));
        bgSeek = addSlider(root, "Éclairage visuel du fond", 0, 100);
        handSeek = addSlider(root, "Aiguilles / chiffres", 0, 100);
        infoSeek = addSlider(root, "Texte / informations", 0, 100);

        secondsBox = check("Afficher les secondes");
        dateBox = check("Afficher la date");
        batteryBox = check("Afficher la batterie");
        root.addView(secondsBox, matchWrap());
        root.addView(dateBox, matchWrap());
        root.addView(batteryBox, matchWrap());

        LinearLayout colorRow1 = horizontal();
        bgColorBtn = button("Fond");
        handColorBtn = button("Aiguilles");
        colorRow1.addView(bgColorBtn, weighted());
        colorRow1.addView(handColorBtn, weighted());
        root.addView(colorRow1, matchWrap());

        textColorBtn = button("Couleur du texte");
        root.addView(textColorBtn, matchWrap());

        bgColorBtn.setOnClickListener(v -> {
            bgColorIndex = (bgColorIndex + 1) % BG_COLORS.length;
            saveProfile(); applyToPreview(); updateColorButtons();
        });
        handColorBtn.setOnClickListener(v -> {
            handColorIndex = (handColorIndex + 1) % HAND_COLORS.length;
            saveProfile(); applyToPreview(); updateColorButtons();
        });
        textColorBtn.setOnClickListener(v -> {
            textColorIndex = (textColorIndex + 1) % TEXT_COLORS.length;
            saveProfile(); applyToPreview(); updateColorButtons();
        });

        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!loading) applyToPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { saveProfile(); }
        };
        bgSeek.setOnSeekBarChangeListener(seekListener);
        handSeek.setOnSeekBarChangeListener(seekListener);
        infoSeek.setOnSeekBarChangeListener(seekListener);

        View.OnClickListener optionListener = v -> { saveProfile(); applyToPreview(); };
        secondsBox.setOnClickListener(optionListener);
        dateBox.setOnClickListener(optionListener);
        batteryBox.setOnClickListener(optionListener);

        LinearLayout actionRow = horizontal();
        Button saveBtn = button("ENREGISTRER");
        Button resetBtn = button("RÉINITIALISER");
        actionRow.addView(saveBtn, weighted());
        actionRow.addView(resetBtn, weighted());
        root.addView(actionRow, matchWrap());

        saveBtn.setOnClickListener(v -> {
            saveProfile();
            Toast.makeText(this, "Projet enregistré", Toast.LENGTH_SHORT).show();
        });
        resetBtn.setOnClickListener(v -> resetCurrentProfile());

        Button installBtn = button("INSTALLER SUR LA WATCH 8");
        LinearLayout.LayoutParams ip = matchWrap();
        ip.setMargins(0, dp(10), 0, 0);
        root.addView(installBtn, ip);
        installBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Installation Watch 8")
                .setMessage("Cette V1 est l'éditeur Android. L'envoi direct du cadran vers la Watch 8 sera ajouté dans l'étape suivante avec le module Wear OS et Watch Face Push.")
                .setPositiveButton("OK", null)
                .show());

        setContentView(scroll);
        loading = true;
        modeSpinner.setSelection(prefs.getInt("mode", 0));
        loading = false;
        loadSavedImage();
        loadProfile();
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            prefs.edit().putString("background_uri", uri.toString()).apply();
            preview.loadBitmap(uri);
        }
    }

    private void switchProfile(boolean toStandby) {
        if (standby == toStandby) return;
        saveProfile();
        standby = toStandby;
        loadProfile();
    }

    private String suffix() { return standby ? "_s" : "_a"; }

    private void loadProfile() {
        loading = true;
        String s = suffix();
        bgSeek.setProgress(prefs.getInt("bg" + s, standby ? 10 : 80));
        handSeek.setProgress(prefs.getInt("hands" + s, standby ? 40 : 100));
        infoSeek.setProgress(prefs.getInt("info" + s, standby ? 45 : 100));
        secondsBox.setChecked(prefs.getBoolean("seconds" + s, !standby));
        dateBox.setChecked(prefs.getBoolean("date" + s, true));
        batteryBox.setChecked(prefs.getBoolean("battery" + s, true));
        bgColorIndex = prefs.getInt("bgColor" + s, 0) % BG_COLORS.length;
        handColorIndex = prefs.getInt("handColor" + s, 0) % HAND_COLORS.length;
        textColorIndex = prefs.getInt("textColor" + s, 0) % TEXT_COLORS.length;
        loading = false;
        activeBtn.setEnabled(standby);
        standbyBtn.setEnabled(!standby);
        updateColorButtons();
        applyToPreview();
    }

    private void saveProfile() {
        if (loading || bgSeek == null) return;
        String s = suffix();
        prefs.edit()
                .putInt("bg" + s, bgSeek.getProgress())
                .putInt("hands" + s, handSeek.getProgress())
                .putInt("info" + s, infoSeek.getProgress())
                .putBoolean("seconds" + s, secondsBox.isChecked())
                .putBoolean("date" + s, dateBox.isChecked())
                .putBoolean("battery" + s, batteryBox.isChecked())
                .putInt("bgColor" + s, bgColorIndex)
                .putInt("handColor" + s, handColorIndex)
                .putInt("textColor" + s, textColorIndex)
                .apply();
    }

    private void resetCurrentProfile() {
        String s = suffix();
        prefs.edit()
                .remove("bg" + s).remove("hands" + s).remove("info" + s)
                .remove("seconds" + s).remove("date" + s).remove("battery" + s)
                .remove("bgColor" + s).remove("handColor" + s).remove("textColor" + s)
                .apply();
        loadProfile();
    }

    private void applyToPreview() {
        if (preview == null || bgSeek == null) return;
        preview.digital = modeSpinner.getSelectedItemPosition() == 1;
        preview.standby = standby;
        preview.bgPercent = bgSeek.getProgress();
        preview.handPercent = handSeek.getProgress();
        preview.infoPercent = infoSeek.getProgress();
        preview.showSeconds = secondsBox.isChecked();
        preview.showDate = dateBox.isChecked();
        preview.showBattery = batteryBox.isChecked();
        preview.bgColor = BG_COLORS[bgColorIndex];
        preview.handColor = HAND_COLORS[handColorIndex];
        preview.textColor = TEXT_COLORS[textColorIndex];
        preview.invalidate();
    }

    private void loadSavedImage() {
        String value = prefs.getString("background_uri", null);
        if (value != null) {
            try { preview.loadBitmap(Uri.parse(value)); } catch (Exception ignored) { }
        }
    }

    private void updateColorButtons() {
        tintButton(bgColorBtn, BG_COLORS[bgColorIndex]);
        tintButton(handColorBtn, HAND_COLORS[handColorIndex]);
        tintButton(textColorBtn, TEXT_COLORS[textColorIndex]);
    }

    private void tintButton(Button button, int color) {
        button.setTextColor(color);
    }

    private SeekBar addSlider(LinearLayout root, String label, int min, int max) {
        TextView t = text(label, 14, false);
        t.setTextColor(Color.LTGRAY);
        root.addView(t, matchWrap());
        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        root.addView(bar, matchWrap());
        return bar;
    }

    private TextView section(String value) {
        TextView t = text(value, 16, true);
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(0, dp(12), 0, dp(4));
        t.setLayoutParams(p);
        return t;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }

    private CheckBox check(String value) {
        CheckBox c = new CheckBox(this);
        c.setText(value);
        c.setTextColor(Color.WHITE);
        return c;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER);
        return l;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static class WatchPreview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap bitmap;
        boolean digital = false;
        boolean standby = false;
        boolean showSeconds = true;
        boolean showDate = true;
        boolean showBattery = true;
        int bgPercent = 80;
        int handPercent = 100;
        int infoPercent = 100;
        int bgColor = Color.BLACK;
        int handColor = Color.WHITE;
        int textColor = Color.WHITE;

        public WatchPreview(MainActivity context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void loadBitmap(Uri uri) {
            try (InputStream in = getContext().getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in);
                invalidate();
            } catch (Exception ignored) { }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) / 2f - 10f;
            Path clip = new Path();
            clip.addCircle(cx, cy, r, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setAlpha(255);
            canvas.drawCircle(cx, cy, r, paint);

            if (bitmap != null && bgPercent > 0) {
                drawCenterCrop(canvas, bitmap, cx - r, cy - r, cx + r, cy + r, bgPercent);
            } else {
                int rr = Color.red(bgColor) * bgPercent / 100;
                int gg = Color.green(bgColor) * bgPercent / 100;
                int bb = Color.blue(bgColor) * bgPercent / 100;
                paint.setColor(Color.rgb(rr, gg, bb));
                paint.setAlpha(255);
                canvas.drawCircle(cx, cy, r, paint);
            }

            Date now = new Date();
            if (digital) drawDigital(canvas, cx, cy, r, now);
            else drawAnalog(canvas, cx, cy, r, now);
            drawInfo(canvas, cx, cy, r, now);
            canvas.restore();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.DKGRAY);
            paint.setAlpha(255);
            canvas.drawCircle(cx, cy, r, paint);
        }

        private void drawAnalog(Canvas canvas, float cx, float cy, float r, Date now) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR);
            int minute = cal.get(java.util.Calendar.MINUTE);
            int second = cal.get(java.util.Calendar.SECOND);

            paint.setColor(handColor);
            paint.setAlpha(alpha(handPercent));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30 - 90);
                float x1 = cx + (float)Math.cos(a) * r * 0.78f;
                float y1 = cy + (float)Math.sin(a) * r * 0.78f;
                float x2 = cx + (float)Math.cos(a) * r * 0.87f;
                float y2 = cy + (float)Math.sin(a) * r * 0.87f;
                paint.setStrokeWidth(i % 3 == 0 ? 5f : 3f);
                canvas.drawLine(x1, y1, x2, y2, paint);
            }

            drawHand(canvas, cx, cy, r * 0.48f, (hour + minute / 60f) * 30f - 90f, 9f, handColor, handPercent);
            drawHand(canvas, cx, cy, r * 0.68f, minute * 6f - 90f, 6f, handColor, handPercent);
            if (showSeconds) {
                drawHand(canvas, cx, cy, r * 0.74f, second * 6f - 90f, 2.5f, Color.RED, handPercent);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(handColor);
            paint.setAlpha(alpha(handPercent));
            canvas.drawCircle(cx, cy, 7f, paint);
        }

        private void drawDigital(Canvas canvas, float cx, float cy, float r, Date now) {
            String pattern = showSeconds ? "HH:mm:ss" : "HH:mm";
            String value = new SimpleDateFormat(pattern, Locale.getDefault()).format(now);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(handColor);
            paint.setAlpha(alpha(handPercent));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(r * (showSeconds ? 0.28f : 0.34f));
            canvas.drawText(value, cx, cy + r * 0.08f, paint);
        }

        private void drawInfo(Canvas canvas, float cx, float cy, float r, Date now) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setAlpha(alpha(infoPercent));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(r * 0.10f);
            canvas.drawText(standby ? "VEILLE" : "ACTIF", cx, cy - r * 0.58f, paint);

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(r * 0.105f);
            if (showDate) {
                String date = new SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(now);
                canvas.drawText(date, cx, cy + r * 0.54f, paint);
            }
            if (showBattery) {
                BatteryManager bm = (BatteryManager)getContext().getSystemService(BATTERY_SERVICE);
                int level = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
                String value = level >= 0 ? "Batterie " + level + "%" : "Batterie --%";
                canvas.drawText(value, cx, cy + r * 0.70f, paint);
            }
        }

        private void drawHand(Canvas canvas, float cx, float cy, float length, float angleDeg,
                              float width, int color, int percent) {
            double a = Math.toRadians(angleDeg);
            float x = cx + (float)Math.cos(a) * length;
            float y = cy + (float)Math.sin(a) * length;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            paint.setAlpha(alpha(percent));
            canvas.drawLine(cx, cy, x, y, paint);
        }

        private void drawCenterCrop(Canvas canvas, Bitmap bm, float left, float top, float right, float bottom, int percent) {
            float dw = right - left;
            float dh = bottom - top;
            float scale = Math.max(dw / bm.getWidth(), dh / bm.getHeight());
            int sw = Math.max(1, Math.round(dw / scale));
            int sh = Math.max(1, Math.round(dh / scale));
            int sx = Math.max(0, (bm.getWidth() - sw) / 2);
            int sy = Math.max(0, (bm.getHeight() - sh) / 2);
            Rect src = new Rect(sx, sy, Math.min(bm.getWidth(), sx + sw), Math.min(bm.getHeight(), sy + sh));
            RectF dst = new RectF(left, top, right, bottom);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(alpha(percent));
            canvas.drawBitmap(bm, src, dst, paint);
            paint.setAlpha(255);
        }

        private int alpha(int percent) {
            return Math.max(0, Math.min(255, Math.round(percent * 2.55f)));
        }
    }
}
