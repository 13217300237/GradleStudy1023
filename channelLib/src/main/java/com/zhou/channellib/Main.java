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
        System.out.println("基准apk的大小是:" + baseApk.length());
        System.out.println("第一区ContentOfZipEntries的size是:" + apk.coze.byteArr.length);
        System.out.println("第二区ApkSigningBlock的offset是:" + apk.asb.offset + "  -size 是:" + apk.asb.byteArr.length);
        System.out.println("第三区CentralDirectory的offset是:" + apk.cd.offset + "       -size:" + apk.cd.byteArr.length);
        System.out.println("第四区EndOfCentralDirectory的size是:" + apk.eocd.byteArr.length);

        //既然一个apk已经被我们解析出来了，那么，我们可以往里面注入渠道信息了
        try {
            ApkBuilder.generate("zhouzhou", apk, outApk);
            System.out.println("out apk的大小是:" + outApk.length());

            //再检查一下新的文件
            String v2Channel = ApkParser.getV2Channel(outApk.getAbsolutePath());
            System.out.println(v2Channel);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
