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

        System.out.println("baseFile:" + baseFile);
        System.out.println("channelConfigFile:" + channelConfigFile);
        System.out.println("outDirFile:" + outDirFile);

        outDirFile.mkdirs();//����37=21���ȴ���Ŀ¼��������ôд���ᱨ��

        Map<String, String> channelMap = new HashMap<>();//��map��������������Լ�������������
        FileReader fr = null;
        BufferedReader br = null;
        try {
            //�������ж�ȡ���������ļ� ��map��ȥ
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
             * ����Ч��̫���ˣ������Ҷ�Ӽ���������,��д̫������ֱ��������Ӱ�
             */
            for (int i = 0; i < 100; i++) {
                channelMap.put("Channel" + i, channelExt.getOutDir() + "/debug-channel" + i + ".apk");
            }


            //����map�����ɶ��
            Set<Map.Entry<String, String>> entries = channelMap.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                System.out.println("��ʼ���" + next.getKey() + "   �����:" + next.getValue());
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
     * �����V1ǩ���Ĳ��Դ���
     *
     * @throws IOException
     */
    private static void makePkg(String basePath, String channelName, String channelApkName) throws IOException {
        File baseApk = new File(basePath);
        //����������Ϣ�����
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(channelApkName));

        //��ԭAPK��������Ϣ������ jos
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

        //���д��һ�� channel �ļ�
        ZipEntry zipEntry = new ZipEntry("META-INF/channel");
        jos.putNextEntry(zipEntry);
        jos.write(channelName.getBytes());
        jos.closeEntry();
        jos.close();
        System.out.println("���·��:" + channelApkName);
    }
}
