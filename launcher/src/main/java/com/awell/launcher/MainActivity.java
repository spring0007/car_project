package com.awell.launcher;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.awell.launcher2.Launcher;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, Launcher.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        Log.i(TAG, "onCreate: huang start activity intent=>" + intent);
        startActivity(intent);

        finish();

    }


    @Override
    public void finish() {
        super.finish();
        Log.i(TAG, "finish: huang finish activity this=>" + this);
    }
}