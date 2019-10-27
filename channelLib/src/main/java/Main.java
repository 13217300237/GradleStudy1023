
import java.io.File;

import channel.ApkBuilder;
import channel.ApkParser;
import channel.ChannelHelper;
import channel.data.Apk;

public class Main {

    public static void main(String[] args) {
        File baseV2Apk = new File("channelLib/app-debug_v2.apk");
        String channel = "xiaomi-black";
        try {
            test(baseV2Apk, channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test(File baseApk, String channel) throws Exception {
        ChannelHelper.reset();
        System.out.println("需要住入的渠道信息是:" + channel);
        long l = System.currentTimeMillis();
        File outDir = new File("channelLib/output");//创建输出目录
        outDir.mkdirs();//生成输出目录
        String name = baseApk.getName();
        name = name.substring(0, name.lastIndexOf("."));
        Apk apk = ApkParser.parser(baseApk);//把apk文件解析成我们自定义的Apk类的对象
        File file = new File(outDir, name + "-" + channel + ".apk");// 生成APK
        ApkBuilder.generateChannel(channel, apk, file);
        System.out.println("注入渠道信息 耗时：" + (System.currentTimeMillis() - l) + " MS");
        System.out.println("解读渠道信息:" + ChannelHelper.getChannel(file.getAbsolutePath()));
        System.out.println("======================");
    }
}
