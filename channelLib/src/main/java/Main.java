
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
        System.out.println("��Ҫס���������Ϣ��:" + channel);
        long l = System.currentTimeMillis();
        File outDir = new File("channelLib/output");//�������Ŀ¼
        outDir.mkdirs();//�������Ŀ¼
        String name = baseApk.getName();
        name = name.substring(0, name.lastIndexOf("."));
        Apk apk = ApkParser.parser(baseApk);//��apk�ļ������������Զ����Apk��Ķ���
        File file = new File(outDir, name + "-" + channel + ".apk");// ����APK
        ApkBuilder.generateChannel(channel, apk, file);
        System.out.println("ע��������Ϣ ��ʱ��" + (System.currentTimeMillis() - l) + " MS");
        System.out.println("���������Ϣ:" + ChannelHelper.getChannel(file.getAbsolutePath()));
        System.out.println("======================");
    }
}
