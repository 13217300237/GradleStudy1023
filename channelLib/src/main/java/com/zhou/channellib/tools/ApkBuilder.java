package com.zhou.channellib.tools;

import com.zhou.channellib.tools.bean.ApkSigningBlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

/**
 * 构建apk(主要用于V2签名的apk重造)
 */
public class ApkBuilder {

    public static Apk check(Apk apk, File outFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            fos.write(apk.coze.byteArr);
            fos.write(apk.asb.byteArr);
            fos.write(apk.cd.byteArr);
            fos.write(apk.eocd.byteArr);
            fos.flush();

            Apk parse = ApkParser.parse(outFile);
            return parse;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;

    }

    /**
     * 生成渠道包
     *
     * @param channelName 我们要注入渠道信息
     * @param apk         之前解析apk文件得到的Apk对象
     * @param outFile     输出文件的地址
     */
    public static void generate(String channelName, Apk apk, File outFile) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(outFile);
        try {
            //第一部分，内容条目区coze
            fos.write(apk.coze.byteArr);

            System.out.println("已经写入第一部分,coze.byteArr.length=" + apk.coze.byteArr.length);

            //开始写第二部分,签名块
            ApkSigningBlock asb = apk.asb;
            ByteBuffer asbByteBuffer = ByteBuffer.wrap(asb.byteArr);
            asbByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            System.out.println("原2 ApkSigningBlock转化成ByteBuffer的size为:" + asbByteBuffer.capacity());
            long sizeOfBlock = asbByteBuffer.getLong();//得到 sizeOfBlock(它的值是除去自身之外，本区块的长度)
            byte[] channelNameByte = channelName.getBytes(Constants.CHARSET);//先把渠道信息变成byte数组
            if (asb.idValueMap.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID)) {//检查原先的id-value对里面有没有我们的渠道信息
                //如果有，那么我们要找到它，然后拿到它的长度，这个长度要作废
                byte[] bytes = asb.idValueMap.get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
                sizeOfBlock = sizeOfBlock - 8 - 4 - bytes.length;//要减掉!!
            }
            //然后是第二部分,签名区,这里必须变更第二区的某些数据，包含3个部分
            int newChannelIdValueBlockLength = channelNameByte.length + 8 + 4;//第一，sizeOfBlock，因为我们注入了渠道信息，导致整个区块变长，所以这个值必须变大
            int newSizeOfBlock = (int) (newChannelIdValueBlockLength + sizeOfBlock + 8);
            ByteBuffer newByteBuffer = ByteBuffer.allocate(newSizeOfBlock);//新建一个缓存区
            newByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            newByteBuffer.putLong(newSizeOfBlock - 8);
            Set<Integer> integers = asb.idValueMap.keySet();
            for (Integer id : integers) {
                if (id != Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID) {
                    byte[] valueBytes = asb.idValueMap.get(id);
                    newByteBuffer.putLong(4 + valueBytes.length);
                    newByteBuffer.putInt(id);
                    newByteBuffer.put(valueBytes);
                }
            }
            //第二，自然就是 id-value块，会增加一个新的id-value块
            //现在注入渠道信息
            newByteBuffer.putLong(4 + channelNameByte.length);
            newByteBuffer.putInt(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
            newByteBuffer.put(channelNameByte);
            //第三，是末尾那个sizeOfBlock，值和第一个sizeOfBlock一样
            //不要忘记封口
            newByteBuffer.putLong(newSizeOfBlock - 8);
            newByteBuffer.put(Constants.APK_SIGNING_BLOCK_MAGIC);
            fos.write(newByteBuffer.array());

            //第三部分,核心目录区
            fos.write(apk.cd.byteArr);

            //第四部分 核心目录结尾区
            ByteBuffer data = ByteBuffer.wrap(apk.eocd.byteArr);
            data.order(ByteOrder.LITTLE_ENDIAN);
            if (data.getInt(0) == Constants.EOCD_TAG) {
                System.out.println("找到核心目录结束标志");
            }
            byte[] bytes16 = new byte[16];
            data.get(bytes16);
            data.getInt(); //移动到了20  //java.nio.BufferUnderflowException 读取的长度超过了允许的长度
            //注释这一块我得弄成数组
            byte[] comment = new byte[data.capacity() - data.position()];
            data.get(comment);

            ByteBuffer newEocd = ByteBuffer.allocate(data.capacity());//新旧EOCD区，大小是一样的
            newEocd.order(ByteOrder.LITTLE_ENDIAN);
            newEocd.put(bytes16);
            newEocd.putInt(newSizeOfBlock + apk.coze.byteArr.length); //我要计算的是核心目录的开始位置
            newEocd.put(comment);
            fos.write(newEocd.array());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
