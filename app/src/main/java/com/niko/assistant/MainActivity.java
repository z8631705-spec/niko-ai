package com.niko.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import java.util.*;

public class MainActivity extends Activity {

    TextView response;
    EditText input;
    TextToSpeech speaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        speaker = new TextToSpeech(this, status -> {
            if(status == TextToSpeech.SUCCESS) {
                speaker.setLanguage(new Locale("fa", "IR"));
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25, 35, 25, 25);
        layout.setBackgroundColor(Color.rgb(10,10,10));

        TextView title = new TextView(this);
        title.setText("N I K O");
        title.setTextColor(Color.RED);
        title.setTextSize(35);
        title.setGravity(Gravity.CENTER);

        response = new TextView(this);
        response.setText("سلام! من نیکو هستم.");
        response.setTextColor(Color.WHITE);
        response.setTextSize(19);
        response.setGravity(Gravity.CENTER);
        response.setPadding(10,40,10,40);

        input = new EditText(this);
        input.setHint("با نیکو صحبت کن...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);

        Button send = new Button(this);
        send.setText("ارسال پیام");
        send.setOnClickListener(v -> reply());

        Button voice = new Button(this);
        voice.setText("🎙 صحبت با نیکو");
        voice.setOnClickListener(v -> listen());

        layout.addView(title);
        layout.addView(response);
        layout.addView(input);
        layout.addView(send);
        layout.addView(voice);

        setContentView(layout);
    }

    void reply() {
        String text = input.getText().toString().trim();

        if(text.isEmpty()) return;

        String answer = "پیامت رو شنیدم. هنوز موتور هوش مصنوعی من متصل نشده.";

        response.setText(answer);
        speaker.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "niko");
        input.setText("");
    }

    void listen() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        try {
            startActivityForResult(intent, 100);
        } catch(Exception e) {
            response.setText("تشخیص گفتار در دسترس نیست.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if(results != null && !results.isEmpty()) {
                input.setText(results.get(0));
                reply();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(speaker != null) speaker.shutdown();
        super.onDestroy();
    }
}
