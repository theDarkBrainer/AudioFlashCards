package com.thedarkbrainer.audioflashcards;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class PlayBackgroundService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {

    public static final String COMMAND_ANSER_OK = "AnswerOK";
    public static final String COMMAND_ANSER_FAIL = "AnswerFail";
    public static final String COMMAND_REPLAY = "Replay";
    public static final String COMMAND_NEXT = "Next";
    public static final String COMMAND_SKIP = "Skip";
    public static final String COMMAND_OTHER_REPLY = "OtherReply";
    public static final String COMMAND_LISTEN = "Listen";

    public static final String PARAM_WORDLIST = "WordsList";
    public static final String PARAM_PLAYMODE = "playMode";

    public static final String RESPONCE_RECEIVER = "com.thedarkbrainer.audioflashcards.playservice";
    public static final String PARAM_MODE = "Mode";
    public static final String PARAM_WORD = "Word";
    public static final String PARAM_ANSWER = "Answer";
    public static final String PARAM_ANSWER1 = "Answer1";
    public static final String PARAM_ANSWER2 = "Answer2";

    private static final String CHANNEL_ID = "my_channel_id";
    private static final int ONGOING_NOTIFICATION_ID = 1338;

    private WordListData mWordListData;
    private WordListData.ComplexIterator mWorldIterator;

    private AudioManager mAudioManager;
    private AudioAttributes mPlaybackAttributes;
    private AudioFocusRequest mFocusRequest;

    private PlayerBox mMediaPlayer;
    private MediaSessionCompat mMediaSessionCompat;
    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            Log.i("PlayerService", "mMediaSessionCallback: onPlay");
            super.onPlay();

            mMediaPlayer.play();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        }

        @Override
        public void onPause() {
            Log.i("PlayerService", "mMediaSessionCallback: onPause");
            super.onPause();

            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }

            stopSelf();
        }

        @Override
        public void onStop() {
            Log.i("PlayerService", "mMediaSessionCallback: onStop");
            super.onStop();

            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }

            stopSelf();
        }

        @Override
        public boolean onMediaButtonEvent (Intent mediaButtonEvent) {
            Log.i("PlayerService","mediaButtonEvent "+mediaButtonEvent.getAction());
            return super.onMediaButtonEvent( mediaButtonEvent );
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.i("PlayerService", "mMediaSessionCallback: onPlayFromMediaId");
            super.onPlayFromMediaId(mediaId, extras);

            mWordListData = (WordListData) extras.getSerializable(PARAM_WORDLIST);
            PlayerBox.PlayMode playMode = PlayerBox.PlayMode.values()[ extras.getInt(PARAM_PLAYMODE, 0) ];

            switch (playMode) {
                case Quiet:
                case SpeakGerman_SpeakEnglish:
                case SpeakEnglish_SpeakGerman:
                    mWorldIterator = mWordListData.iterator_random();
                    mWorldIterator = mWordListData.iterator_random();
                    break;

                case SpeakGerman_ListenEnglish:
                case SpeakEnglish_ListenGerman:
                case SpeakGerman_ClickEnglish:
                case SpeakEnglish_ClickGerman:
                    mWorldIterator = mWordListData.iterator_sm2();
                    break;
            }

            mMediaPlayer = new PlayerBox(getApplicationContext(), playMode, new PlayerBox.OnCompletionListener() {
                @Override
                public void onCompleted() {
                    mMediaSessionCompat.getController().sendCommand(COMMAND_NEXT, null, null);
                }
            });

            mMediaPlayer.play();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        }

        @Override
        public void onSkipToNext() {
            Log.i("PlayerService","onSkipToNext");
            mMediaSessionCompat.getController().sendCommand(COMMAND_NEXT, null, null);
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);

            Log.i("PlayerService", "mMediaSessionCallback: onCommand: " + command);
            if ( mMediaPlayer.isPlaying() ) {
                PlayerBox.OnCompletionListener doneReplying = new PlayerBox.OnCompletionListener() {
                    @Override
                    public void onCompleted() {
                        mMediaSessionCompat.getController().sendCommand(COMMAND_NEXT, null, null);
                        updateNotification();
                    }
                };

                if (COMMAND_ANSER_OK.equalsIgnoreCase(command)) {
                    mWorldIterator.setCurrentAnswer(true);
                    try {
                        mMediaPlayer.speak_other("Richtig. ", "Correct. ", doneReplying);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        mMediaSessionCompat.getController().sendCommand(COMMAND_REPLAY, null, null);
                    }
                    updateNotification();
                } else if (COMMAND_ANSER_FAIL.equalsIgnoreCase(command)) {
                    mWorldIterator.setCurrentAnswer(false);
                    mWorldIterator.get().mErrors++;
                    try {
                        mMediaPlayer.speak_other("Falsch. ", "Incorrect. ", doneReplying);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        mMediaSessionCompat.getController().sendCommand(COMMAND_REPLAY, null, null);
                    }
                    updateNotification();
                } else if (COMMAND_REPLAY.equalsIgnoreCase(command)) {
                    PlayBackgroundService.this.playWord();
                } else if (COMMAND_NEXT.equalsIgnoreCase(command)) {
                    mWorldIterator.next();
                    PlayBackgroundService.this.playWord();
                } else if (COMMAND_SKIP.equalsIgnoreCase(command)) {
                    mWorldIterator.next();
                    PlayBackgroundService.this.playWord();
                } else if (COMMAND_OTHER_REPLY.equalsIgnoreCase(command)) {
                    WordListData.Data currWord = mWorldIterator.get();
                    try {
                        mMediaPlayer.speak_other(currWord.mGerman, currWord.mEnglish, doneReplying);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        mMediaSessionCompat.getController().sendCommand(COMMAND_REPLAY, null, null);
                    }
                    updateNotification();
                } else if (COMMAND_LISTEN.equalsIgnoreCase(command)) {
                    TheListener.OnListener doneListening = new TheListener.OnListener() {
                        @Override
                        public void OnResult(ArrayList<String> matches) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (matches != null && matches.size() > 0)
                                matchWord(matches.get(0));
                            else {
                                mMediaSessionCompat.getController().sendCommand(COMMAND_NEXT, null, null);
                            }

                        }
                    };

                    mMediaPlayer.listen(doneListening);
                }
            }
        }
    };

    public void onCreate() {
        super.onCreate();
        Log.i("PlayerService", "onCreate");

        initMediaSession();

        createNotificationChannel();
        startForeground(ONGOING_NOTIFICATION_ID,  buildForegroundNotification(getString(R.string.app_name), ""));

        mAudioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        mPlaybackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(this)
                .build();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("PlayerService", "onDestroy");

        if (mAudioManager != null)
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);

        mMediaSessionCompat.getController().getTransportControls().stop();
        mMediaSessionCompat.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("PlayerService", "onStartCommand");
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.i("PlayerService", "onAudioFocusChange: " + focusChange);
        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                Log.i("PlayerService", "onAudioFocusChange: AUDIOFOCUS_LOSS");
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(PlayBackgroundService.RESPONCE_RECEIVER);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra(PlayBackgroundService.PARAM_MODE, "Quit");
                sendBroadcast(broadcastIntent);

                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                Log.i("PlayerService", "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT");
                //mMediaPlayer.pause();
                //updateNotification();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                Log.i("PlayerService", "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                if( mMediaPlayer != null ) {
                    //mMediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                Log.i("PlayerService", "onAudioFocusChange: AUDIOFOCUS_GAIN");
                //if( mMediaPlayer != null ) {
                //    if( !mMediaPlayer.isPlaying() ) {
                //        mMediaSessionCompat.getController().sendCommand(COMMAND_NEXT, null, null);
                //    }
                //}
                //updateNotification();
                break;
            }
            default:
                Log.i("PlayerService", "onAudioFocusChange: " + focusChange);
                break;
        }
    }

    private void initMediaSession() {
        //Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        //mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        //PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        PendingIntent pendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                mediaButtonReceiver,
                PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | KeyEvent.ACTION_DOWN);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "PlayerService", mediaButtonReceiver, pendingIntent);

        mMediaSessionCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        Log.i("PlayerService", "setMediaPlaybackState: "+state);
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | KeyEvent.ACTION_DOWN);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | KeyEvent.ACTION_DOWN);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "getString(R.string.channel_name)";
            String description = "getString(R.string.channel_description)";
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildForegroundNotification(String title, String text) {
        Log.i("PlayerService", "buildForegroundNotification");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);

        builder
                // Add the metadata for the currently playing track
                .setContentTitle(title)
                .setContentText(text)
                .setSubText("description.getDescription()")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))

                // Enable launching the player by clicking the notification
                .setContentIntent(mMediaSessionCompat.getController().getSessionActivity())

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add an app icon and set its accent color
                // Be careful about the color
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
        ;

        if ( mMediaPlayer != null && mMediaPlayer.isPlaying() ) {
            // Add a pause button
            builder.addAction(new NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_PAUSE)));
        }
        else {
            // Add a play button
            builder.addAction(new NotificationCompat.Action(
                    android.R.drawable.ic_media_play, "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_PLAY)));
        }

        builder
                // Take advantage of MediaStyle features
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSessionCompat.getSessionToken())
                        .setShowActionsInCompactView(0)

                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                                PlaybackStateCompat.ACTION_STOP)))

                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            ;

        return(builder.build());
    }

    private void updateNotification() {
        Log.i("PlayerService", "updateNotification");
        final WordListData.Data currWord = mWorldIterator.get();
        Notification notification = buildForegroundNotification(currWord.mGerman, currWord.mEnglish);
        NotificationManagerCompat.from(getApplicationContext()).notify(ONGOING_NOTIFICATION_ID, notification);
    }

    private void updateMetadata(String title, String artist, String album) {
        Log.i("PlayerService", "updateMetadata");
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
        //metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        //metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private void playWord() {
        Log.i("PlayerService", "playWord");

        final WordListData.Data currWord = mWorldIterator.get();
        currWord.mUses++;

        int res = mAudioManager.requestAudioFocus(mFocusRequest);
        if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {

        } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //playbackNowAuthorized = true;
            //playbackNow();

            1(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {

        }

        updateMetadata(currWord.mGerman, currWord.mEnglish, "");

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(PlayBackgroundService.RESPONCE_RECEIVER);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PlayBackgroundService.PARAM_MODE, "SetWord");
        broadcastIntent.putExtra(PlayBackgroundService.PARAM_WORD, mMediaPlayer.getCurrentWord(currWord.mGerman, currWord.mEnglish));
        sendBroadcast(broadcastIntent);

        PlayerBox.OnCompletionListener doneSpeaking = null;

        switch (mMediaPlayer.getPlayMode()) {
            case Quiet:
                break;

            case SpeakGerman_SpeakEnglish:
            case SpeakEnglish_SpeakGerman:
                doneSpeaking = new PlayerBox.OnCompletionListener() {
                    @Override
                    public void onCompleted() {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaSessionCompat.getController().sendCommand(COMMAND_OTHER_REPLY, null, null);
                        }
                    }
                };
                break;

            case SpeakGerman_ListenEnglish:
            case SpeakEnglish_ListenGerman:
                doneSpeaking = new PlayerBox.OnCompletionListener() {
                    @Override
                    public void onCompleted() {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaSessionCompat.getController().sendCommand(COMMAND_LISTEN, null, null);
                        }
                    }
                };
                break;

            case SpeakGerman_ClickEnglish:
            case SpeakEnglish_ClickGerman:
                doneSpeaking = new PlayerBox.OnCompletionListener() {
                    @Override
                    public void onCompleted() {
                        if (mMediaPlayer.isPlaying()) {
                            Log.i("PlayerService", "broadcast click exam...");
                            Intent broadcastIntent = new Intent();
                            broadcastIntent.setAction(PlayBackgroundService.RESPONCE_RECEIVER);
                            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                            broadcastIntent.putExtra(PlayBackgroundService.PARAM_MODE, "ClickExam");
                            if (mMediaPlayer.getPlayMode() == PlayerBox.PlayMode.SpeakGerman_ClickEnglish) {
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER, currWord.mEnglish);
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER1, mWorldIterator.getRandomExcudingCurrent().mEnglish);
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER2, mWorldIterator.getRandomExcudingCurrent().mEnglish);
                            } else {
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER, currWord.mGerman);
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER1, mWorldIterator.getRandomExcudingCurrent().mGerman);
                                broadcastIntent.putExtra(PlayBackgroundService.PARAM_ANSWER2, mWorldIterator.getRandomExcudingCurrent().mGerman);
                            }
                            sendBroadcast(broadcastIntent);
                            Log.i("PlayerService", "... click exam broadcast done.");
                        }
                    }
                };
                break;
        }

        if (doneSpeaking != null) {
            try {
                mMediaPlayer.speak_main(currWord.mGerman, currWord.mEnglish, doneSpeaking);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        updateNotification();
    }

    private static int levensteinDistance(String src, String dest)
    {
        int[][] d = new int[src.length() + 1][ dest.length() + 1];
        int i, j, cost;
        char[] str1 = src.toCharArray();
        char[] str2 = dest.toCharArray();

        for (i = 0; i <= str1.length; i++)
        {
            d[i][0] = i;
        }
        for (j = 0; j <= str2.length; j++)
        {
            d[0][j] = j;
        }
        for (i = 1; i <= str1.length; i++)
        {
            for (j = 1; j <= str2.length; j++)
            {

                if (str1[i - 1] == str2[j - 1])
                    cost = 0;
                else
                    cost = 1;

                d[i][j] =
                        Math.min(
                                d[i - 1][j] + 1,              // Deletion
                                Math.min(
                                        d[i][j - 1] + 1,          // Insertion
                                        d[i - 1][j - 1] + cost)); // Substitution

                if ((i > 1) && (j > 1) && (str1[i - 1] ==
                        str2[j - 2]) && (str1[i - 2] == str2[j - 1]))
                {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost);
                }
            }
        }

        return d[str1.length][str2.length];
    }

    private void matchWord(String word)
    {
        Log.i("PlayerService", "matchWord: " + word);

        WordListData.Data currWord = mWorldIterator.get();

        int matchScore = levensteinDistance( word, mMediaPlayer.getPlayMode() == PlayerBox.PlayMode.SpeakEnglish_ListenGerman ? currWord.mGerman : currWord.mEnglish );

        boolean correctAnswer = matchScore <= 5;

        String msg = correctAnswer ? COMMAND_ANSER_OK : COMMAND_ANSER_FAIL;

        mMediaSessionCompat.getController().sendCommand(msg, null, null);
    }
}
