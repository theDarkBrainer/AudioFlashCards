package com.thedarkbrainer.audioflashcards.media_player;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

public class TheListener implements RecognitionListener {



    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;

    private boolean mIsGerman;

    private OnListener mResultListener;

    interface OnListener
    {
        void OnResult(ArrayList<String> matches);
    }

    TheListener(Context context, boolean isGerman, int waitTime) {
        mIsGerman = isGerman;

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizer.setRecognitionListener(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, mIsGerman ? "de" : "en");
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    void stop()
    {
        mSpeechRecognizer.stopListening();
    }

    void listen(OnListener resultListener)
    {
        Log.i("Listener", "start listening");
        mResultListener = resultListener;
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) { }

    @Override
    public void onBeginningOfSpeech() { }

    @Override
    public void onRmsChanged(float rmsdB) { }

    @Override
    public void onBufferReceived(byte[] buffer) { }

    @Override
    public void onEndOfSpeech(){ }

    @Override
    public void onError(int error) {
        Log.i("Listener", "onError: "+error);

        mResultListener.OnResult( null );
        mResultListener = null;
    }

    @Override
    public void onResults(Bundle results) {
        Log.i("Listener", "onResults");

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        mResultListener.OnResult( matches );
        mResultListener = null;
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.i("Listener", "onPartialResults");

        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        mResultListener.OnResult( matches );
        mResultListener = null;
   }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i("Listener", "onEvent type: " + eventType);
    }
}
