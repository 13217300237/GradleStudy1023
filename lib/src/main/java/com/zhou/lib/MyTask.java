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
        setGroup("V1签名渠道包");
        setDescription("V1签名渠道包");
        channelExt = getProject().getExtensions().getByType(ChannelExt.class);
    }

    @TaskAction
    void run() {
        System.out.println("==============进入渠道包打包逻辑===============" + channelExt);
        //读取配置好的参数
        if (channelExt == null || !channelExt.isOk()) {
            System.out.println(" 没有取得必须的参数...");
            return;
        }
        File baseFile = new File(channelExt.getBaseApkPath());
        File channelConfigFile = new File(channelExt.getChannelConfigPath());
        File outDirFile = new File(channelExt.getOutDir());
        File themeConfigFile = new File(channelExt.getThemeConfigPath());

        System.out.println("baseFile:" + baseFile);
        System.out.println("channelConfigFile:" + channelConfigFile);
        System.out.println("outDirFile:" + outDirFile);

        outDirFile.mkdirs();//不管37=21，先创建目录，反正这么写不会报错

        List<String> channelConfigs = FlavorUtil.getStrListFromFile(channelConfigFile);
        List<String> themeConfigs = FlavorUtil.getStrListFromFile(themeConfigFile);
        //然后计算出两个list的乘积(数组A有4个元素，数组B有5个元素，所以乘积一共有20个元素)
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
