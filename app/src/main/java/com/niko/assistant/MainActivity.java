
package com.niko.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.BatteryManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.camera2.CameraManager;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private LinearLayout root, messages;
    private TextView status, orbText;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean listening = false;
    private boolean ttsReady = false;
    private boolean flashlightOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    private final int PURPLE = Color.rgb(174, 92, 255);
    private final int BG = Color.rgb(7, 6, 13);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        createInterface();
        initializeSpeech();
        initializeTTS();
        initializeFlashlight();
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke > 0) d.setStroke(2, PURPLE);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void createInterface() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(22, 25, 22, 25);
        root.setBackgroundColor(BG);

        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("N I K O", 30, PURPLE);
        root.addView(title);

        TextView subtitle = text("YOUR PERSONAL AI ASSISTANT", 11, Color.LTGRAY);
        root.addView(subtitle);

        root.addView(space(25));

        LinearLayout orb = new LinearLayout(this);
        orb.setGravity(Gravity.CENTER);
        orb.setBackground(shape(Color.rgb(24, 12, 38), 300, 1));
        orb.setLayoutParams(new LinearLayout.LayoutParams(300, 300));

        orbText = text("N", 100, Color.WHITE);
        orbText.setBackground(shape(Color.rgb(67, 25, 105), 300, 1));
        orbText.setLayoutParams(new LinearLayout.LayoutParams(230, 230));
        orb.addView(orbText);
        root.addView(orb);

        root.addView(space(18));

        status = text("نیکو آماده است 💜", 17, Color.WHITE);
        root.addView(status);

        root.addView(space(20));

        Button listenButton = button("🎙 صحبت با نیکو");
        listenButton.setOnClickListener(v -> startListening());
        root.addView(listenButton);

        Button stopButton = button("⏹ توقف شنیدن");
        stopButton.setOnClickListener(v -> stopListening());
        root.addView(stopButton);

        Button testButton = button("🔊 تست صدای نیکو");
        testButton.setOnClickListener(v -> speak("سلام! من نیکو هستم. آماده‌ام کمکت کنم."));
        root.addView(testButton);

        Button appsButton = button("📱 باز کردن برنامه‌ها");
        appsButton.setOnClickListener(v -> showApps());
        root.addView(appsButton);

        Button flashButton = button("🔦 چراغ‌قوه");
        flashButton.setOnClickListener(v -> toggleFlashlight());
        root.addView(flashButton);

        Button settingsButton = button("⚙ تنظیمات گوشی");
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        root.addView(settingsButton);

        root.addView(space(20));

        TextView historyTitle = text("گفتگو با نیکو", 18, PURPLE);
        root.addView(historyTitle);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        root.addView(messages);
    }

    private View space(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return v;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(shape(Color.rgb(39, 20, 57), 35, 1));
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, 58);
        p.setMargins(0, 7, 0, 7);
        b.setLayoutParams(p);
        return b;
    }

    private void addMessage(String who, String message) {
        TextView t = text(who + ": " + message, 15, Color.WHITE);
        t.setGravity(Gravity.RIGHT);
        t.setPadding(12, 12, 12, 12);
        t.setBackground(shape(Color.rgb(28, 19, 39), 18, 0));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 5, 0, 5);
        messages.addView(t, p);
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(new Locale("fa", "IR"));
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    private void speak(String message) {
        addMessage("نیکو", message);

        if (ttsReady && tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "NIKO_SPEECH");
        } else {
            Toast.makeText(this,
                    "صدای فارسی در تنظیمات گفتار گوشی بررسی شود",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initializeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("سرویس تشخیص گفتار در دسترس نیست");
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                status.setText("دارم گوش می‌دم...");
                orbText.setText("...");
            }

            @Override public void onBeginningOfSpeech() {}

            @Override public void onRmsChanged(float rmsdB) {}

            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                status.setText("در حال پردازش...");
            }

            @Override public void onError(int error) {
                listening = false;
                status.setText("برای صحبت دوباره دکمه میکروفون رو بزن");
                orbText.setText("N");
            }

            @Override public void onResults(Bundle results) {
                listening = false;
                orbText.setText("N");

                ArrayList<String> data =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (data != null && !data.isEmpty()) {
                    String command = data.get(0);
                    addMessage("شما", command);
                    handleCommand(command);
                }

                status.setText("نیکو آماده است 💜");
            }

            @Override public void onPartialResults(Bundle partialResults) {}

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (recognizer == null) {
            speak("سرویس تشخیص گفتار در دسترس نیست.");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 100);
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

            listening = true;
            recognizer.startListening(intent);
        } catch (Exception e) {
            status.setText("خطا در شروع میکروفون");
        }
    }

    private void stopListening() {
        listening = false;
        if (recognizer != null) recognizer.cancel();
        status.setText("شنیدن متوقف شد");
        orbText.setText("N");
    }

    private void handleCommand(String raw) {
        String c = raw.toLowerCase(Locale.ROOT);

        if (c.contains("سلام")) {
            speak("سلام! خوبی؟ من نیکو هستم.");
        } else if (c.contains("اسمت") || c.contains("اسم تو")) {
            speak("اسم من نیکوئه، دستیار شخصی تو!");
        } else if (c.contains("ساعت")) {
            String time = new SimpleDateFormat("HH:mm",
                    Locale.getDefault()).format(new Date());
            speak("الان ساعت " + time است.");
        } else if (c.contains("تاریخ")) {
            String date = new SimpleDateFormat("yyyy/MM/dd",
                    Locale.getDefault()).format(new Date());
            speak("امروز " + date است.");
        } else if (c.contains("باتری")) {
            BatteryManager bm = (BatteryManager)
                    getSystemService(BATTERY_SERVICE);
            int level = bm.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY);
            speak("شارژ باتری " + level + " درصد است.");
        } else if (c.contains("چراغ") &&
                (c.contains("روشن") || c.contains("خاموش"))) {
            toggleFlashlight();
        } else if (c.contains("اینستاگرام")) {
            openApp("com.instagram.android");
        } else if (c.contains("تلگرام")) {
            openApp("org.telegram.messenger");
        } else if (c.contains("واتساپ")) {
            openApp("com.whatsapp");
        } else if (c.contains("دوربین")) {
            openIntent(new Intent("android.media.action.IMAGE_CAPTURE"));
        } else if (c.contains("ماشین حساب") || c.contains("حسابگر")) {
            openIntent(new Intent("android.intent.action.MAIN")
                    .addCategory("android.intent.category.APP_CALCULATOR"));
        } else if (c.contains("تنظیمات")) {
            openIntent(new Intent(android.provider.Settings.ACTION_SETTINGS));
        } else if (c.contains("مرورگر") || c.contains("اینترنت")) {
            openIntent(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com")));
        } else if (c.contains("خداحافظ")) {
            speak("فعلاً! هر وقت خواستی من اینجام.");
        } else {
            speak("این فرمان رو هنوز یاد نگرفتم. یه جور دیگه بگو.");
        }
    }

    private void openIntent(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            speak("باشه، انجام شد.");
        } catch (Exception e) {
            speak("این برنامه در گوشی پیدا نشد.");
        }
    }

    private void openApp(String packageName) {
        Intent launch = getPackageManager()
                .getLaunchIntentForPackage(packageName);

        if (launch != null) {
            startActivity(launch);
            speak("دارم برنامه رو باز می‌کنم.");
        } else {
            speak("این برنامه نصب نیست یا قابل دسترسی نیست.");
        }
    }

    private void showApps() {
        String[] names = {"اینستاگرام", "تلگرام", "واتساپ"};
        String[] packages = {
                "com.instagram.android",
                "org.telegram.messenger",
                "com.whatsapp"
        };

        new AlertDialog.Builder(this)
                .setTitle("انتخاب برنامه")
                .setItems(names, (dialog, which) ->
                        openApp(packages[which]))
                .setNegativeButton("بستن", null)
                .show();
    }

    private void initializeFlashlight() {
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                Boolean flash = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    cameraId = id;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void toggleFlashlight() {
        if (cameraId == null) {
            speak("چراغ‌قوه در دسترس نیست.");
            return;
        }

        try {
            flashlightOn = !flashlightOn;
            cameraManager.setTorchMode(cameraId, flashlightOn);
            speak(flashlightOn ? "چراغ‌قوه روشن شد." : "چراغ‌قوه خاموش شد.");
        } catch (Exception e) {
            flashlightOn = !flashlightOn;
            speak("نتونستم چراغ‌قوه رو کنترل کنم.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                speak("برای صحبت با من باید اجازه میکروفون رو بدی.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
