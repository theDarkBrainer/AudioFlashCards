package com.thedarkbrainer.audioflashcards;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thedarkbrainer.audioflashcards.media_service.MediaBrowserHelper;
import com.thedarkbrainer.audioflashcards.media_service.MusicService;
import com.thedarkbrainer.audioflashcards.media_service.PlayerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int REC_CODE_NEWWORD = 101;
    private final int REC_CODE_EDITWORD = 102;

    private static final int REQUEST_SELECT_FILE_AND_LOAD = 111;
    private static final int REQUEST_SAVE_TO_SSD_WITH_UI = 112;
    private static final int REQUEST_SAVE_ALL = 113;
    private static final int REQUEST_EXPORT_PLAY_AUDIO = 114;

    private static final String EXTERNAL_FILE_PATH_KEY = "ExternalFile";

    private String mReequestSaveAll_File;
    private int mRequestListenWhich;

    private WordListData mWordListData;
    private ListView mWordListView;
    private WordListAdapter mWordListDataAdapter;

    private PlayButtonListener mPlayButtonListener;
    private boolean mIsPlaying = false;

    private class WordListAdapter extends BaseAdapter {

        private WordListData mData;

        WordListAdapter(WordListData data) {
            mData = data;
        }

        @Override
        public int getCount() {
            return mData.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mData.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.word_list_item, container, false);
            }

            WordListData.Data data = mData.getItem(position);

            TextView textGerman = convertView.findViewById(R.id.text_german);
            TextView textInfo = convertView.findViewById(R.id.text_info);
            TextView textEnglish = convertView.findViewById(R.id.text_english);

            textGerman.setText(data.mGerman);
            textInfo.setText(String.format("uses: %d errors: %d", data.mUses, data.mErrors));
            textEnglish.setText(data.mEnglish);

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        this.setSupportActionBar(toolbar);

        mWordListData = new WordListData( getApplicationContext() );

        mWordListDataAdapter = new WordListAdapter(mWordListData);

        mWordListView = findViewById(R.id.wordlist);
        mWordListView.setAdapter(mWordListDataAdapter);
        mWordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WordListData.Data data = (WordListData.Data) mWordListView.getItemAtPosition(position);

                Intent intent = new Intent(MainActivity.this, WordActivity.class);
                try {
                    intent.putExtra(WordActivity.PARAM_POSITION, position);
                    intent.putExtra(WordActivity.PARAM_GERMAN, data.mGerman);
                    intent.putExtra(WordActivity.PARAM_ENGLISH, data.mEnglish);
                    startActivityForResult(intent, REC_CODE_EDITWORD);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),"Cannot run activity.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton addButton = findViewById(R.id.btn_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WordActivity.class);
                try {
                    startActivityForResult(intent, REC_CODE_NEWWORD);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),"Cannot run activity.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPlayButtonListener = new PlayButtonListener();

        FloatingActionButton playButton = findViewById(R.id.btn_play);
        playButton.setOnClickListener( mPlayButtonListener );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = false;

        switch ( item.getItemId() ) {
            case R.id.menu_settings: {
                //Intent i = new Intent(this, ActivityPreferences.class);
                //startActivity(i);
                result = true;
            } break;

            case R.id.menu_clear: {
                this.confirmLossOfData(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mWordListData.clear();
                        mWordListData.save( getApplicationContext() );
                        mWordListDataAdapter.notifyDataSetChanged();
                    }});
            } break;

            case R.id.menu_load: {
                this.confirmLossOfData(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mWordListData.clear();
                        mWordListData.save( getApplicationContext() );

                        if (Build.VERSION.SDK_INT >= 23) {
                            String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};
                            if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_SELECT_FILE_AND_LOAD);
                            } else {
                                MainActivity.this.selectFileAndLoad();
                            }
                        } else {
                            MainActivity.this.selectFileAndLoad();
                        }
                    }});
            } break;

            case R.id.menu_save_as: {
                if (Build.VERSION.SDK_INT >= 23) {
                    String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!hasPermissions(this, PERMISSIONS)) {
                        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_SAVE_TO_SSD_WITH_UI);
                    } else {
                        this.saveToSSDWithUI();
                    }
                } else {
                    this.saveToSSDWithUI();
                }
            } break;

            default:
                result = super.onOptionsItemSelected(item);
        }


        return result;
    }

    private void confirmLossOfData(DialogInterface.OnClickListener yesCallback) {
        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("Do you really want to loose the current word list?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, yesCallback)
                .setNegativeButton(android.R.string.no, null).show();
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".txt")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    private void selectFileAndLoad() {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final List<File> arrFiles = this.getListFiles(downloadFolder);

        List<String> arrChoices = new ArrayList<>();
        for(File file : arrFiles)
            arrChoices.add(file.getName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Load Word List from Downloads");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.choose_file, null, false);
        final ListView input = viewInflated.findViewById(R.id.input);
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrChoices );
        input.setAdapter(itemsAdapter);
        builder.setView(viewInflated);

        final AlertDialog dialog = builder.show();

        input.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();

                File file = arrFiles.get(position);

                try {
                    mWordListData.load( new FileInputStream(file) );
                    mWordListDataAdapter.notifyDataSetChanged();

                    if (Build.VERSION.SDK_INT >= 23) {
                        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_SAVE_ALL);
                        } else {
                            mWordListData.save(getApplicationContext());
                        }
                    } else {
                        mWordListData.save(getApplicationContext());
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),"Could not load file!",Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveAllWithPermission() {
        mWordListData.save(getApplicationContext());

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mReequestSaveAll_File = sharedPref.getString(EXTERNAL_FILE_PATH_KEY, "");
        if ( ! mReequestSaveAll_File.isEmpty() ) {
            saveToSSD(mReequestSaveAll_File);

            if (Build.VERSION.SDK_INT >= 23) {
                String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if (!hasPermissions(this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_SAVE_ALL);
                } else {
                    this.saveToSSD(mReequestSaveAll_File);
                }
            } else {
                this.saveToSSD(mReequestSaveAll_File);
            }

        }
    }

    private void saveToSSDWithUI() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String fileName = sharedPref.getString(EXTERNAL_FILE_PATH_KEY, "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save the Word List in Downloads");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.filename_input, null, false);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText( fileName );
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                String fileName = input.getText().toString();

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString( EXTERNAL_FILE_PATH_KEY, fileName );
                editor.apply();

                saveToSSD(fileName);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveToSSD(String fileName) {
        if ( ! fileName.endsWith(".txt"))
            fileName += ".txt";

        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadFolder, fileName);
        try {
            mWordListData.save( new FileOutputStream(file) );
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Could not save file!",Toast.LENGTH_LONG).show();
            Log.e("Saving file", e.toString());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REC_CODE_EDITWORD: {
                if ( data != null ) {
                    int pos = data.getIntExtra(WordActivity.PARAM_POSITION, -1);
                    if (pos >= 0) {
                        WordListData.Data wordData = mWordListData.getItem(pos);
                        wordData.mGerman = data.getStringExtra(WordActivity.PARAM_GERMAN);
                        wordData.mEnglish = data.getStringExtra(WordActivity.PARAM_ENGLISH);

                        if (wordData.isEmpty()) {
                            mWordListData.removeItem( pos );
                        }
                        saveAllWithPermission();
                        mWordListDataAdapter.notifyDataSetChanged();
                    }
                }
            } break;

            case REC_CODE_NEWWORD: {
                if ( data != null ) {
                    WordListData.Data wordData = new WordListData.Data();
                    wordData.mGerman = data.getStringExtra(WordActivity.PARAM_GERMAN);
                    wordData.mEnglish = data.getStringExtra(WordActivity.PARAM_ENGLISH);

                    if (!wordData.isEmpty()) {
                        mWordListData.addItem(wordData);
                        saveAllWithPermission();

                        mWordListDataAdapter.notifyDataSetChanged();
                    }
                }
            } break;
        }
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

            case REQUEST_SELECT_FILE_AND_LOAD: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectFileAndLoad();
                } else {
                    Toast.makeText(this, "The app was not allowed to read from your storage", Toast.LENGTH_LONG).show();
                }
            } break;

            case REQUEST_SAVE_TO_SSD_WITH_UI: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveToSSDWithUI();
                } else {
                    Toast.makeText(this, "The app was not allowed to write in your storage", Toast.LENGTH_LONG).show();
                }
            } break;

            case REQUEST_SAVE_ALL: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWordListData.save( getApplicationContext() );
                    this.saveToSSD(mReequestSaveAll_File);
                } else {
                    mWordListData.save( getApplicationContext() );
                    Toast.makeText(this, "The app was not allowed to write in your storage", Toast.LENGTH_LONG).show();
                }
            } break;

            case REQUEST_EXPORT_PLAY_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPlayButtonListener.renderPlayAudio();
                } else {
                    Toast.makeText(getApplicationContext(), "Requesting writing denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    class PlayButtonListener implements View.OnClickListener {

        private MediaBrowserHelper mMediaBrowserHelper;

        @Override
        public void onClick(View v) {
            if ( mMediaBrowserHelper != null && mIsPlaying ) {
                playWords();
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.select_play_mode)
                        .setItems(R.array.play_modes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        playWords();
                                        break;
                                    case 1:
                                        exerciseWords(true, true);
                                        break;
                                    case 2:
                                        exerciseWords(false, true);
                                        break;
                                    case 3:
                                        exerciseWords(true, false);
                                        break;
                                    case 4:
                                        exerciseWords(false, false);
                                        break;
                                    default:
                                        exerciseWords(true, false);
                                        break;
                                }
                            }
                        });
                builder.create().show();
            }
        }

        private void exerciseWords(boolean doGerman, boolean withAudio) {
            if (mMediaBrowserHelper != null) {
                mMediaBrowserHelper.onStop();
                mMediaBrowserHelper = null;
            }

            Intent intent = new Intent(MainActivity.this, PlayActivity.class);
            intent.putExtra(PlayActivity.PARAM_DO_GERMAN, doGerman);
            intent.putExtra(PlayActivity.PARAM_DO_AUDIO, withAudio);
            intent.putExtra(PlayActivity.PARAM_WORD_LIST, mWordListData);
            startActivity(intent);
        }

        private void playWords() {
            if ( mMediaBrowserHelper == null ) {
                mMediaBrowserHelper = new MediaBrowserConnection(MainActivity.this, null);
                mMediaBrowserHelper.registerCallback(new MediaBrowserListener());

                if (Build.VERSION.SDK_INT >= 23) {
                    String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_EXPORT_PLAY_AUDIO);
                    } else {
                        this.renderPlayAudio();
                    }
                } else {
                    this.renderPlayAudio();
                }
            }
            else {
                if (mIsPlaying)
                    mMediaBrowserHelper.getTransportControls().pause();
                else
                    mMediaBrowserHelper.getTransportControls().play();
            }
        }

        private void renderPlayAudio() {
            PlayAudioRenderer renderAudioAsyncTask = new PlayAudioRenderer( mWordListData );
            renderAudioAsyncTask.process( MainActivity.this );
        }

        class PlayAudioRenderer extends WordListAudioRenderer {
            PlayAudioRenderer(WordListData wordListData) {
                super(wordListData);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute( result );

                FloatingActionButton playButton = findViewById(R.id.btn_play);
                if ( result && mMediaBrowserHelper != null ) {
                    mMediaBrowserHelper.onStart();
                }
            }
        };
    }

    private class MediaBrowserListener extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            mIsPlaying = playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            FloatingActionButton playButton = findViewById(R.id.btn_play);
            playButton.setImageResource( mIsPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play );
        }
    }
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
}
