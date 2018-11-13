package com.thedarkbrainer.audioflashcards.media_player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.thedarkbrainer.audioflashcards.WordListData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PlayerBox {

    public enum PlayMode {
        Quiet,
        SpeakGerman_SpeakEnglish,
        SpeakEnglish_SpeakGerman,
        SpeakGerman_ListenEnglish,
        SpeakEnglish_ListenGerman,
        SpeakGerman_ClickEnglish,
        SpeakEnglish_ClickGerman
    }

    public interface OnCompletionListener {
        void onCompleted();
    }

    private class MyUtteranceProgressListener extends UtteranceProgressListener
    {
        private OnCompletionListener mDoneListener;
        private int mWaitTime;

        public void setDoneListener(OnCompletionListener listener)
        {
            mDoneListener = listener;
        }

        public void setmWaitTime(int time)
        {
            mWaitTime = time;
        }

        @Override
        public void onStart(String utteranceId) {

        }

        @Override
        public void onDone(String utteranceId) {
            if ( mWaitTime > 0 ) {
                try {
                    Thread.sleep(mWaitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if ( mDoneListener != null )
                mDoneListener.onCompleted();
        }

        @Override
        public void onError(String utteranceId) {

        }
    }

    private PlayMode mPlayMode;

    private TextToSpeech mSpeaker;
    private MyUtteranceProgressListener mUtteranceProgressListener;
    private TheListener mListenerMain;

    private boolean mIsPlaying = false;

    private WordListData.Data mDataSource;
    private List<WordListData.Data> mArraySpeak = new ArrayList<>();

    private OnCompletionListener mOnCompletionListener;

    private Thread mPlayThread;
    private Runnable mPlayRunnable = new Runnable() {
        public void run() {
            while ( true ) {
                WordListData.Data item = null;
                synchronized (mArraySpeak) {
                    if ( mArraySpeak.size() > 0 ) {
                        item = mArraySpeak.get(0);
                        mArraySpeak.remove(0);
                    }
                }

                if ( item != null ) {
                    final WordListData.Data sayItem = item;

                    synchronized (mArraySpeak) {
                        try {
                            speak(true, sayItem.getGerman(), sayItem.getEnglish(), new OnCompletionListener() {
                                @Override
                                public void onCompleted() {
                                    if ( mIsPlaying ) {
                                        try {
                                            speak(false, sayItem.getGerman(), sayItem.getEnglish(), new OnCompletionListener() {
                                                @Override
                                                public void onCompleted() {
                                                    mIsPlaying = false;
                                                    mOnCompletionListener.onCompleted();
                                                }
                                            });
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            mOnCompletionListener.onCompleted();
                                        }
                                    }
                                    else
                                    {
                                        mOnCompletionListener.onCompleted();
                                    }
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            mOnCompletionListener.onCompleted();
                        }
                    }
                }
            }
        }
    };


    public PlayerBox(Context context, PlayMode playMode) {

        mPlayMode = playMode;

        Log.d("PlayerBox", "PlayerBox::PlayerBox");
        if (mPlayMode != PlayMode.Quiet) {

            mSpeaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        Log.d("PlayerBox", "speakers ready");
                        mPlayThread = new Thread(mPlayRunnable);
                        mPlayThread.start();
                    } else {
                        //throw new Exception("TextToSpeak is unsupported");
                    }
                }
            });

            mUtteranceProgressListener = new MyUtteranceProgressListener();
            mUtteranceProgressListener.setmWaitTime(2000);

            mSpeaker.setSpeechRate(1.0f);
            mSpeaker.setOnUtteranceProgressListener(mUtteranceProgressListener);
        }
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void release() {
        Log.d("PlayerBox", "release");
    }

    PlayMode getPlayMode() {
        return mPlayMode;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public int getCurrentPosition() {
        return 0;
    }

    public void seekTo(int pos) {}

    public void setVolume(float left, float right) {}

    public void setDataSource(WordListData.Data data) {
        Log.d("PlayerBox", "setDataSource: "+data.getGerman());
        mDataSource = data;
    }

    public void start() {
        Log.d("PlayerBox", "play");
        mIsPlaying = true;
        synchronized (mArraySpeak) {
            mArraySpeak.add(mDataSource);
        }
    }

    public void pause() {
        Log.d("PlayerBox", "pause play");
        mIsPlaying = false;

        if ( mSpeaker != null )
            mSpeaker.stop();
        if ( mListenerMain != null )
            mListenerMain.stop();
    }

    public void stop() {
        Log.d("PlayerBox", "stop play");
        mIsPlaying = false;

        if ( mSpeaker != null )
            mSpeaker.stop();
        if ( mListenerMain != null )
            mListenerMain.stop();
    }

    void speak(boolean isGerman, String text_de, String text_en, final OnCompletionListener doneListener) throws InterruptedException {
        if ( mIsPlaying ) {
            Log.d("PlayerBox", "speak_main: saying a word:" + text_de + " - " + text_en);

            mSpeaker.setLanguage(isGerman ? Locale.GERMANY : Locale.US);

            mUtteranceProgressListener.setDoneListener( doneListener );
            mSpeaker.speak(isGerman ? text_de : text_en, TextToSpeech.QUEUE_FLUSH, null, "TheSpeaker");
        }
    }

    void listen(TheListener.OnListener doneListening) {
        mListenerMain.listen( doneListening );
    }
}
