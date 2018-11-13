package com.thedarkbrainer.audioflashcards;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
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
import java.util.List;
import java.util.Random;

import com.thedarkbrainer.audioflashcards.media_client.MediaBrowserHelper;
import com.thedarkbrainer.audioflashcards.media_service.MusicService;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PARAM_WORDLIST = "WordsList";
    public static final String PARAM_PLAYMODE = "playMode";

    private MediaBrowserHelper mMediaBrowserHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        WordListData wordListData = (WordListData) intent.getSerializableExtra(PARAM_WORDLIST);
        int playMode = intent.getIntExtra(PARAM_PLAYMODE, 0);

        findViewById( R.id.btn_replay ).setOnClickListener( this );
        findViewById( R.id.btn_next ).setOnClickListener( this );
        findViewById( R.id.btn_remove ).setOnClickListener( this );
        findViewById( R.id.btn_stop ).setOnClickListener( this );
        findViewById( R.id.btn_skip ).setOnClickListener( this );
        findViewById( R.id.btn_answer1 ).setOnClickListener( this );
        findViewById( R.id.btn_answer2 ).setOnClickListener( this );
        findViewById( R.id.btn_answer3 ).setOnClickListener( this );

        findViewById(R.id.text_answer).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_answers).setVisibility(View.INVISIBLE);
        findViewById(R.id.btn_skip).setVisibility(View.INVISIBLE);

        Bundle dataBundle = new Bundle();
        dataBundle.putSerializable(MusicService.PARAM_WORDLIST, wordListData);
        dataBundle.putInt(MusicService.PARAM_PLAYMODE, playMode);

        mMediaBrowserHelper = new MediaBrowserConnection(this, dataBundle);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowserHelper.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserHelper.onStop();
        finish();
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
        findViewById(R.id.btn_skip).setVisibility(View.INVISIBLE);

        switch (v.getId()) {
            case R.id.btn_replay: {
                mMediaBrowserHelper.getTransportControls().rewind();
            } break;

            case R.id.btn_next: {
                mMediaBrowserHelper.getTransportControls().skipToNext();
            } break;

            case R.id.btn_skip: {
                mMediaBrowserHelper.getTransportControls().skipToNext();
            } break;

            case R.id.btn_stop:
                mMediaBrowserHelper.getTransportControls().stop();
                this.finish();
                break;

            case R.id.btn_answer1:
            case R.id.btn_answer2:
            case R.id.btn_answer3: {
                Button answerButton = (Button) v;
                answerButton.getText();

                //String command = (Integer) v.getTag() == 1 ? PlayBackgroundService.COMMAND_ANSER_OK : PlayBackgroundService.COMMAND_ANSER_FAIL;
                //MediaControllerCompat.getMediaController(PlayActivity.this).sendCommand(command, null, null);

            } break;
        }
    }


    /**
     * Customize the connection to our {@link android.support.v4.media.MediaBrowserServiceCompat}
     * and implement our app specific desires.
     */
    private class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context, Bundle data) {
            super(context, MusicService.class, data);
        }

        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
        }

        @Override
        protected void onChildrenLoaded(@NonNull String parentId,
                                        @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            final MediaControllerCompat mediaController = getMediaController();

            // Queue up all media items for this simple sample.
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                mediaController.addQueueItem(mediaItem.getDescription());
            }

            // Start play immediately
            mediaController.getTransportControls().play();
        }
    }

    /**
     * Implementation of the {@link MediaControllerCompat.Callback} methods we're interested in.
     * <p>
     * Here would also be where one could override
     * {@code onQueueChanged(List<MediaSessionCompat.QueueItem> queue)} to get informed when items
     * are added or removed from the queue. We don't do this here in order to keep the UI
     * simple.
     */
    private class MediaBrowserListener extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            Log.d("PlayActivity", "MediaBrowserListener:onPlaybackStateChanged state="+(playbackState == null ? "null" : playbackState.getState()));
            //if ( playbackState != null && playbackState.getState() != PlaybackStateCompat.STATE_PLAYING )
            //    finish();
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }
            EditText wordEdit = findViewById(R.id.edit_word);
            wordEdit.setText( mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) );
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
        }
    }
}
