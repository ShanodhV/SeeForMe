package com.shanodh.seeforme.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/**
 * Helper class for text-to-speech
 */
public class TextToSpeechHelper {
    private static final String TAG = "TextToSpeechHelper";
    private TextToSpeech textToSpeech;
    private final TtsCallback callback;

    public interface TtsCallback {
        void onTtsInitialized(boolean success);
        void onTtsCompleted();
    }

    public TextToSpeechHelper(Context context, TtsCallback callback) {
        this.callback = callback;
        
        this.textToSpeech = new TextToSpeech(context, status -> {
            boolean success = status == TextToSpeech.SUCCESS;
            if (success && textToSpeech != null) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                success = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                textToSpeech.setSpeechRate(0.9f);  // Slightly slower than normal for better understanding
                textToSpeech.setPitch(1.0f);       // Normal pitch
                
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "TTS Started: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (callback != null) {
                            callback.onTtsCompleted();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "TTS Error: " + utteranceId);
                    }
                });
            }
            
            if (callback != null) {
                callback.onTtsInitialized(success);
            }
        });
    }

    public void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id");
        }
    }

    public void speakWithQueue(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance_id");
        }
    }

    public void stop() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }
} 