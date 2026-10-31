package com.niko.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.speech.tts.TextToSpeech;
import android.speech.RecognizerIntent;
import android.content.Intent;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView chat, status;
    EditText input;
    TextToSpeech speaker;
    LinearLayout messages;

    int red = Color.rgb(255, 40, 65);

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    TextView text(String value, int size, int color) {
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

        speaker = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                speaker.setLanguage(new Locale("fa", "IR"));
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 25, 22, 15);
        root.setBackgroundColor(Color.rgb(8, 8, 12));

        TextView title = text("N I K O", 30, red);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        status = text("●  SYSTEM ONLINE", 12,
                Color.rgb(130, 130, 145));
        root.addView(status);

        TextView subtitle = text(
                "YOUR PERSONAL AI ASSISTANT", 10,
                Color.GRAY);
        subtitle.setPadding(0, 5, 0, 25);
        root.addView(subtitle);

        TextView orb = text("N", 55, Color.WHITE);
        orb.setTypeface(null, Typeface.BOLD);
        orb.setBackground(shape(red, 200));

        LinearLayout.LayoutParams orbParams =
                new LinearLayout.LayoutParams(155, 155);
        orbParams.gravity = Gravity.CENTER;
        orbParams.setMargins(0, 15, 0, 10);
        root.addView(orb, orbParams);

        TextView greeting = text(
                "سلام! من نیکو هستم.", 19, Color.WHITE);
        root.addView(greeting);

        TextView description = text(
                "آماده‌ام به حرف‌هات گوش بدم.", 13,
                Color.GRAY);
        root.addView(description);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(messages);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        -1, 0, 1);
        scrollParams.setMargins(0, 20, 0, 15);
        root.addView(scroll, scrollParams);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(8, 7, 8, 7);
        bottom.setBackground(shape(
                Color.rgb(28, 28, 35), 35));

        input = new EditText(this);
        input.setHint("پیامت رو بنویس...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setBackgroundColor(Color.TRANSPARENT);

        bottom.addView(input,
                new LinearLayout.LayoutParams(0, 55, 1));

        TextView send = text("➤", 23, Color.WHITE);
        send.setBackground(shape(red, 50));

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(48, 48);
        bottom.addView(send, sendParams);

        send.setOnClickListener(v -> sendMessage());

        root.addView(bottom);

        TextView mic = text("🎙  صحبت با نیکو", 16,
                Color.WHITE);
        mic.setBackground(shape(
                Color.rgb(150, 20, 38), 35));

        LinearLayout.LayoutParams micParams =
                new LinearLayout.LayoutParams(-1, 55);
        micParams.setMargins(0, 12, 0, 0);
        root.addView(mic, micParams);

        mic.setOnClickListener(v -> voiceInput());

        setContentView(root);
    }

    void sendMessage() {
        String message = input.getText().toString().trim();

        if (message.isEmpty()) return;

        addMessage("تو: " + message, true);
        input.setText("");

        String answer =
                "پیامت رو دریافت کردم. موتور هوش مصنوعی هنوز متصل نشده.";

        addMessage("NIKO: " + answer, false);

        if (speaker != null) {
            speaker.speak(answer,
                    TextToSpeech.QUEUE_FLUSH, null, "niko");
        }
    }

    void addMessage(String value, boolean user) {
        TextView bubble = text(value, 14, Color.WHITE);
        bubble.setGravity(Gravity.RIGHT);
        bubble.setPadding(16, 13, 16, 13);
        bubble.setBackground(shape(
                user ? Color.rgb(100, 20, 35)
                     : Color.rgb(35, 35, 45), 20));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 5, 0, 5);
        messages.addView(bubble, params);
    }

    void voiceInput() {
        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");

        try {
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            addMessage(
                    "تشخیص گفتار روی این دستگاه در دسترس نیست.",
                    false);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 &&
                resultCode == RESULT_OK && data != null) {

            java.util.ArrayList<String> results =
                    data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                input.setText(results.get(0));
                sendMessage();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
        }
        super.onDestroy();
    }
}
