package com.zhou.gradlestudy1023;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("currentChannel", "" + getV1Channel(this));
    }

    public static String getV1Channel(Context context) {
        //当前app的apk文件
        String sourceDir = context.getApplicationInfo().sourceDir;
        ZipFile zipfile = null;
        StringBuilder channel = new StringBuilder();
        try {
            // 遍历APK中所有文件
            zipfile = new ZipFile(sourceDir);
            Enumeration<? extends ZipEntry> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // 读取 META-INF/channel 中的信息（渠道信息）
                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/channel")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipfile
                            .getInputStream(entry)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        channel.append(line);
                    }
                    br.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return channel.toString();
    }
}
