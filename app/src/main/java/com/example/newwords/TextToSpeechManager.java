package com.example.newwords;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;

public class TextToSpeechManager implements TextToSpeech.OnInitListener {

    private static volatile TextToSpeechManager instance;
    private TextToSpeech tts;
    private Context context;
    private boolean isInitialized = false;
    private boolean isShuttingDown = false;

    private TextToSpeechManager(Context context) {
        this.context = context.getApplicationContext();
        initTTS();
    }

    public static TextToSpeechManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TextToSpeechManager.class) {
                if (instance == null) {
                    instance = new TextToSpeechManager(context);
                }
            }
        } else {
            // Проверяем, не умер ли TTS движок
            if (instance.tts == null) {
                instance.initTTS();
            }
        }
        return instance;
    }

    private void initTTS() {
        if (isShuttingDown) return;

        // Если TTS уже существует, сначала его остановим
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            tts = null;
        }

        isInitialized = false;
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = false;
            } else {
                // Устанавливаем скорость и высоту речи
                tts.setSpeechRate(0.9f);  // Немного медленнее для лучшего понимания
                tts.setPitch(1.0f);

                // Добавляем слушатель для отслеживания завершения
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            // Озвучивание завершено
                        }

                        @Override
                        public void onError(String utteranceId) {
                            // Ошибка при озвучивании
                        }
                    });
                }

                isInitialized = true;
            }
        } else {
            isInitialized = false;
        }
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;

        // Проверяем, не закрыт ли TTS
        if (tts == null) {
            // Пересоздаем TTS
            initTTS();
            // Ждем инициализации перед озвучиванием
            retrySpeak(text, 0);
            return;
        }

        // Ждем инициализации
        if (!isInitialized) {
            retrySpeak(text, 0);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_" + System.currentTimeMillis());
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_" + System.currentTimeMillis());
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Если ошибка, пробуем пересоздать TTS
            initTTS();
        }
    }

    private void retrySpeak(final String text, final int attempt) {
        if (attempt > 5) return; // Максимум 5 попыток

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isInitialized && tts != null) {
                speak(text);
            } else if (attempt < 5) {
                retrySpeak(text, attempt + 1);
            }
        }, 200); // Ждем 200ms между попытками
    }

    public void shutdown() {
        isShuttingDown = true;
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            tts = null;
        }
        instance = null;
    }

    public boolean isInitialized() {
        return isInitialized && tts != null;
    }

    public void restart() {
        if (tts == null || !isInitialized) {
            initTTS();
        }
    }
}