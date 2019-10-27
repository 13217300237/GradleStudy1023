
import java.io.File;

import channel.ApkBuilder;
import channel.ApkParser;
import channel.data.Apk;

public class Main {


    public static void main(String[] args) throws Exception {
        long l = System.currentTimeMillis();
        /**
         * 1、初始化:创建输出目录、读取渠道文件
         */
        File baseApk = new File("channelLib/app-debug.apk");
        File outDir = new File("channelLib/output");
        outDir.mkdirs();
        String name = baseApk.getName();
        name = name.substring(0, name.lastIndexOf("."));
        String channel = "zhouzhouxaaaa";
        /**
         * 2、解析APK(zip文件)
         */
        Apk apk = ApkParser.parser(baseApk);
        /**
         * 3、生成APK
         */
        File file = new File(outDir, name + "-" + channel +
                ".apk");
        ApkBuilder.generateChannel(channel, apk, file);
        System.out.println(System.currentTimeMillis() - l);
    }
}
