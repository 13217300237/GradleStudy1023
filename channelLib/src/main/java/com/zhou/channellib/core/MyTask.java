package com.zhou.channellib.core;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.List;

import channel.ApkBuilder;
import channel.ApkParser;
import channel.data.Apk;

class MyTask extends DefaultTask {

    private ChannelExt channelExt;

    public MyTask() {
        setGroup("V1ǩ��������");
        setDescription("V1ǩ��������");
        channelExt = getProject().getExtensions().getByType(ChannelExt.class);
    }

    @TaskAction
    void run() throws Exception {
        System.out.println("==============��������������߼�===============" + channelExt);
        //��ȡ���úõĲ���
        if (channelExt == null || !channelExt.isOk()) {
            System.out.println(" û��ȡ�ñ���Ĳ���...");
            return;
        }
        File baseFile = new File(channelExt.getBaseApkPath());
        File channelConfigFile = new File(channelExt.getChannelConfigPath());
        File outDirFile = new File(channelExt.getOutDir());
        File themeConfigFile = new File(channelExt.getThemeConfigPath());
        outDirFile.mkdirs();
        List<String> channelConfigs = FlavorUtil.getStrListFromFile(channelConfigFile);
        List<String> themeConfigs = FlavorUtil.getStrListFromFile(themeConfigFile);
        //Ȼ����������list�ĳ˻�(����A��4��Ԫ�أ�����B��5��Ԫ�أ����Գ˻�һ����20��Ԫ��)
        List<String> finalFlavors = FlavorUtil.calculateListProduct(channelConfigs, themeConfigs);
        for (String flavorName : finalFlavors) {
            Apk apk = ApkParser.parser(baseFile);//2������APK(zip�ļ�)
            File file = new File(outDirFile, "app-debug-" + flavorName + ".apk");
            ApkBuilder.generateChannel(flavorName, apk, file);//3������APK
        }

    }


}
