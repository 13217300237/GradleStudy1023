package com.zhou.channellib.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ��ζ��ȡ������
 */
public class FlavorUtil {
    static final String flavorDirName = "flavor";

    /**
     * ���V1ǩ���ķ�ζ
     *
     * @param sourceDir apk�ļ���path
     * @return
     */
    public static String getV1Flavor(String sourceDir) {
        //��ǰapp��apk�ļ�
        ZipFile zipfile = null;
        StringBuilder channel = new StringBuilder();
        try {
            // ����APK�������ļ�
            zipfile = new ZipFile(sourceDir);
            Enumeration<? extends ZipEntry> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // ��ȡ META-INF/channel �е���Ϣ��������Ϣ��
                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/" + flavorDirName)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipfile
                            .getInputStream(entry)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        channel.append(line);
                    }
                    br.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return channel.toString();
    }

    /**
     * �����V1ǩ���Ĳ��Դ���
     * <p>
     * 1. �ӻ�׼apk����һ�� flavor apk��������ȫһ��
     * 2. �ڸոո��Ƴ���flavor apk�У�д��������Ϣ
     *
     * @throws IOException
     */
    public static void makePkg(String basePath, String flavorName, String outApkName) throws IOException {
        File baseApk = new File(basePath);
        //����������Ϣ�����
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(outApkName));

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
        ZipEntry zipEntry = new ZipEntry("META-INF/" + FlavorUtil.flavorDirName);
        jos.putNextEntry(zipEntry);
        jos.write(flavorName.getBytes());//�ѷ�ζ��Ϣд�뵽 META-INF���½����ļ���
        jos.closeEntry();
        jos.close();
        System.out.println("���·��:" + outApkName);
    }

    static List<String> getStrListFromFile(File f) {
        List<String> strList = new ArrayList<>();
        //���ж�ȡ�ļ���ÿһ�У�����γ�һ��List
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);

            String currentLine;
            System.out.println("��ǰ�ļ��ǣ�" + f.getAbsolutePath());
            while ((currentLine = br.readLine()) != null) {
                strList.add(currentLine);

                System.out.println("��ǰ�У�" + currentLine);
            }
            System.out.println("�ļ���ȡ��ϣ�" + f.getAbsolutePath());
            return strList;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return strList;
    }

    /**
     * ������������ĳ˻�
     *
     * @param list1
     * @param list2
     * @return
     */
    static List<String> calculateListProduct(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        //�ȴ����������
        if (list1 == null || list1.size() == 0) {
            return list2;
        } else if (list2 == null || list2.size() == 0) {
            return list1;
        }

        //Ȼ����������������
        //��������Ϊ�յ����
        StringBuilder sb;
        for (int i = 0; i < list1.size(); i++) {
            for (int j = 0; j < list2.size(); j++) {
                sb = new StringBuilder(list1.get(i));//
                sb.append("_");  //���ά��֮��������ַ�������ʽ��ʾ���������ⶨ�壬�������ﶨ���ˣ������ʱ���Ҫ���ն�Ӧ�Ĺ���ȥ����
                sb.append(list2.get(j));
                result.add(sb.toString());
            }
        }

        return result;
    }
}