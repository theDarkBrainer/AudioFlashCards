package com.thedarkbrainer.audioflashcards;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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

import java.util.List;

import com.thedarkbrainer.audioflashcards.media_service.MediaBrowserHelper;
import com.thedarkbrainer.audioflashcards.media_service.MusicService;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PARAM_WORDLIST = "WordsList";
    public static final String PARAM_PLAYMODE = "playMode";

    private static final int REQUEST_EXPORT_AUDIO = 100;

    private MediaBrowserHelper mMediaBrowserHelper;
    private WordListData mWordListData;
    private int mPlayMode;

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
        dataBundle.putSerializable(MusicService.PARAM_WORDLIST, mWordListData);
        dataBundle.putInt(MusicService.PARAM_PLAYMODE, mPlayMode);

        mMediaBrowserHelper = new MediaBrowserConnection(this, dataBundle);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());
    }


    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXPORT_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    renderAudio();
                } else {
                    Toast.makeText(getApplicationContext(), "Requesting writing denied!", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_EXPORT_AUDIO);
            } else {
                this.renderAudio();
            }
        } else {
            this.renderAudio();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserHelper.onStop();
        mMediaBrowserHelper = null;
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
                mMediaBrowserHelper.onStop();
                mMediaBrowserHelper = null;
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

    class PlayAudioRenderer extends WordListAudioRenderer {
        PlayAudioRenderer(WordListData wordListData) {
            super(wordListData);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute( result );
            if ( result && mMediaBrowserHelper != null )
                mMediaBrowserHelper.onStart();
            else
                finish();
        }
    };

    private void renderAudio() {
        PlayAudioRenderer renderAudioAsyncTask = new PlayAudioRenderer( mWordListData );

        renderAudioAsyncTask.process( this );
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
