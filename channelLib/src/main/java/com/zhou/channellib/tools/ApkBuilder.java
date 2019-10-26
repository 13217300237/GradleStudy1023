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
 * ����apk(��Ҫ����V2ǩ����apk����)
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
     * ����������
     *
     * @param channelName ����Ҫע��������Ϣ
     * @param apk         ֮ǰ����apk�ļ��õ���Apk����
     * @param outFile     ����ļ��ĵ�ַ
     */
    public static void generate(String channelName, Apk apk, File outFile) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(outFile);
        try {
            //��һ���֣�������Ŀ��coze
            fos.write(apk.coze.byteArr);

            System.out.println("�Ѿ�д���һ����,coze.byteArr.length=" + apk.coze.byteArr.length);

            //��ʼд�ڶ�����,ǩ����
            ApkSigningBlock asb = apk.asb;
            ByteBuffer asbByteBuffer = ByteBuffer.wrap(asb.byteArr);
            asbByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            System.out.println("ԭ2 ApkSigningBlockת����ByteBuffer��sizeΪ:" + asbByteBuffer.capacity());
            long sizeOfBlock = asbByteBuffer.getLong();//�õ� sizeOfBlock(����ֵ�ǳ�ȥ����֮�⣬������ĳ���)
            byte[] channelNameByte = channelName.getBytes(Constants.CHARSET);//�Ȱ�������Ϣ���byte����
            if (asb.idValueMap.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID)) {//���ԭ�ȵ�id-value��������û�����ǵ�������Ϣ
                //����У���ô����Ҫ�ҵ�����Ȼ���õ����ĳ��ȣ��������Ҫ����
                byte[] bytes = asb.idValueMap.get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
                sizeOfBlock = sizeOfBlock - 8 - 4 - bytes.length;//Ҫ����!!
            }
            //Ȼ���ǵڶ�����,ǩ����,����������ڶ�����ĳЩ���ݣ�����3������
            int newChannelIdValueBlockLength = channelNameByte.length + 8 + 4;//��һ��sizeOfBlock����Ϊ����ע����������Ϣ��������������䳤���������ֵ������
            int newSizeOfBlock = (int) (newChannelIdValueBlockLength + sizeOfBlock + 8);
            ByteBuffer newByteBuffer = ByteBuffer.allocate(newSizeOfBlock);//�½�һ��������
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
            //�ڶ�����Ȼ���� id-value�飬������һ���µ�id-value��
            //����ע��������Ϣ
            newByteBuffer.putLong(4 + channelNameByte.length);
            newByteBuffer.putInt(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
            newByteBuffer.put(channelNameByte);
            //��������ĩβ�Ǹ�sizeOfBlock��ֵ�͵�һ��sizeOfBlockһ��
            //��Ҫ���Ƿ��
            newByteBuffer.putLong(newSizeOfBlock - 8);
            newByteBuffer.put(Constants.APK_SIGNING_BLOCK_MAGIC);
            fos.write(newByteBuffer.array());

            //��������,����Ŀ¼��
            fos.write(apk.cd.byteArr);

            //���Ĳ��� ����Ŀ¼��β��
            ByteBuffer data = ByteBuffer.wrap(apk.eocd.byteArr);
            data.order(ByteOrder.LITTLE_ENDIAN);
            if (data.getInt(0) == Constants.EOCD_TAG) {
                System.out.println("�ҵ�����Ŀ¼������־");
            }
            byte[] bytes16 = new byte[16];
            data.get(bytes16);
            data.getInt(); //�ƶ�����20  //java.nio.BufferUnderflowException ��ȡ�ĳ��ȳ���������ĳ���
            //ע����һ���ҵ�Ū������
            byte[] comment = new byte[data.capacity() - data.position()];
            data.get(comment);

            ByteBuffer newEocd = ByteBuffer.allocate(data.capacity());//�¾�EOCD������С��һ����
            newEocd.order(ByteOrder.LITTLE_ENDIAN);
            newEocd.put(bytes16);
            newEocd.putInt(newSizeOfBlock + apk.coze.byteArr.length); //��Ҫ������Ǻ���Ŀ¼�Ŀ�ʼλ��
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
