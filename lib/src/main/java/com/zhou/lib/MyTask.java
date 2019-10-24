package com.zhou.lib;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MyTask extends DefaultTask {

    private ChannelExt channelExt;

    public MyTask() {
        setGroup("V1ǩ��������");
        setDescription("V1ǩ��������");
        channelExt = getProject().getExtensions().getByType(ChannelExt.class);
    }

    @TaskAction
    void run() {
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

        System.out.println("baseFile:" + baseFile);
        System.out.println("channelConfigFile:" + channelConfigFile);
        System.out.println("outDirFile:" + outDirFile);

        outDirFile.mkdirs();//����37=21���ȴ���Ŀ¼��������ôд���ᱨ��

        List<String> channelConfigs = FlavorUtil.getStrListFromFile(channelConfigFile);
        List<String> themeConfigs = FlavorUtil.getStrListFromFile(themeConfigFile);
        //Ȼ����������list�ĳ˻�(����A��4��Ԫ�أ�����B��5��Ԫ�أ����Գ˻�һ����20��Ԫ��)
        List<String> finalFlavors = FlavorUtil.calculateListProduct(channelConfigs, themeConfigs);

        for (String flavorName : finalFlavors) {
            try {
                String outApkPath = channelExt.getOutDir() + "/debug-" + flavorName + ".apk";
                FlavorUtil.makePkg(channelExt.getBaseApkPath(), flavorName, outApkPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}
