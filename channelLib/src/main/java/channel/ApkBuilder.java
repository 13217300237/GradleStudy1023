package channel;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import channel.data.Apk;
import channel.data.Constants;
import channel.data.bean.ApkSigningBlock;

/**
 * ����Apk��Ķ���������һ��Apk�ļ�
 */
public class ApkBuilder {

    public static void generateChannel(String channel, Apk apk, File out) throws
            Exception {
        if (apk.isV2()) {
            System.out.println("����V2ǩ����apk��������V2�ķ�ʽ��ע��������Ϣ");
            generateV2Channel(channel, apk, out);
        } else if (apk.isV1()) {
            System.out.println("����V1ǩ����apk��������V1�ķ�ʽ��ע��������Ϣ");
            generateV1Channel(channel, apk, out);
        }
    }

    static void generateV1Channel(String channel, Apk apk, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);

        //��ȡ��һ��������
        int cdOffset = apk.getEocd().getCdOffset();
        int cdSize = apk.getEocd().getCdSize();
        RandomAccessFile apkFile = new RandomAccessFile(apk.getFile(), "r");
        byte[] cozeBytes = new byte[cdOffset];
        apkFile.read(cozeBytes);
        //д�뵽����ļ�
        fos.write(cozeBytes);


        //д��cd
        apkFile.seek(cdOffset);
        byte[] cdBytes = new byte[cdSize];
        apkFile.read(cdBytes);
        fos.write(cdBytes);


        //д��eocd
        ByteBuffer data = apk.getEocd().getData();
        byte[] bytes = new byte[Constants.EOCD_COMMENT_LEN_OFFSET];
        data.get(bytes);
        data.flip();
        fos.write(bytes);
        byte[] channelBytes = channel.getBytes(Constants.CHARSET);
        ByteBuffer commentlen = ByteBuffer.allocate(2);
        commentlen.order(ByteOrder.LITTLE_ENDIAN);
        commentlen.putShort((short) channelBytes.length);
        fos.write(commentlen.array());

        fos.write(channelBytes);


        apkFile.close();
        fos.flush();
        fos.close();
    }

    static void generateV2Channel(String channel, Apk apk, File out) throws Exception {
        FileOutputStream fos = new FileOutputStream(out);
        /**
         * 1��д���һ��������
         */
        int v2Size = apk.getApkSigningBlock().getData().capacity();
        int cdOffset = apk.getEocd().getCdOffset();

        int coze_len = cdOffset - v2Size;
        RandomAccessFile apkFile = new RandomAccessFile(apk.getFile(), "r");
        byte[] cozeBytes = new byte[coze_len];
        apkFile.read(cozeBytes);
        //д�뵽����ļ�
        fos.write(cozeBytes);

        /**
         * 2��д��ǩ����
         */
        ApkSigningBlock v2SignBlock = apk.getApkSigningBlock();
        ByteBuffer v2Block = v2SignBlock.getData();
        int capacity = v2Block.capacity();
        byte[] channelBytes = channel.getBytes(Constants.CHARSET);
        int block_size = capacity + 8 + 4 + channelBytes.length; //��ǩ�����ܴ�С
        if (v2SignBlock.getPair().containsKey(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID)) {//����Ѿ�����������Ҫ��ӵ�������Ϣ��id
            ByteBuffer v2ChannelValue = v2SignBlock.getPair().get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//��ȡ�Ѿ����ڵ�v2��������
            block_size = block_size - 8 - 4 - v2ChannelValue.capacity();
        }
        ByteBuffer newV2Block = ByteBuffer.allocate(block_size);
        newV2Block.order(ByteOrder.LITTLE_ENDIAN);
        long blockSizeFieldValue = block_size - 8;
        newV2Block.putLong(blockSizeFieldValue);
        Set<Integer> ids = v2SignBlock.getPair().keySet();
        for (Integer id : ids) {
            if (id != Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID) { //�����Ѿ����ڵ�����id����
                ByteBuffer value = v2SignBlock.getPair().get(id);
                newV2Block.putLong(4 + value.capacity());
                newV2Block.putInt(id);
                newV2Block.put(value);
            }
        }
        //���������Ϣ
        newV2Block.putLong(4 + channelBytes.length);
        newV2Block.putInt(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
        newV2Block.put(channelBytes);
        newV2Block.putLong(blockSizeFieldValue);
        newV2Block.put(Constants.APK_SIGNING_BLOCK_MAGIC);
        fos.write(newV2Block.array());
        /**
         * 3��д��cd
         */
        int cdSize = apk.getEocd().getCdSize();
        apkFile.seek(cdOffset);
        byte[] cdBytes = new byte[cdSize];
        apkFile.read(cdBytes);
        fos.write(cdBytes);

        /**
         * 4���޸�eocd�е�����
         */
        byte[] bytes = new byte[16];
        ByteBuffer data = apk.getEocd().getData();
        data.get(bytes);
        data.getInt();
        byte[] comment = new byte[data.capacity() - data.position()];
        data.get(comment);
        ByteBuffer newEocd = ByteBuffer.allocate(data.capacity());
        newEocd.order(ByteOrder.LITTLE_ENDIAN);
        newEocd.put(bytes);
        newEocd.putInt(newV2Block.capacity() + coze_len);
        newEocd.put(comment);

        fos.write(newEocd.array());

        data.flip();
        apkFile.close();
        fos.flush();
        fos.close();
    }

}
