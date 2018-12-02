package com.thedarkbrainer.audioflashcards;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PARAM_DO_GERMAN = "DoGerman";
    public static final String PARAM_DO_AUDIO = "DoAudio";
    public static final String PARAM_WORD_LIST = "WordList";

    private boolean mDoGerman = true;
    private boolean mDoAudio = true;
    private WordListData mWordListData;
    private WordListData.ComplexIterator mWordsIterator;

    private TextToSpeech mSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDoGerman = extras.getBoolean(PARAM_DO_GERMAN, true);
            mDoAudio = extras.getBoolean(PARAM_DO_AUDIO, true);
            mWordListData = (WordListData) extras.getSerializable(PARAM_WORD_LIST);
        }
        else {
            mWordListData = new WordListData(this);
        }

        findViewById( R.id.btn_next ).setOnClickListener( this );
        findViewById( R.id.btn_replay ).setOnClickListener( this );
        findViewById( R.id.btn_stop ).setOnClickListener( this );
        findViewById( R.id.btn_answer1 ).setOnClickListener( this );
        findViewById( R.id.btn_answer2 ).setOnClickListener( this );
        findViewById( R.id.btn_answer3 ).setOnClickListener( this );

        findViewById(R.id.text_answer).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_answers).setVisibility(View.INVISIBLE);

        mWordsIterator = mWordListData.iterator_sm2();

        if ( mDoAudio == false ) {
            Log.d("PlayActivity", "no audio");

            findViewById(R.id.btn_replay).setVisibility(mDoAudio ? View.VISIBLE : View.INVISIBLE);
            doWord();
        }
        else {
            findViewById(R.id.btn_replay).setVisibility(View.INVISIBLE);

            Log.d("PlayActivity", "create TextToSpeech");

            mSpeaker = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        Log.d("PlayActivity", "speaker ready");
                        findViewById(R.id.btn_replay).setVisibility(mDoAudio ? View.VISIBLE : View.INVISIBLE);
                        doWord();
                    } else {
                        Log.d("PlayActivity", "speaker init failed");
                    }
                }
            });

            mSpeaker.setSpeechRate(1.0f);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( mSpeaker != null )
            mSpeaker.stop();
    }

    @Override
    public void onDestroy() {
        mWordListData.save(this);

        if ( mSpeaker != null )
            mSpeaker.shutdown();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        findViewById(R.id.text_answer).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_answers).setVisibility(View.INVISIBLE);

        switch (v.getId()) {
            case R.id.btn_replay: {
                doWord();
            } break;

            case R.id.btn_next: {
                mWordsIterator.next();
                doWord();
            } break;

            case R.id.btn_stop:
                this.finish();
                break;

            case R.id.btn_answer1:
            case R.id.btn_answer2:
            case R.id.btn_answer3: {
                Button answerButton = (Button) v;
                answerButton.getText();

                boolean isCorrect = (Integer) v.getTag() == 1;
                reportWord( isCorrect );

                if ( isCorrect ) {
                    mWordsIterator.next();
                    doWord();
                }
                else {
                    Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
                    doWord();
                }
            } break;
        }
    }

    private void doWord() {
        Log.d("PlayActivity", "doWord");

        WordListData.Data word = mWordsIterator.get();

        EditText wordEdit = findViewById(R.id.edit_word);
        wordEdit.setText( mDoGerman ? word.getGerman() : word.getEnglish() );

        if ( mSpeaker != null ) {
            mSpeaker.stop();

            mSpeaker.setLanguage( mDoGerman ? Locale.GERMANY : Locale.US );
            mSpeaker.speak( mDoGerman ? word.getGerman() : word.getEnglish(), TextToSpeech.QUEUE_FLUSH, null, "PlayActivitySpeaker");
        }

        ArrayList<Button> answerButtons = new ArrayList<>();
        answerButtons.add((Button) findViewById(R.id.btn_answer1));
        answerButtons.add((Button) findViewById(R.id.btn_answer2));
        answerButtons.add((Button) findViewById(R.id.btn_answer3));

        ArrayList<String> answerTexts = new ArrayList<>();
        answerTexts.add( mDoGerman ? word.getEnglish() : word.getGerman() );
        answerTexts.add( mDoGerman ? mWordsIterator.getRandomExcudingCurrent().getEnglish() : mWordsIterator.getRandomExcudingCurrent().getGerman() );
        answerTexts.add( mDoGerman ? mWordsIterator.getRandomExcudingCurrent().getEnglish() : mWordsIterator.getRandomExcudingCurrent().getGerman() );
        ArrayList<Integer> answerTags = new ArrayList<>();
        answerTags.add(1);
        answerTags.add(0);
        answerTags.add(0);

        Random randomGenerator = new Random(System.currentTimeMillis());
        for (int loops = 0; loops < 10; loops++) {
            int i = randomGenerator.nextInt(3);
            int j = randomGenerator.nextInt(3);
            Collections.swap(answerTexts, i, j);
            Collections.swap(answerTags, i, j);
        }

        findViewById(R.id.text_answer).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_answers).setVisibility(View.VISIBLE);

        int i = 0;
        for (Button button : answerButtons) {
            button.setText(answerTexts.get(i));
            button.setTag(answerTags.get(i));
            i ++;
        }
    }

    void reportWord(boolean isOk) {
        mWordsIterator.setCurrentAnswer(isOk);
        if( ! isOk )
            mWordsIterator.get().increateErrors();
        mWordsIterator.get().increaseUses();
    }
}
