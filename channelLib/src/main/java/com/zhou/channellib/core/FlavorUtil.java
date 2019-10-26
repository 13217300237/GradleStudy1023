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
 * 风味读取工具类
 */
public class FlavorUtil {
    static final String flavorDirName = "flavor";

    /**
     * 获得V1签名的风味
     *
     * @param sourceDir apk文件的path
     * @return
     */
    public static String getV1Flavor(String sourceDir) {
        //当前app的apk文件
        ZipFile zipfile = null;
        StringBuilder channel = new StringBuilder();
        try {
            // 遍历APK中所有文件
            zipfile = new ZipFile(sourceDir);
            Enumeration<? extends ZipEntry> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // 读取 META-INF/channel 中的信息（渠道信息）
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
     * 这个是V1签名的测试代码
     * <p>
     * 1. 从基准apk复制一份 flavor apk，内容完全一样
     * 2. 在刚刚复制出的flavor apk中，写入渠道信息
     *
     * @throws IOException
     */
    public static void makePkg(String basePath, String flavorName, String outApkName) throws IOException {
        File baseApk = new File(basePath);
        //加入渠道信息后输出
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(outApkName));

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
        ZipEntry zipEntry = new ZipEntry("META-INF/" + FlavorUtil.flavorDirName);
        jos.putNextEntry(zipEntry);
        jos.write(flavorName.getBytes());//把风味信息写入到 META-INF的新建的文件中
        jos.closeEntry();
        jos.close();
        System.out.println("输出路径:" + outApkName);
    }

    static List<String> getStrListFromFile(File f) {
        List<String> strList = new ArrayList<>();
        //逐行读取文件的每一行，最后形成一个List
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);

            String currentLine;
            System.out.println("当前文件是：" + f.getAbsolutePath());
            while ((currentLine = br.readLine()) != null) {
                strList.add(currentLine);

                System.out.println("当前行：" + currentLine);
            }
            System.out.println("文件读取完毕：" + f.getAbsolutePath());
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
     * 计算两个数组的乘积
     *
     * @param list1
     * @param list2
     * @return
     */
    static List<String> calculateListProduct(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        //先处理特殊情况
        if (list1 == null || list1.size() == 0) {
            return list2;
        } else if (list2 == null || list2.size() == 0) {
            return list1;
        }

        //然后来进行正经计算
        //两个都不为空的情况
        StringBuilder sb;
        for (int i = 0; i < list1.size(); i++) {
            for (int j = 0; j < list2.size(); j++) {
                sb = new StringBuilder(list1.get(i));//
                sb.append("_");  //多个维度之间如何用字符串的形式表示，可以随意定义，但是这里定义了，解读的时候就要按照对应的规则去解析
                sb.append(list2.get(j));
                result.add(sb.toString());
            }
        }

        return result;
    }
}