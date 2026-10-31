
package com.niko.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.provider.Settings;
import java.util.Locale;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private final int PURPLE = Color.rgb(174, 92, 255);
    private final int BG = Color.rgb(7, 6, 13);

    private TextView status, result, orbText, ttsStatus;
    private View ring, orb;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean listening = false;
    private boolean torchOn = false;
    private String cameraId;

    private GradientDrawable shape(int color, int radius,
                                   int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke > 0) d.setStroke(3, PURPLE);
        return d;
    }

    private TextView makeText(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView makeButton(String value) {
        TextView b = makeText(value, 15, Color.WHITE);
        b.setPadding(14, 15, 14, 15);
        b.setBackground(shape(Color.rgb(30, 18, 45), 28, 1));
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();

        tts = new TextToSpeech(this, code -> {
            if (code == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(new Locale("fa", "IR"));
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;

                runOnUiThread(() -> {
                    if (ttsReady) {
                        ttsStatus.setText("صدای فارسی آماده است");
                    } else {
                        ttsStatus.setText(
                            "زبان فارسی موتور گفتار نصب نیست"
                        );
                    }
                });
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, 10
            );
        }

        setupRecognizer();
        setupFlashlight();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(22, 35, 22, 30);
        scroll.addView(root);

        TextView title = makeText("N I K O", 34, PURPLE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = makeText(
            "PERSONAL AI ASSISTANT", 11, Color.LTGRAY
        );
        subtitle.setPadding(0, 7, 0, 22);
        root.addView(subtitle);

        status = makeText("● آماده به کار", 14, PURPLE);
        root.addView(status);

        FrameLayout area = new FrameLayout(this);
        LinearLayout.LayoutParams areaParams =
            new LinearLayout.LayoutParams(330, 330);
        areaParams.setMargins(0, 24, 0, 18);
        areaParams.gravity = Gravity.CENTER;
        root.addView(area, areaParams);

        ring = new View(this);
        ring.setBackground(shape(Color.TRANSPARENT, 300, 1));
        area.addView(ring, new FrameLayout.LayoutParams(
            310, 310, Gravity.CENTER
        ));

        orb = new View(this);
        GradientDrawable orbBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{
                Color.rgb(95, 30, 160),
                Color.rgb(30, 8, 55),
                Color.rgb(120, 35, 190)
            }
        );
        orbBg.setShape(GradientDrawable.OVAL);
        orbBg.setStroke(4, PURPLE);
        orb.setBackground(orbBg);

        area.addView(orb, new FrameLayout.LayoutParams(
            255, 255, Gravity.CENTER
        ));

        orbText = makeText("N", 90, Color.WHITE);
        orbText.setTypeface(null, Typeface.BOLD);
        orbText.setBackground(shape(
            Color.rgb(70, 22, 115), 200, 1
        ));
        area.addView(orbText, new FrameLayout.LayoutParams(
            175, 175, Gravity.CENTER
        ));

        area.setOnClickListener(v -> startListening());

        ScaleAnimation pulse = new ScaleAnimation(
            0.94f, 1.06f, 0.94f, 1.06f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(1400);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        ring.startAnimation(pulse);

        TextView instruction = makeText(
            "برای صحبت با نیکو، دایره را لمس کن",
            15, Color.WHITE
        );
        root.addView(instruction);

        result = makeText(
            "منتظر فرمان تو هستم", 14, Color.LTGRAY
        );
        result.setPadding(10, 22, 10, 18);
        root.addView(result);

        ttsStatus = makeText(
            "در حال آماده‌سازی صدا...", 12, PURPLE
        );
        root.addView(ttsStatus);

        TextView testVoice = makeButton("🔊 تست صدای نیکو");
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(
                -1, -2
            );
        p.setMargins(0, 22, 0, 10);
        root.addView(testVoice, p);
        testVoice.setOnClickListener(v ->
            speak("سلام، من نیکو هستم. آماده‌ام کمکت کنم.")
        );

        TextView flashlight = makeButton("🔦 روشن / خاموش کردن چراغ‌قوه");
        root.addView(flashlight, new LinearLayout.LayoutParams(-1, -2));
        flashlight.setOnClickListener(v -> toggleFlashlight());

        TextView apps = makeButton("📱 باز کردن برنامه‌ها");
        LinearLayout.LayoutParams p2 =
            new LinearLayout.LayoutParams(-1, -2);
        p2.setMargins(0, 10, 0, 0);
        root.addView(apps, p2);
        apps.setOnClickListener(v -> showApps());

        TextView settings = makeButton("⚙ تنظیمات گوشی");
        LinearLayout.LayoutParams p3 =
            new LinearLayout.LayoutParams(-1, -2);
        p3.setMargins(0, 10, 0, 0);
        root.addView(settings, p3);
        settings.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_SETTINGS))
        );

        TextView footer = makeText(
            "NIKO  •  PURPLE EDITION", 10, PURPLE
        );
        footer.setPadding(0, 25, 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }

    private void setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("تشخیص گفتار در این گوشی در دسترس نیست");
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("● گوش می‌دم...");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("● صدات رو دریافت کردم");
            }

            @Override
            public void onRmsChanged(float value) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                listening = false;
                status.setText("● در حال پردازش");
            }

            @Override
            public void onError(int error) {
                listening = false;
                status.setText("● آماده به کار");
                result.setText("صدا دریافت نشد؛ دوباره امتحان کن.");
            }

            @Override
            public void onResults(Bundle data) {
                listening = false;
                ArrayList<String> matches =
                    data.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (matches != null && !matches.isEmpty()) {
                    handleVoice(matches.get(0));
                } else {
                    result.setText("متوجه نشدم؛ دوباره بگو.");
                }
            }

            @Override
            public void onPartialResults(Bundle data) {}

            @Override
            public void onEvent(int type, Bundle params) {}
        });
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, 10
            );
            return;
        }

        if (recognizer == null) {
            result.setText("تشخیص گفتار در دسترس نیست.");
            return;
        }

        if (listening) {
            recognizer.cancel();
            listening = false;
            status.setText("● آماده به کار");
            return;
        }

        Intent intent = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE, "fa-IR"
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS, 1
        );

        try {
            listening = true;
            result.setText("دارم گوش می‌دم...");
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            result.setText("شروع میکروفون ناموفق بود.");
        }
    }

    private void handleVoice(String words) {
        result.setText("تو گفتی: " + words);
        String c = words.toLowerCase().trim();
        String answer = "";

        if (c.contains("ساعت")) {
            answer = "الان ساعت " +
                new SimpleDateFormat(
                    "HH:mm", Locale.getDefault()
                ).format(new Date()) + " است.";
        }
        else if (c.contains("تاریخ")) {
            answer = "امروز " +
                java.text.DateFormat.getDateInstance(
                    java.text.DateFormat.FULL,
                    new Locale("fa", "IR")
                ).format(new Date()) + " است.";
        }
        else if (c.contains("باتری") || c.contains("شارژ")) {
            Intent battery = registerReceiver(
                null,
                new android.content.IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            );
            int level = battery == null ? -1 :
                battery.getIntExtra(
                    BatteryManager.EXTRA_LEVEL, -1
                );
            answer = level >= 0
                ? "شارژ باتری " + level + " درصد است."
                : "وضعیت باتری در دسترس نیست.";
        }
        else if (c.contains("چراغ") &&
                 (c.contains("روشن") || c.contains("خاموش"))) {
            boolean turnOn = c.contains("روشن");
            setFlashlight(turnOn);
            answer = turnOn
                ? "چراغ‌قوه روشن شد."
                : "چراغ‌قوه خاموش شد.";
        }
        else if (c.contains("اینستاگرام")) {
            answer = launchApp("com.instagram.android", "اینستاگرام");
        }
        else if (c.contains("تلگرام")) {
            answer = launchApp("org.telegram.messenger", "تلگرام");
        }
        else if (c.contains("واتساپ")) {
            answer = launchApp("com.whatsapp", "واتساپ");
        }
        else if (c.contains("دوربین")) {
            startActivity(new Intent("android.media.action.IMAGE_CAPTURE"));
            answer = "دوربین رو باز کردم.";
        }
        else if (c.contains("ماشین حساب") ||
                 c.contains("حسابگر")) {
            answer = launchApp("com.google.android.calculator",
                               "ماشین حساب");
        }
        else if (c.contains("تنظیمات")) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            answer = "تنظیمات گوشی رو باز کردم.";
        }
        else if (c.contains("وای فای") || c.contains("وای‌فای")) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            answer = "تنظیمات وای‌فای رو باز کردم.";
        }
        else if (c.contains("بلوتوث")) {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            answer = "تنظیمات بلوتوث رو باز کردم.";
        }
        else if (c.contains("مرورگر") || c.contains("اینترنت")) {
            Intent web = new Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.google.com")
            );
            startActivity(web);
            answer = "مرورگر رو باز کردم.";
        }
        else if (c.contains("سلام")) {
            answer = "سلام! من نیکو هستم. چه کاری برات انجام بدم؟";
        }
        else if (c.contains("اسمت")) {
            answer = "اسم من نیکوئه، دستیار شخصی تو.";
        }
        else {
            answer = "این فرمان هنوز به من اضافه نشده.";
        }

        result.setText(answer);
        speak(answer);
    }

    private String launchApp(String packageName, String name) {
        Intent launch = getPackageManager()
            .getLaunchIntentForPackage(packageName);

        if (launch != null) {
            startActivity(launch);
            return "دارم " + name + " رو باز می‌کنم.";
        }
        return name + " روی گوشی پیدا نشد.";
    }

    private void showApps() {
        String[] names = {
            "اینستاگرام", "تلگرام", "واتساپ",
            "دوربین", "تنظیمات"
        };
        String[] packages = {
            "com.instagram.android",
            "org.telegram.messenger",
            "com.whatsapp",
            "android.media.action.IMAGE_CAPTURE",
            "settings"
        };

        new AlertDialog.Builder(this)
            .setTitle("برنامه موردنظر رو انتخاب کن")
            .setItems(names, (dialog, which) -> {
                if (which == 3) {
                    startActivity(new Intent(
                        "android.media.action.IMAGE_CAPTURE"
                    ));
                } else if (which == 4) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } else {
                    String msg = launchApp(
                        packages[which], names[which]
                    );
                    result.setText(msg);
                }
            }).show();
    }

    private void setupFlashlight() {
        try {
            CameraManager manager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            for (String id : manager.getCameraIdList()) {
                Boolean available = manager.getCameraCharacteristics(id)
                    .get(
                        android.hardware.camera2.CameraCharacteristics
                            .FLASH_INFO_AVAILABLE
                    );
                if (available != null && available) {
                    cameraId = id;
                    break;
                }
            }
        } catch (Exception e) {
            cameraId = null;
        }
    }

    private void setFlashlight(boolean enabled) {
        try {
            if (cameraId == null) setupFlashlight();
            if (cameraId == null) {
                result.setText("چراغ‌قوه در دسترس نیست.");
                return;
            }

            CameraManager manager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            manager.setTorchMode(cameraId, enabled);
            torchOn = enabled;
        } catch (Exception e) {
            result.setText("تغییر وضعیت چراغ‌قوه انجام نشد.");
        }
    }

    private void toggleFlashlight() {
        setFlashlight(!torchOn);
        String msg = torchOn
            ? "چراغ‌قوه روشن شد."
            : "چراغ‌قوه خاموش شد.";
        result.setText(msg);
        speak(msg);
    }

    private void speak(String words) {
        if (tts != null && ttsReady) {
            status.setText("● نیکو در حال صحبت");
            tts.speak(
                words,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "NIKO_SPEECH"
            );
        } else {
            ttsStatus.setText(
                "صدای فارسی آماده نیست؛ موتور گفتار گوشی را بررسی کن."
            );
            status.setText("● پاسخ متنی آماده است");
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );
        if (requestCode == 10 && grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status.setText("● میکروفون فعال شد");
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
