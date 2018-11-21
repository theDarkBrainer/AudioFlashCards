package com.thedarkbrainer.audioflashcards;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class WordListAudioRenderer extends AsyncTask<Void, Void, Boolean> {
    private WordListData mWordListData;

    private CountDownLatch mSynthesizeFinishSignal;
    private ProgressDialog mRenderProgressDlg;
    private TextToSpeech mSpeaker;

    WordListAudioRenderer(WordListData wordListData) {
        mWordListData = wordListData;
    }

    void process(Context context)
    {
        mRenderProgressDlg = new ProgressDialog( context );

        mSpeaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    Log.d("WordListAudioRenderer", "speaker ready");
                    execute();
                } else {
                    Log.d("WordListAudioRenderer", "speaker init failed");
                    onPostExecute( false );
                }
            }
        });

        mSpeaker.setSpeechRate(1.0f);
        mSpeaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                Log.d("WordListAudioRenderer", "UtteranceProgressListener: onDone");
                if ( mSynthesizeFinishSignal != null )
                    mSynthesizeFinishSignal.countDown();
            }


            @Override
            public void onError(String utteranceId) {
                Log.d("WordListAudioRenderer", "UtteranceProgressListener: onError");
                if ( mSynthesizeFinishSignal != null )
                    mSynthesizeFinishSignal.countDown();
            }
        });
    }

    static void clearAudioFolder() {
        File parentFolder = getAudioFolder();
        if ( parentFolder.exists() ) {
            for (String s : parentFolder.list()) {
                File currentFile = new File(parentFolder.getPath(), s);
                currentFile.delete();
            }

            parentFolder.delete();
        }
    }

    static File getAudioFolder() {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File parentFolder = new File(downloadFolder, "AudioFlashCards");
        return parentFolder;
    }

    static File getAudioFile(int wordIndex) {
        File audioFolder = getAudioFolder();
        String fileName = "/Item" + wordIndex + ".wav";
        return new File(audioFolder, fileName);
    }

    @Override
    protected void onPreExecute() {
        mRenderProgressDlg.setMessage("Rendering audio, please wait...");
        mRenderProgressDlg.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
        mRenderProgressDlg.setProgress( 0 );
        mRenderProgressDlg.setMax( mWordListData.getCount() );
        mRenderProgressDlg.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File parentFolder = new File(downloadFolder, "AudioFlashCards");
        if ( ! parentFolder.exists() ) {
            parentFolder.mkdirs();
        }

        mSpeaker.setLanguage( Locale.GERMANY );

        String utteranceID = "wordListAudioRenderer";

        Log.d("WordListAudioRenderer", "doInBackground: cnt=" + mWordListData.getCount());
        for(int i=0; i<mWordListData.getCount(); i++) {
            WordListData.Data word = mWordListData.getItem( i );

            if ( mRenderProgressDlg != null && mRenderProgressDlg.isShowing() )
                mRenderProgressDlg.setProgress( i );

            File file = getAudioFile( i );
            if ( ! file.exists() ) {
                mSynthesizeFinishSignal = new CountDownLatch(1);
                mSpeaker.synthesizeToFile(word.getGerman(), null, file, utteranceID);
                try {
                    mSynthesizeFinishSignal.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d("WordListAudioRenderer", "onPostExecute");

        if ( mRenderProgressDlg != null && mRenderProgressDlg.isShowing() )
            mRenderProgressDlg.dismiss();

        if ( mSpeaker != null ) {
            mSpeaker.stop();
            mSpeaker.shutdown();
        }
    }
}