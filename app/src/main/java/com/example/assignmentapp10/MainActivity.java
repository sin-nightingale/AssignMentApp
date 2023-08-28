package com.example.assignmentapp10;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assignmentapp10.Bluetooth.BlueMActivity;
import com.example.assignmentapp10.Bluetooth.BlueMServerActivity;
import com.example.assignmentapp10.Maps.MapsActivity;
import com.example.assignmentapp10.Message.MessActivity;
import com.example.assignmentapp10.NFCReader.NFCMainActivity;
import com.example.assignmentapp10.NFCReader.NfcActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button b1 = (Button) findViewById(R.id.button1);
        Button b2 = (Button) findViewById(R.id.button2);
        Button b3 = (Button) findViewById(R.id.button3);
        Button b4 = (Button) findViewById(R.id.button4);
        Button b5 = (Button) findViewById(R.id.button5);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MainActivity.this, MapsActivity.class);
                startActivities(intents);
                finish();
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MainActivity.this, BlueMActivity.class);
                startActivities(intents);
                finish();
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MainActivity.this, BlueMServerActivity.class);
                startActivities(intents);
                finish();
            }
        });
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MainActivity.this, MessActivity.class);
                startActivities(intents);
                finish();
            }
        });
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent[] intents = new Intent[1];
                intents[0] = new Intent(MainActivity.this, NFCMainActivity.class);
                startActivities(intents);
                finish();
            }
        });
    }
}