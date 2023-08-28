package com.example.assignmentapp10.NFCReader;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;

import com.example.assignmentapp10.MainActivity;
import com.example.assignmentapp10.R;


public class NFCMainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcmain);


    }


    public void onClick(View v) {
        Intent[] intents = new Intent[1];
        intents[0] = new Intent(NFCMainActivity.this, MainActivity.class);
        startActivities(intents);
        finish();
    }
}