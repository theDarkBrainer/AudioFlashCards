package com.thedarkbrainer.audioflashcards;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PARAM_WORDLIST = "WordsList";
    public static final String PARAM_PLAYMODE = "playMode";

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private WordListData mWordListData;
    private int mPlayMode;

    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;
    private BroadcastReceiver mPlayBoxMessageReceiver;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(PlayActivity.this, mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(PlayActivity.this, mMediaControllerCompat);
                MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayActivity.this);

                Bundle dataBundle = new Bundle();
                dataBundle.putSerializable(PlayBackgroundService.PARAM_WORDLIST, mWordListData);
                dataBundle.putInt(PlayBackgroundService.PARAM_PLAYMODE, mPlayMode);

                controller.getTransportControls().playFromMediaId("word", dataBundle);

            } catch( RemoteException e ) {

            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            Log.i("PlayActivity", "mMediaControllerCompatCallback: onPlaybackStateChanged " + state.getState());
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED:
                case PlaybackStateCompat.STATE_STOPPED: {
                    finish();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        mWordListData = (WordListData) intent.getSerializableExtra(PARAM_WORDLIST);
        mPlayMode = intent.getIntExtra(PARAM_PLAYMODE, 0);

        this.findViewById( R.id.btn_replay ).setOnClickListener( this );
        this.findViewById( R.id.btn_next ).setOnClickListener( this );
        this.findViewById( R.id.btn_remove ).setOnClickListener( this );
        this.findViewById( R.id.btn_stop ).setOnClickListener( this );
        this.findViewById( R.id.btn_skip ).setOnClickListener( this );
        this.findViewById( R.id.btn_answer1 ).setOnClickListener( this );
        this.findViewById( R.id.btn_answer2 ).setOnClickListener( this );
        this.findViewById( R.id.btn_answer3 ).setOnClickListener( this );

        findViewById(R.id.text_answer).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_answers).setVisibility(View.INVISIBLE);

        IntentFilter filter = new IntentFilter(PlayBackgroundService.RESPONCE_RECEIVER);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        mPlayBoxMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String mode = intent.getStringExtra(PlayBackgroundService.PARAM_MODE);
                if ( mode != null ) {
                    switch (mode) {
                        case "SetWord":
                            EditText editWord = findViewById(R.id.edit_word);

                            editWord.setText(intent.getStringExtra(PlayBackgroundService.PARAM_WORD));
                            break;

                        case "ClickExam":
                            ArrayList<Button> answerButtons = new ArrayList<>();
                            answerButtons.add((Button) findViewById(R.id.btn_answer1));
                            answerButtons.add((Button) findViewById(R.id.btn_answer2));
                            answerButtons.add((Button) findViewById(R.id.btn_answer3));

                            ArrayList<String> answerTexts = new ArrayList<>();
                            answerTexts.add(intent.getStringExtra(PlayBackgroundService.PARAM_ANSWER));
                            answerTexts.add(intent.getStringExtra(PlayBackgroundService.PARAM_ANSWER1));
                            answerTexts.add(intent.getStringExtra(PlayBackgroundService.PARAM_ANSWER2));

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

                            break;

                        case "Quit":
                            finish();
                            break;
                    }
                }
            }
        };

        registerReceiver(mPlayBoxMessageReceiver, filter);

        mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, PlayBackgroundService.class),
                mMediaBrowserCompatConnectionCallback, getIntent().getExtras());

        mMediaBrowserCompat.connect();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMediaBrowserCompat.disconnect();
        unregisterReceiver(mPlayBoxMessageReceiver);
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
                MediaControllerCompat.getMediaController(PlayActivity.this).sendCommand(PlayBackgroundService.COMMAND_REPLAY, null, null );
            } break;

            case R.id.btn_next: {
                MediaControllerCompat.getMediaController(PlayActivity.this).sendCommand(PlayBackgroundService.COMMAND_NEXT, null, null );
            } break;

            case R.id.btn_skip: {
                MediaControllerCompat.getMediaController(PlayActivity.this).sendCommand(PlayBackgroundService.COMMAND_SKIP, null, null );
            } break;

            case R.id.btn_stop:
                this.finish();
                break;

            case R.id.btn_answer1:
            case R.id.btn_answer2:
            case R.id.btn_answer3: {
                Button answerButton = (Button) v;
                answerButton.getText();

                String command = (Integer) v.getTag() == 1 ? PlayBackgroundService.COMMAND_ANSER_OK : PlayBackgroundService.COMMAND_ANSER_FAIL;
                MediaControllerCompat.getMediaController(PlayActivity.this).sendCommand(command, null, null);

            } break;
        }
    }
}
