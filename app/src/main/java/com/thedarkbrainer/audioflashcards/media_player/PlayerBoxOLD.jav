package com.thedarkbrainer.audioflashcards.media_player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
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

    private PlayMode mPlayMode;

    private TheSpeaker mSpeakerMain;
    private TheSpeaker mSpeakerOther;
    private TheListener mListenerMain;

    private boolean mIsPlaying = false;

    private String mDataSource;
    private List<String> mArraySpeak = new ArrayList<>();

    private OnCompletionListener mOnCompletionListener;

    private Thread mPlayThread;
    private Runnable mPlayRunnable = new Runnable() {
        public void run() {
            while ( true ) {
                String item = null;
                synchronized (mArraySpeak) {
                    if ( mArraySpeak.size() > 0 ) {
                        item = mArraySpeak.get(0);
                        mArraySpeak.remove(0);
                    }
                }

                if ( item != null ) {
                    Log.d("PlayerBox", "mPlayRunnable: b");
                    synchronized (mArraySpeak) {
                        mSpeakerMain.speak(item, item, new TheSpeaker.OnListener() {
                            @Override
                            public void OnAction() {
                                Log.d("PlayerBox", "mPlayRunnable: done");
                                mOnCompletionListener.onCompleted();
                            }
                        });
                    }
                }
            }
        }
    };

    public PlayerBox(Context context, PlayMode playMode, OnCompletionListener doneListener) {

        mPlayMode = playMode;
        mOnCompletionListener = doneListener;

        Log.d("PlayerBox", "PlayerBox::PlayerBox");
        if (mPlayMode != PlayMode.Quiet) {

            TheSpeaker.OnListener initListener2 = new TheSpeaker.OnListener() {
                private int callCnt = 0;
                @Override
                public void OnAction() {
                    callCnt++;
                    if (callCnt >= 2) {
                        Log.d("PlayerBox", "speakers ready");
                        mPlayThread = new Thread(mPlayRunnable);
                        mPlayThread.start();
                    }
                }
            };

            switch (mPlayMode) {
                case SpeakGerman_SpeakEnglish:
                    mSpeakerMain = new TheSpeaker(context, true, 4000, initListener2);
                    mSpeakerOther = new TheSpeaker(context, false, 2000, initListener2);
                    break;

                case SpeakEnglish_SpeakGerman:
                    mSpeakerMain = new TheSpeaker(context, false, 4000, initListener2);
                    mSpeakerOther = new TheSpeaker(context, true, 2000, initListener2);
                    break;

                case SpeakGerman_ListenEnglish:
                    mSpeakerMain = new TheSpeaker(context, true, 2000, initListener2);
                    mSpeakerOther = new TheSpeaker(context, true, 2000, initListener2);
                    mListenerMain = new TheListener(context, false, 200);
                    break;

                case SpeakEnglish_ListenGerman:
                    mSpeakerMain = new TheSpeaker(context, false, 2000, initListener2);
                    mSpeakerOther = new TheSpeaker(context, false, 2000, initListener2);
                    mListenerMain = new TheListener(context, true, 200);
                    break;

                case SpeakGerman_ClickEnglish:
                    mSpeakerMain = new TheSpeaker(context, true, 200, initListener2);
                    mSpeakerOther = new TheSpeaker(context, false, 1000, initListener2);
                    break;

                case SpeakEnglish_ClickGerman:
                    mSpeakerMain = new TheSpeaker(context, false, 200, initListener2);
                    mSpeakerOther = new TheSpeaker(context, true, 1000, initListener2);
                    break;
            }
        } else {
            Log.d("PlayerBox", "speakers ready now");
            doneListener.onCompleted();
        }
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

    public void setDataSource(String mediaId) {
        Log.d("PlayerBox", "setDataSource: "+mediaId);
        mDataSource = mediaId;
    }

    public void play() {
        Log.d("PlayerBox", "play");
        mIsPlaying = true;
        synchronized (mArraySpeak) {
            mArraySpeak.add(mDataSource);
        }
    }

    public void pause() {
        Log.d("PlayerBox", "pause play");
        mIsPlaying = false;

        if ( mSpeakerMain != null )
            mSpeakerMain.stop();
        if ( mSpeakerOther != null )
            mSpeakerOther.stop();
        if ( mListenerMain != null )
            mListenerMain.stop();
    }

    public void stop() {
        Log.d("PlayerBox", "stop play");
        mIsPlaying = false;

        if ( mSpeakerMain != null )
            mSpeakerMain.stop();
        if ( mSpeakerOther != null )
            mSpeakerOther.stop();
        if ( mListenerMain != null )
            mListenerMain.stop();
    }

    String getCurrentWord(String text_de, String text_en) {
        String result;
        if (mSpeakerMain != null)
            result = mSpeakerMain.getWord(text_de, text_en);
        else
            result = text_de + " - " + text_en;
        return result;
    }

    void speak_main(String text_de, String text_en, final OnCompletionListener listener) throws InterruptedException {
        if ( mIsPlaying ) {
            Log.d("PlayerBox", "speak_main: saying a word:" + text_de + " - " + text_en);

            mSpeakerMain.speak(text_de, text_en, new TheSpeaker.OnListener() {
                @Override
                public void OnAction() {
                    if (listener != null)
                        listener.onCompleted();
                }
            });
        }
    }

    void speak_other(String text_de, String text_en, final OnCompletionListener listener) throws InterruptedException {
        if ( mIsPlaying ) {
            Log.d("PlayerBox", "speak_other: saying a word:" + text_de + " - " + text_en);

            mSpeakerOther.speak(text_de, text_en, new TheSpeaker.OnListener() {
                @Override
                public void OnAction() {
                    if (listener != null)
                        listener.onCompleted();
                }
            });
        }
    }

    void listen(TheListener.OnListener doneListening) {
        mListenerMain.listen( doneListening );
    }
}
