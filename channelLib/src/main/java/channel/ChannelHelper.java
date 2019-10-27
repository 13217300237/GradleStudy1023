package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import channel.data.Apk;
import channel.data.Constants;

/**
 * Created by Lance on 2017/5/21.
 */

public class ChannelHelper {

    private static String channel = null;

    public static void reset() {
        channel = null;
    }

    public static String getChannel(String sourceDir) {
        if (channel != null) {
            System.out.println("已有渠道信息，不再读取 直接返回" + channel);
            return channel;
        }
        try {
            Apk apk = ApkParser.parser(sourceDir);
            if (apk.isV2()) {
                System.out.println("这是V2签名的apk，现在用V2的方式来读取渠道信息");
                return v2Channel(apk);
            } else if (apk.isV1()) {
                System.out.println("这是V1签名的apk，现在用V1的方式来读取渠道信息");
                return v1Channel(apk);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String v1Channel(Apk apk) throws UnsupportedEncodingException {
        ByteBuffer data = apk.getEocd().getData();
        short commentlen = data.getShort(Constants.EOCD_COMMENT_LEN_OFFSET);
        if (commentlen == 0) {
            return null;
        }
        byte[] commentBytes = new byte[commentlen];
        data.position(Constants.EOCD_COMMENT_OFFSET);
        data.get(commentBytes);
        channel = new String(commentBytes, Constants.CHARSET);
        return channel;
    }

    private static String v2Channel(Apk apk) throws UnsupportedEncodingException {
        ByteBuffer byteBuffer = apk.getApkSigningBlock().getPair().get(Constants
                .APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
        channel = new String(byteBuffer.array(), Constants.CHARSET);
        return channel;

    }


    public static String getV1Channel(String sourceDir) {
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
                if (entryName.startsWith("META-INF/channel")) {
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

}
