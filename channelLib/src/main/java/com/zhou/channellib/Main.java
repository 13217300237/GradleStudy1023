package com.zhou.channellib;

import com.zhou.channellib.tools.Apk;
import com.zhou.channellib.tools.ApkBuilder;
import com.zhou.channellib.tools.ApkParser;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        File baseApk = new File("channellib/app-debug_v2.apk");
        File outApk = new File("channellib/app-debug_v2_out.apk");
        Apk apk = ApkParser.parse(baseApk);
        System.out.println("��׼apk�Ĵ�С��:" + baseApk.length());
        System.out.println("��һ��ContentOfZipEntries��size��:" + apk.coze.byteArr.length);
        System.out.println("�ڶ���ApkSigningBlock��offset��:" + apk.asb.offset + "  -size ��:" + apk.asb.byteArr.length);
        System.out.println("������CentralDirectory��offset��:" + apk.cd.offset + "       -size:" + apk.cd.byteArr.length);
        System.out.println("������EndOfCentralDirectory��size��:" + apk.eocd.byteArr.length);

        //��Ȼһ��apk�Ѿ������ǽ��������ˣ���ô�����ǿ���������ע��������Ϣ��
        try {
            ApkBuilder.generate("zhouzhou", apk, outApk);
            System.out.println("out apk�Ĵ�С��:" + outApk.length());

            //�ټ��һ���µ��ļ�
            String v2Channel = ApkParser.getV2Channel(outApk.getAbsolutePath());
            System.out.println(v2Channel);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
