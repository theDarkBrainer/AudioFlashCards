package com.thedarkbrainer.audioflashcards.media_player;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.thedarkbrainer.audioflashcards.WordListData;

import java.util.Locale;
import java.util.Vector;

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

    private OnCompletionListener mOnCompletionListener;

    private PlayerRunnable mPlayRunnable = new PlayerRunnable();
    private Thread mPlayThread;

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
        mPlayRunnable.setActive( false );
        mPlayThread.interrupt();
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
        try {
            putWordToSay( data );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Log.d("PlayerBox", "play");
        mIsPlaying = true;
        mPlayRunnable.setActive(true);
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

        mPlayRunnable.setActive(false);

        if ( mSpeaker != null )
            mSpeaker.stop();
        if ( mListenerMain != null )
            mListenerMain.stop();
    }

    void speak(boolean isGerman, String text_de, String text_en, final OnCompletionListener doneListener) throws InterruptedException {
        if ( mIsPlaying ) {
            Log.d("PlayerBox", "speak: saying a word:" + (isGerman ? text_de : text_en));

            mSpeaker.setLanguage(isGerman ? Locale.GERMANY : Locale.US);

            mUtteranceProgressListener.setDoneListener( doneListener );
            mSpeaker.speak(isGerman ? text_de : text_en, TextToSpeech.QUEUE_FLUSH, null, "TheSpeaker");
        }
    }

    void listen(TheListener.OnListener doneListening) {
        mListenerMain.listen( doneListening );
    }


    private WordListData.Data mSayWord = null;

    private synchronized void putWordToSay(WordListData.Data item) throws InterruptedException {
        while (mSayWord != null) {
            Log.d("PlayerBox", "putWordToSay: waiting on word to finish...");
            wait();
        }
        mSayWord = item;
        Log.d("PlayerBox", "putWordToSay");
        notify();
        //Later, when the necessary event happens, the thread that is running it calls notify() from a block synchronized on the same object.
    }

    // Called by Consumer
    public synchronized WordListData.Data getWordToSay() throws InterruptedException {
        notify();
        while (mSayWord == null) {
            Log.d("PlayerBox", "putWordToSay: waiting a word to be put...");
            wait();//By executing wait() from a synchronized block, a thread gives up its hold on the lock and goes to sleep.
        }
        WordListData.Data result = mSayWord;
        mSayWord = null;
        return result;
    }

    private class PlayerRunnable implements Runnable
    {
        private boolean mIsActive = true;

        public synchronized boolean getActive() {
            return mIsActive;
        }

        public synchronized void setActive(boolean active) {
            mIsActive = active;
        }

        public void run() {
            try {
                while (getActive()) {
                    final WordListData.Data message = getWordToSay();
                    Log.d("PlayerBox", "Play Thread: Got a word: " + message.getGerman());

                    speak(true, message.getGerman(), message.getEnglish(), new OnCompletionListener() {
                        @Override
                        public void onCompleted() {
                            Log.d("PlayerBox", "completed word: " + message.getGerman());

                            if ( mIsPlaying ) {
                                try {
                                    speak(false, message.getGerman(), message.getEnglish(), new OnCompletionListener() {
                                        @Override
                                        public void onCompleted() {
                                            Log.d("PlayerBox", "completed word: " + message.getEnglish());

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
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("PlayerBox", "Thread STOPPED!");
        }
    }
}
