package com.thedarkbrainer.audioflashcards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class YourBroadcastReceiver extends BroadcastReceiver {

    // Constructor is mandatory
    public YourBroadcastReceiver ()
    {
        super ();
        Log.i ("YourBroadcastReceiver", ".ctor");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        Log.i ("YourBroadcastReceiver", intentAction.toString() + " happended");
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            Log.i ("YourBroadcastReceiver", "no media button information");
            //Toast.makeText(PlayBackgroundService.this.getApplicationContext(), "Audio Focus GAIN", Toast.LENGTH_SHORT).show();
            return;
        }
        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) {
            Log.i ("PlayerService", "no keypress");
            //Toast.makeText(PlayBackgroundService.this.getApplicationContext(), "Audio Focus GAIN", Toast.LENGTH_SHORT).show();
            return;
        }
        // other stuff you want to do
    }
}
