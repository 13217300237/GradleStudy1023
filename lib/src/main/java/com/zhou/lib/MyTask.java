package com.zhou.lib;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

        System.out.println("baseFile:" + baseFile);
        System.out.println("channelConfigFile:" + channelConfigFile);
        System.out.println("outDirFile:" + outDirFile);

        outDirFile.mkdirs();//不管37=21，先创建目录，反正这么写不会报错

        Map<String, String> channelMap = new HashMap<>();//用map来存放渠道名，以及渠道包的名字
        FileReader fr = null;
        BufferedReader br = null;
        try {
            //来，逐行读取渠道配置文件 到map中去
            fr = new FileReader(channelConfigFile);
            br = new BufferedReader(fr);
            String currentLine;
            StringBuilder sb;
            while ((currentLine = br.readLine()) != null) {
                sb = new StringBuilder(channelExt.getOutDir());
                sb.append("/debug-");
                sb.append(currentLine);
                sb.append(".apk");
                channelMap.put(currentLine, sb.toString());
            }

            /**
             * 由于效率太高了，所以我多加几个渠道包,手写太慢，我直接在这里加吧
             */
            for (int i = 0; i < 100; i++) {
                channelMap.put("Channel" + i, channelExt.getOutDir() + "/debug-channel" + i + ".apk");
            }


            //遍历map，生成多个
            Set<Map.Entry<String, String>> entries = channelMap.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                System.out.println("开始打包" + next.getKey() + "   输出到:" + next.getValue());
                makePkg(channelExt.getBaseApkPath(), next.getKey(), next.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getLocalizedMessage());

        } finally {
            try {
                if (fr != null)
                    fr.close();
                if (br != null)
                    br.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * 这个是V1签名的测试代码
     *
     * @throws IOException
     */
    private static void makePkg(String basePath, String channelName, String channelApkName) throws IOException {
        File baseApk = new File(basePath);
        //加入渠道信息后输出
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(channelApkName));

        //将原APK中所有信息拷贝到 jos
        ZipFile jarFile = new ZipFile(baseApk);
        Enumeration<? extends ZipEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            jos.putNextEntry(new ZipEntry(zipEntry.getName()));
            InputStream is = jarFile.getInputStream(zipEntry);
            byte[] buffer = new byte[2048];
            int len;
            while ((len = is.read(buffer)) != -1) {
                jos.write(buffer, 0, len);
            }
            jos.closeEntry();
        }

        //最后写入一个 channel 文件
        ZipEntry zipEntry = new ZipEntry("META-INF/channel");
        jos.putNextEntry(zipEntry);
        jos.write(channelName.getBytes());
        jos.closeEntry();
        jos.close();
        System.out.println("输出路径:" + channelApkName);
    }
}
