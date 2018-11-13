package com.thedarkbrainer.audioflashcards.media_player;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TheSpeaker {

    private class MyUtteranceProgressListener extends UtteranceProgressListener
    {
        private OnListener mDoneListener;
        private int mWaitTime;

        public void setDoneListener(OnListener listener)
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
                mDoneListener.OnAction();
        }

        @Override
        public void onError(String utteranceId) {

        }
    }

    private TextToSpeech mSpeaker;
    private MyUtteranceProgressListener mUtteranceProgressListener;

    private boolean mIsGerman;
    private Locale mCurrentLocale;

    interface OnListener
    {
        void OnAction();
    }

    TheSpeaker(Context context, boolean isGerman, int waitTime, final OnListener initListener)
    {
        mIsGerman = isGerman;

        mSpeaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    initListener.OnAction();
                } else {
                    //throw new Exception("TextToSpeak is unsupported");
                }
            }
        });

        mUtteranceProgressListener = new MyUtteranceProgressListener();
        mUtteranceProgressListener.setmWaitTime(waitTime);

        mSpeaker.setSpeechRate(1.0f);
        mSpeaker.setOnUtteranceProgressListener(mUtteranceProgressListener);
    }

    String getWord(String german, String english) {
        return mIsGerman ? german : english;
    }

    void stop()
    {
        mSpeaker.stop();
    }

    static boolean isGerman = true;

    void speak(String german, String english, OnListener doneListener)
    {
        mIsGerman = isGerman;
        isGerman = !isGerman;

        Log.d("TheSpeaker", "speak: mIsGerman="+mIsGerman);
        String text = mIsGerman ? german : english;

        //if ( mCurrentLocale == null ) {
            mCurrentLocale = mIsGerman ? Locale.GERMANY : Locale.US;
            mSpeaker.setLanguage(mCurrentLocale);
        //}

        mUtteranceProgressListener.setDoneListener( doneListener );
        mSpeaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TheSpeaker");
    }
}
