package com.zhou.gradlestudy1023;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import channel.ChannelHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv);

        String v2Channel = ChannelHelper.getChannel(getApplicationInfo().sourceDir);
        tv.setText(v2Channel);
    }


}
