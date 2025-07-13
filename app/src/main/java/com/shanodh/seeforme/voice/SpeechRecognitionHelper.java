package com.shanodh.seeforme.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Helper class for speech recognition
 */
public class SpeechRecognitionHelper {

    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private SpeechRecognitionCallback callback;

    public interface SpeechRecognitionCallback {
        void onResults(ArrayList<String> results);
        void onError(int errorCode);
        void onReadyForSpeech();
        void onEndOfSpeech();
    }

    public SpeechRecognitionHelper(Activity activity, SpeechRecognitionCallback callback) {
        this.activity = activity;
        this.callback = callback;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    if (callback != null) {
                        callback.onReadyForSpeech();
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float v) {
                }

                @Override
                public void onBufferReceived(byte[] bytes) {
                }

                @Override
                public void onEndOfSpeech() {
                    if (callback != null) {
                        callback.onEndOfSpeech();
                    }
                }

                @Override
                public void onError(int i) {
                    if (callback != null) {
                        callback.onError(i);
                    }
                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (callback != null && results != null) {
                        callback.onResults(results);
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                }

                @Override
                public void onEvent(int i, Bundle bundle) {
                }
            });
        } else {
            Toast.makeText(activity, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    public void startListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            speechRecognizer.startListening(intent);
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
} 