
package com.niko.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
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
import java.util.Locale;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private final int RED = Color.rgb(255, 35, 65);
    private final int BG = Color.rgb(7, 7, 12);

    private TextView status, result, orbText;
    private View orb, ring;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean listening = false;

    private GradientDrawable shape(int color, int radius,
                                   int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke > 0) d.setStroke(3, RED);
        return d;
    }

    private TextView makeText(String value, int size,
                              int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();

        tts = new TextToSpeech(this, code -> {
            if (code == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(
                    new Locale("fa", "IR")
                );
                ready = language != TextToSpeech.LANG_MISSING_DATA
                     && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                10
            );
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        status.setText("● دارم گوش می‌دم");
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
                        status.setText("● آماده‌ام");
                        result.setText("دوباره روی دایره بزن.");
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
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle data) {}

                    @Override
                    public void onEvent(int type, Bundle params) {}
                }
            );
        }

        status.setText("● NIKO آماده است");
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24, 35, 24, 25);
        root.setBackgroundColor(BG);

        TextView title = makeText("N I K O", 34, RED);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = makeText(
            "PERSONAL AI ASSISTANT", 10, Color.GRAY
        );
        subtitle.setPadding(0, 8, 0, 25);
        root.addView(subtitle);

        status = makeText("● INITIALIZING", 13, RED);
        root.addView(status);

        FrameLayout area = new FrameLayout(this);
        LinearLayout.LayoutParams areaParams =
            new LinearLayout.LayoutParams(300, 300);
        areaParams.setMargins(0, 30, 0, 25);
        areaParams.gravity = Gravity.CENTER;
        root.addView(area, areaParams);

        ring = new View(this);
        ring.setBackground(shape(Color.TRANSPARENT, 200, 1));

        FrameLayout.LayoutParams ringParams =
            new FrameLayout.LayoutParams(270, 270,
                Gravity.CENTER);
        area.addView(ring, ringParams);

        orb = new View(this);
        orb.setBackground(shape(
            Color.rgb(45, 5, 18), 200, 1
        ));

        FrameLayout.LayoutParams orbParams =
            new FrameLayout.LayoutParams(195, 195,
                Gravity.CENTER);
        area.addView(orb, orbParams);

        orbText = makeText("N", 68, Color.WHITE);
        orbText.setTypeface(null, Typeface.BOLD);
        orbText.setBackground(shape(
            Color.rgb(145, 8, 35), 200, 1
        ));

        FrameLayout.LayoutParams textParams =
            new FrameLayout.LayoutParams(130, 130,
                Gravity.CENTER);
        area.addView(orbText, textParams);

        area.setOnClickListener(v -> startListening());

        ScaleAnimation pulse = new ScaleAnimation(
            0.94f, 1.05f, 0.94f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(1600);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        ring.startAnimation(pulse);

        TextView instruction = makeText(
            "برای صحبت، دایره را لمس کن", 16, Color.WHITE
        );
        root.addView(instruction);

        result = makeText(
            "منتظر فرمان صوتی تو هستم", 13, Color.GRAY
        );
        result.setPadding(10, 25, 10, 25);
        root.addView(result);

        TextView footer = makeText(
            "VOICE SYSTEM  •  NIKO", 10, RED
        );
        footer.setPadding(0, 20, 0, 0);
        root.addView(footer);

        setContentView(root);
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                10
            );
            return;
        }

        if (recognizer == null) {
            result.setText("تشخیص گفتار در این دستگاه در دسترس نیست.");
            return;
        }

        if (listening) {
            recognizer.cancel();
            listening = false;
            status.setText("● آماده");
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
            RecognizerIntent.EXTRA_LANGUAGE,
            "fa-IR"
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS, 1
        );

        try {
            listening = true;
            status.setText("● در حال اتصال به میکروفون");
            result.setText("دارم گوش می‌دم...");
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            status.setText("● آماده");
            result.setText("شروع تشخیص صدا ناموفق بود.");
        }
    }

    private void handleVoice(String words) {
    result.setText("تو گفتی: " + words);

    String command = words.toLowerCase();
    String answer;

    if (command.contains("ساعت")) {
        answer = "الان ساعت " +
            new java.text.SimpleDateFormat(
                "HH:mm",
                java.util.Locale.getDefault()
            ).format(new java.util.Date()) + " است.";
    }
    else if (command.contains("تاریخ")) {
        answer = "امروز " +
            java.text.DateFormat.getDateInstance(
                java.text.DateFormat.FULL,
                new java.util.Locale("fa", "IR")
            ).format(new java.util.Date()) + " است.";
    }
    else if (command.contains("باتری")) {
        android.content.Intent battery =
            registerReceiver(
                null,
                new android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED
                )
            );

        if (battery != null) {
            int level = battery.getIntExtra(
                android.os.BatteryManager.EXTRA_LEVEL, -1
            );
            answer = "شارژ باتری " + level + " درصد است.";
        } else {
            answer = "نتونستم وضعیت باتری رو بخونم.";
        }
    }
    else {
        answer = "این فرمان رو هنوز یاد نگرفتم.";
    }

    result.setText(answer);
    speak(answer);
    }
