package com.zhou.channel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FlavorV2Util {

    public static void v2SignInjectFlavor(File fileIn, String flavorName, File fileOut) {
        try {
            RandomAccessFile apkFile = new RandomAccessFile(fileIn, "r");
            ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);//��һ�����ҵ�EOCD������apk��ƫ��λ��
            if (null == eocdBuffer) {
                eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                        .EOCD_COMMENT_MAX_LEN);
            }
            if (null == eocdBuffer) {
                apkFile.close();
                throw new Exception(fileIn.getPath() + " ����һ����׼��apk�ļ�");
            }

            int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);//��16�ֽڿ�ʼ����4���ֽ�Ϊint, ���ֵ����CD - CentralDirectory �Ŀ�ʼindex

            V2SignBlock v2SignBlock = findV2SignBlock(apkFile, cdOffset);//�ڶ����������Ƿ����v2ǩ����,���Ǳ����cd�Ŀ�ʼλ��ȥ���ƣ�����
            print(v2SignBlock);//��ӡ���Կ������ж��ٸ�idv-value

//            ��Ȼ�أ������Ѿ�ȷ�����������V2ǩ��
            if (v2SignBlock != null) {//V2ǩ���鲻Ϊ�գ���˵����V2ǩ����apk
                //��ô�����Ǿ���V2ǩ�������ע��������Ϣ
                generateV2Channel(fileIn, eocdBuffer, flavorName, v2SignBlock, fileOut);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void print(V2SignBlock v2SignBlock) {
        Map<Integer, ByteBuffer> map = v2SignBlock.getPair();
        //����������Ҫ��һ��
        Iterator<Map.Entry<Integer, ByteBuffer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ByteBuffer> next = iterator.next();
            Integer key = next.getKey();
            ByteBuffer value = next.getValue();
            value.flip();
            byte[] b = new byte[value.remaining()];
            value.get(b);
            String decodeValue = new String(b);
            System.out.println("id-value��ֵ��    " + key + ":" + decodeValue);
        }
    }

    /**
     * ��һ����
     * ���ļ��ж�ȡ��EndOfCentralDirectory���ֽ�����
     *
     * @param apkFile
     * @param offset
     * @return
     * @throws Exception
     */
    static ByteBuffer findEocd(RandomAccessFile apkFile, int offset) throws Exception {
        apkFile.seek(apkFile.length() - offset); //��������apk�ļ������ȴ�ĩβ�����˵� EndOfCentralDirectory�Ŀ�ʼλ��
        //��ȡoffset��������
        ByteBuffer eocd_buffer = ByteBuffer.allocate(offset);  // �����offsetֵ��22����һ����˼�ǣ�����22���ֽڶ�ȡ����
        eocd_buffer.order(ByteOrder.LITTLE_ENDIAN);//MMP ??��ûע�Ϳ�����
        apkFile.read(eocd_buffer.array());// ???������
        //ѭ������eocd����

        System.out.println("������һ��ѭ����������ʲô...");
        //Ȼ��������һ��forѭ��, ���ﻹ���ֻ��һ��ѭ��,��Ȼ��֪��
        for (int current_offset = 0; current_offset + Constants.EOCD_COMMENT_OFFSET <= offset; current_offset++) {
            System.out.println("�������飬�������ֻ��һ��ѭ��");
            int eocd_tag = eocd_buffer.getInt(current_offset);//һ��int��4���ֽ� ������EOCD�ĸ�ʽͼ��ȡһ��int4�ֽڣ���ʾ������Ŀ¼�Ľ�����־
            if (eocd_tag == Constants.EOCD_TAG) {//����Ƿ�����Ǻ���Ŀ¼������־
                int comment_index = current_offset + Constants
                        .EOCD_COMMENT_LEN_OFFSET; //�ҵ�comment��index
                short comment_len = eocd_buffer.getShort(comment_index);// �ҵ�ע�ͳ��ȵ�ֵ
                System.out.println("comment_len:" + comment_len);
                System.out.println("offset:" + offset);
                System.out.println("comment_index:" + comment_index);
                if (comment_len == offset - comment_index - 2) {//
                    byte[] array = eocd_buffer.array();
                    ByteBuffer eocd = ByteBuffer.allocate(offset - current_offset);
                    eocd.order(ByteOrder.LITTLE_ENDIAN);
                    System.arraycopy(array, current_offset, eocd.array(), 0, eocd.capacity());
                    return eocd;
                }
            }
        }
        return null;
    }

    /**
     * @param apkFile
     * @param offset
     * @return
     * @throws IOException
     */
    static V2SignBlock findV2SignBlock(RandomAccessFile apkFile, int offset) throws IOException {
        System.out.println("====findV2SignBlock start======");
        //�ڶ���v2ǩ�����е�block of size ��ƫ��
        int block_2size_offset = offset - 24;
        apkFile.seek(block_2size_offset);
        ByteBuffer v2BlockMagicBuffer = ByteBuffer.allocate(24);
        v2BlockMagicBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //��ȡv2ǩ�����еڶ���block size ��magic
        apkFile.read(v2BlockMagicBuffer.array());
        //��ȡblocksize
        long block2_size = v2BlockMagicBuffer.getLong();// ����sizeOfBlock
        //��ȡmagic
        byte[] block_magic = new byte[16];
        v2BlockMagicBuffer.get(block_magic);
        //���magic����v2ǩ����magic
        System.out.println("====111111111======");
        if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, block_magic)) {//�Ա�magic���ֵ�Ƿ���ͬ�������ͬ����ô����ȷ������v2ǩ����
            System.out.println("====ȷ������V2ǩ����apk��=======");
            //����block size ��ȡ���е�v2 block����

            //��ô��һ�����ҵ� 2 ApkSigningBlock����ʼλ��
            apkFile.seek(offset - block2_size - 8);// ���ֵ����������������� 2ApkSigningBlock����ʼλ��
            ByteBuffer v2BlockBuffer = ByteBuffer.allocate((int) (block2_size + 8));//�Ȼ��ֿռ䣬�����Ѿ�+8��Ҳ����������
            v2BlockBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(v2BlockBuffer.array());// �ٰ�����2ApkSigningBlock��byte���ݶ�д�뵽������
            // ����id-value
            Map pair = new LinkedHashMap<Integer, ByteBuffer>();
            // �����һ��blocksize���ڵڶ���
            if (v2BlockBuffer.getLong() == block2_size) {//��һ��sizeOfBlock��ֵ��ڶ���sizeOfBlock��ֵ�����ͬ�� ע�⣬����getLongһ�£�position�Ϳ�ʼ�ƶ���,ʵ���Ͼ�+8��
                //ѭ����ȡ id-value
                while (v2BlockBuffer.position() < v2BlockBuffer.capacity() - 8 - 16) {// ����ų������8�ֽڵڶ���sizeOfBlock��magic
                    //��ȡid value���ݵ��ܳ���
                    long id_value_size = v2BlockBuffer.getLong();//�õ���һ��id-value��ֵ�Խṹ���ܳ���
                    //��ȡid
                    int id = v2BlockBuffer.getInt();// �õ�id
                    //��ȡvalue
                    ByteBuffer value = ByteBuffer.allocate((int) (id_value_size - 4));//Ϊʲô��ȥ��value��ʱ�����ǻ���ȥ��ByteBuffer.allocate����ռ���
                    value.order(ByteOrder.LITTLE_ENDIAN);
                    v2BlockBuffer.get(value.array());
                    pair.put(id, value);//�����Ͱ�id-value��ֵ���õ���
                }
                if (pair.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//�������idΪAPK_SIGNATURE_SCHEME_V2_BLOCK_ID V2ǩ�����Ĭ��ֵ��һ��id
                    v2BlockBuffer.flip(); // �ѻ�����position��Ϊͷ��
                    System.out.println("====return new V2SignBlock(pair, v2BlockBuffer);=======");
                    return new V2SignBlock(pair, v2BlockBuffer);
                }
            } else {
                //Ҳ����˵�������� �����ط���sizeOfBlock����ͬ�����

            }
        }
        System.out.println("====This is not a V2 signed Apk====");

        return null;
    }

    static void generateV2Channel(File fileIn, ByteBuffer eocdBuffer, String channel, V2SignBlock v2SignBlock, File fileOut) throws Exception {
        FileOutputStream fos = new FileOutputStream(fileOut);
        /**
         * 1��д���һ��������
         */
        int v2SigningBlockSize = v2SignBlock.getData().capacity();//V2ǩ����Ĵ�С
        int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);//ȡ��centralDirectory������ʼλ��
        int cdSize = eocdBuffer.getInt(Constants.EOCD_CD_SIZE_OFFSET);//ȡ��centralDirectory����size
        int coze_len = cdOffset - v2SigningBlockSize;//��� 1 ContentsOfZipEntries �ĳ��� ���õ�3���ֵ���ʼindex ��ȥ �ڶ����ֵĳ��ȣ��͵õ��˵�һ���ֳ��ȣ�
        //�������ֵ���ʼindex����ʵҲ���ǵڶ����ֵĽ���λ��
        RandomAccessFile apkInFile = new RandomAccessFile(fileIn, "r");
        byte[] cozeBytes = new byte[coze_len];//������������
        apkInFile.read(cozeBytes);// �����ݶ�������������
        //д�뵽����ļ�
        fos.write(cozeBytes);//�ѻ�������д�� �������

        /**
         * 2��д��ǩ����
         */
        byte[] channelBytes = channel.getBytes(Constants.CHARSET);//��utf-8�ĸ�ʽ������ȥ����Ϣ
        //��ǩ�����ܴ�С
        int block_size = v2SigningBlockSize + 8 + 4 + channelBytes.length;//��Ȼ����Ҫд������Ϣ��ǩ�����У���ô��Ҫ������ӽ�ȥ֮��ǩ����Ӧ���Ƕ��
        //����Ѿ�����������Ҫ��ӵ�������Ϣ��id
        if (v2SignBlock.getPair().containsKey(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID)) {//�����������Զ��������id,��ô����Ѿ�����
            //��ȡ�Ѿ����ڵ�v2��������
            ByteBuffer v2ChannelValue = v2SignBlock.getPair().get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//�ǾͰ�ֵ�ó���
            block_size = block_size - 8 - 4 - v2ChannelValue.capacity();//��������Ҫд���µ�������Ϣ���Ǿ�Ҫ�Ѿɵĳ��ȳ�������Ҫͬʱ��������һ����id-value
        }
        ByteBuffer newV2Block = ByteBuffer.allocate(block_size);// �����������µ�ǩ�����С�Ļ�����
        newV2Block.order(ByteOrder.LITTLE_ENDIAN);
        long blockSizeFieldValue = block_size - 8;//-8 ��Ϊ�˼�ȥ��һ���ֶΣ�sizeOfBlock,����ʾ�����Լ�֮�⣬����ǩ���黹ʣ�¶���
        newV2Block.putLong(blockSizeFieldValue);//ok�����ֵ��������Ҫ��ֵ
        Set<Integer> ids = v2SignBlock.getPair().keySet();//Ȼ������������֮ǰ�ʹ��ڵ�id-value�ṹ
        for (Integer id : ids) {
            //�����Ѿ����ڵ�����id����
            if (id != Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID) {//�ɵ�������Ϣ�Ͳ�Ҫд����
                ByteBuffer value = v2SignBlock.getPair().get(id);// value
                newV2Block.putLong(4 + value.capacity()); //��һС�飬��ʾ��һ��id-value�Ĵ�С
                newV2Block.putInt(id);//����id
                newV2Block.put(value);//����value
            }
        }
        //���������Ϣ
        newV2Block.putLong(4 + channelBytes.length); //����id-valueҲһ��
        newV2Block.putInt(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//id
        newV2Block.put(channelBytes);//value ������ε�channelName

        newV2Block.putLong(blockSizeFieldValue); //����б����˷�ڣ�sizeOfBlock��magic
        newV2Block.put(Constants.APK_SIGNING_BLOCK_MAGIC);//��magic

        fos.write(newV2Block.array());//�ѻ�����������д�뵽�����

        /**
         * 3��д��cd 3centralDirectory����Ŀ¼��
         */
        apkInFile.seek(cdOffset);//����seek��cd������ʼλ��
        byte[] cdBytes = new byte[cdSize];//������������
        apkInFile.read(cdBytes);//��ԭ�ļ���һ������ݶ�ȡ��cdByte����
        fos.write(cdBytes);//���������д�뵽outFile�ļ���

        /**
         * 4���޸�eocd�е����� ,ΪʲôҪ�޸����أ� ��Ϊ�������ڵڶ������ڼ���������Ϣ�������˵�������Ŀ�ʼλ�÷����˱仯�����Ǳ�������ֱ仯д�뵽 eocd��16�����4���ֽ���
         */
        byte[] bytes = new byte[16];
        eocdBuffer.get(bytes);//������16���ֽ�,�����ݶ���bytes��
        eocdBuffer.getInt();//����4���ֽ�
        byte[] comment = new byte[eocdBuffer.capacity() - eocdBuffer.position()];//comment��comment-length���ݵ��ܳ��ȣ���byte����װ����
        eocdBuffer.get(comment);//

        ByteBuffer newEocd = ByteBuffer.allocate(eocdBuffer.capacity());//����һ���µ�eocd������Ȼ����СҪһģһ��
        newEocd.order(ByteOrder.LITTLE_ENDIAN);
        newEocd.put(bytes);//ԭ�����ڵ�16�ֽڣ�ԭ���Ž�ȥ
        newEocd.putInt(newV2Block.capacity() + coze_len);//��1,2���ֵĳ��ȼ����������ǵ������ֵ���ʼƫ����
        newEocd.put(comment);//������ע��Ҳ������

        fos.write(newEocd.array());//ͬ����д�뵽�������
        System.out.println("v2ǩ���ļ�������ע�����");
        eocdBuffer.flip();
        apkInFile.close();
        fos.flush();
        fos.close();
    }

    public static String getV2Channel(String sourceDir) {
        File f = new File(sourceDir);
        try {
            RandomAccessFile apkFile = new RandomAccessFile(f, "r");
            ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);//��һ�����ҵ�EOCD������apk��ƫ��λ��
            if (null == eocdBuffer) {
                eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                        .EOCD_COMMENT_MAX_LEN);
            }
            if (null == eocdBuffer) {
                apkFile.close();
                throw new Exception(sourceDir + " ����һ����׼��apk�ļ�");
            }
            //�ҵ�ǩ����
            int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);
            V2SignBlock v2SignBlock = findV2SignBlock(apkFile, cdOffset);
            if (null == v2SignBlock)
                throw new RuntimeException("v2SignBlock �ǿ�");
            //�ҵ�id-value��
            Map<Integer, ByteBuffer> map = v2SignBlock.getPair();
            if (map != null) {
                ByteBuffer byteBuffer = map.get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//��id-value�����ҵ�������Ϣ
                byte[] array = byteBuffer.array();
                String str = new String(array, Constants.CHARSET); //��������Ϣ����ַ��������س�ȥ
                return str;
            } else {
                throw new RuntimeException("getPair�ǿ�");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
