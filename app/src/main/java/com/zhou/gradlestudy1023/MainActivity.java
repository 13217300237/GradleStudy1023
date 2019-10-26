package com.zhou.gradlestudy1023;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zhou.channel.FlavorV2Util;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv);
//        tv.setText(FlavorUtil.getV1Flavor(getApplicationInfo().sourceDir));
        tv.setText(FlavorV2Util.getV2Channel(getApplicationInfo().sourceDir));
    }


}
