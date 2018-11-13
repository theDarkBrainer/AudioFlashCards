package com.thedarkbrainer.audioflashcards;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

public class WordActivity extends AppCompatActivity {

    public static final String PARAM_POSITION = "Pos";
    public static final String PARAM_GERMAN = "German";
    public static final String PARAM_ENGLISH = "English";

    private int mEditPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        mEditPosition = intent.getIntExtra(PARAM_POSITION, 0);
        String germanText = intent.getStringExtra(PARAM_GERMAN);
        String englishText = intent.getStringExtra(PARAM_ENGLISH);

        EditText editGerman = findViewById(R.id.edit_german);
        EditText editEnglish = findViewById(R.id.edit_english);

        editGerman.setText(germanText);
        editEnglish.setText(englishText);

        if(editGerman.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        EditText editGerman = findViewById(R.id.edit_german);
        EditText editEnglish = findViewById(R.id.edit_english);

        Intent intent = new Intent();
        intent.putExtra(PARAM_POSITION, mEditPosition);
        intent.putExtra(PARAM_GERMAN, editGerman.getText().toString());
        intent.putExtra(PARAM_ENGLISH, editEnglish.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.menu_player, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent = new Intent();

        switch (item.getItemId()) {
            case android.R.id.home:
                EditText editGerman = findViewById(R.id.edit_german);
                EditText editEnglish = findViewById(R.id.edit_english);

                intent.putExtra(PARAM_POSITION, mEditPosition);
                intent.putExtra(PARAM_GERMAN, editGerman.getText().toString());
                intent.putExtra(PARAM_ENGLISH, editEnglish.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
                return true;

            case R.id.mnu_delete:
                setResult(RESULT_OK, intent);
                intent.putExtra(PARAM_POSITION, mEditPosition);
                intent.putExtra(PARAM_GERMAN, "");
                intent.putExtra(PARAM_ENGLISH, "");
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
