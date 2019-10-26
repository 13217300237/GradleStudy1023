package com.zhou.channellib.tools;

import com.zhou.channellib.tools.bean.ApkSigningBlock;
import com.zhou.channellib.tools.bean.CentralDirectory;
import com.zhou.channellib.tools.bean.ContentsOfZipEntries;
import com.zhou.channellib.tools.bean.EndOfCentralDirectory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * apk������,��һ��apk�ļ������Ϊ�����Զ����apk��Ķ���
 */
public class ApkParser {
    /**
     * ��һ��apk�ļ���������Apk����
     *
     * @param f
     */
    public static Apk parse(File f) {
        Apk apk = new Apk(f);
        RandomAccessFile apkFile = null;
        try {
            apkFile = new RandomAccessFile(apk.apkFile, "r");
            //���ҵ�ƨ��4 EndOfCentralDirectory
            EndOfCentralDirectory eocd = findEndOfCentralDirectory(apkFile);
            // Ȼ���ƨ���У��ҵ�3 CentralDirectory��ƫ��λ���Լ���С
            CentralDirectory cd = findCentralDirectory(apkFile, eocd); //Ȼ��ȷ����cd����byte����
            // ��Ȼ�ҵ��˵�������3 CentralDirectory,��ô�������ֽ����ŵ�2ǩ���飬Ҳ���ҵ���
            ApkSigningBlock asb = findApkSigningBlock(apkFile, cd.getOffset());
            //����ҵ�һ����
            ContentsOfZipEntries coze = findContentsOfZipEntries(apkFile, asb);

            apk.coze = coze;
            apk.asb = asb;
            apk.cd = cd;
            apk.eocd = eocd;

            return apk;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (apkFile != null) {
                try {
                    apkFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * ��ƨ�ɵĹ��̷ǳ������뷨
     * <p>
     * ��ƨ��ͼ��
     * ��һ���ִ�������apk�ļ��ֽڵ�ĩβ������0��22�ֽڣ����ǹ̶����ȣ���������comment����һ����ȷ���ĳ��ȣ����������޷���ͨ�����������ҵ��������
     * ����Ҳ�з�����
     * �ж�������ǲ���4 EndOfCentralDirectory ��ֻ��Ҫ�ҵ�������� ͷ��4���ֽڣ���4���ֽڱ�ʾ��3CentralDirectory�Ľ�����ǣ�����һ���̶�ֵ�����Ǹ��������ж�������Ŀ�ʼλ�õ�
     * <p>
     * ok�����Ǽ���comment�ĳ�����0����ôȫ�����Ⱦ���22������seek��apkLength-22���������4���ֽڵ�int���ж����� ��3CentralDirectory�Ľ�����ǹ̶�ֵ���Ƿ���ͬ��
     * �����ͬ����˵��comment����ȷʵ��0.
     * ��ô�������ͬ�أ���˵�������Ȳ���0������ͨ��ѭ���ķ�ʽ��������ǰ�ƽ���22�������Ǿ�23,24,25...ֱ��������4���ֽ��� ��3CentralDirectory�Ľ�����ǹ̶�ֵ�� Ϊֹ
     * ���ѭ��ֹͣ�������ˡ�3CentralDirectory�Ľ�����ǹ̶�ֵ�������Ǿ��ҵ���ƨ�ɵ���ʼλ��,�������Ǿ��ܹ���RandomAccessFile.seek/read ����д��ByteBuffer��ȥ
     *
     * @param apkFile
     * @return
     */
    private static EndOfCentralDirectory findEndOfCentralDirectory(RandomAccessFile apkFile) {
        EndOfCentralDirectory endOfCentralDirectory = new EndOfCentralDirectory();
        int commentLength = 0;
        try {
            //���� comment���Ⱦ���0 ... Ϊʲô������0�أ�������ΪcommentΪ�յ�����൱�ࡣ��
            apkFile.seek(apkFile.length() - Constants.EOCD_COMMENT_OFFSET - commentLength);//seek������22
            ByteBuffer eocdBuffer = ByteBuffer.allocate(Constants.EOCD_COMMENT_OFFSET + commentLength);//22������ô�Σ��ȸ�22���ֽڵĿռ�
            eocdBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(eocdBuffer.array());//apk��ʼд�����ݵ�����
            if (eocdBuffer.getInt(0) == Constants.EOCD_TAG) {//�����ǲ���������
                endOfCentralDirectory.setByteArr(eocdBuffer.array());
                endOfCentralDirectory.setCdOffset(eocdBuffer.getInt(Constants.EOCD_CD_OFFSET));
                endOfCentralDirectory.setCdSize(eocdBuffer.getInt(Constants.EOCD_CD_SIZE_OFFSET));
                return endOfCentralDirectory;
            } else {
                //����������Ƿ��ּ��費��������ô�����Ǿ��ټ���comment������ short�����ֵ0xffff.
                int commentLengthMax = 0xffff;
                //ѭ��
                for (commentLength = 0; commentLength <= commentLengthMax; commentLength++) {
                    apkFile.seek(apkFile.length() - Constants.EOCD_CD_OFFSET - commentLength);
                    eocdBuffer = ByteBuffer.allocate(Constants.EOCD_CD_OFFSET + commentLength);
                    eocdBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    apkFile.read(eocdBuffer.array());//apk��ʼд�����ݵ�����
                    if (eocdBuffer.getInt() == Constants.EOCD_TAG) {
                        endOfCentralDirectory.setByteArr(eocdBuffer.array());
                        endOfCentralDirectory.setCdOffset(eocdBuffer.getInt(Constants.EOCD_CD_OFFSET));
                        endOfCentralDirectory.setCdSize(eocdBuffer.getInt(Constants.EOCD_CD_SIZE_OFFSET));
                        return endOfCentralDirectory;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ������Ŀ¼�� CentralDirectory
     *
     * @param apkFile
     * @param endOfCentralDirectory
     * @return
     */
    private static CentralDirectory findCentralDirectory(RandomAccessFile apkFile, EndOfCentralDirectory endOfCentralDirectory) {
        CentralDirectory cd = new CentralDirectory();
        int cdOffset = endOfCentralDirectory.getCdOffset();
        int cdSize = endOfCentralDirectory.getCdSize();
        try {
            apkFile.seek(cdOffset);
            byte[] cdBytes = new byte[cdSize];
            apkFile.read(cdBytes);
            cd.setByteArr(cdBytes);
            cd.setOffset(cdOffset);
            cd.setSize(cdSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cd;
    }

    /**
     * ��apkǩ����
     *
     * @param apkFile
     * @param cdOffset centralDirectory��ʼλ��
     * @return
     */
    private static ApkSigningBlock findApkSigningBlock(RandomAccessFile apkFile, int cdOffset) {
        ApkSigningBlock apkSigningBlock = new ApkSigningBlock();
        try {
            apkFile.seek(cdOffset - 8 - 16);//�����8��ǩ�����sizeOfBlock8�ֽڣ�16��magic��16�ֽ�
            //�ұ�������24���ֽڶ�������Ȼ����ܻ��sizeOfBlock��ֵ��Ҳ�ͻ����ǩ�����size
            ByteBuffer byteBuffer = ByteBuffer.allocate(8 + 16);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(byteBuffer.array());//���ҾͶ�������
            long blockSize = byteBuffer.getLong();//�ȶ�һ��8�ֽڵ�long
            //�ٶ�һ��16�ֽڵ�magic
            byte[] magic = new byte[16];
            byteBuffer.get(magic);
            //�����������ȥ����֮����Ҫ�ж��ǲ���V2ǩ����������ǣ�����һ��magicӦ�ú͹ٷ�����ֵһģһ��
            if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, magic)) {
                apkSigningBlock.setOffset((int) (cdOffset - blockSize - 8));//����ǩ�����Ŀ�ʼλ��
                apkFile.seek(cdOffset - blockSize - 8);//���ʱ���Ҿ�seek����ǩ�����ͷ��

                ByteBuffer bb = ByteBuffer.allocate((int) (blockSize + 8));//
                bb.order(ByteOrder.LITTLE_ENDIAN);
                apkFile.read(bb.array());//�����ҵõ� ����ǩ�����byteBuffer
                apkSigningBlock.setByteArr(bb.array());

                Map<Integer, byte[]> idValueMap = new LinkedHashMap<>();
                //��������Ȼ��Ҫ��ǩ�����ж���һЩ��Ҫ����
                long xxx = bb.getLong();
                System.out.println("�Ա�����sizeOfBlock��ֵ:" + xxx + "- " + blockSize);
                if (xxx == blockSize) {//������һ��Ԥ�������������ͬ����˵����������
                    //����Ҫ����
                    //ѭ�����ӵ�ǰlocation��ʼ��ÿһ�������8+4+N������8+16��Ҫ��
                    while (bb.position() < bb.capacity() - 8 - 16) {
                        long idValueLength = bb.getLong();
                        int id = bb.getInt();
                        byte[] valueByte = new byte[(int) idValueLength - 4];
                        bb.get(valueByte);
                        //��ֵ�ԣ��������ã��ȴ�����
                        idValueMap.put(id, valueByte);
                    }
                    if (idValueMap.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//����֮���أ�����ʽ
                        apkSigningBlock.setIdValueMap(idValueMap);
                    } else {
                        throw new RuntimeException("�����쳣��ǩ�����idValueMap ��û���ҵ�idΪ0x7109871a�ļ�ֵ��...");
                    }
                } else {
                    throw new RuntimeException("�����쳣��ǩ�����2��sizeOfBlockֵ��ͬ...");
                }
                return apkSigningBlock;
            } else {
                throw new RuntimeException("�������v2ǩ����apk");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ContentsOfZipEntries findContentsOfZipEntries(RandomAccessFile apkFile, ApkSigningBlock apkSigningBlock) {
        ContentsOfZipEntries coze = new ContentsOfZipEntries();
        int cdOffset = apkSigningBlock.getOffset();//ǩ�����Ŀ�ʼλ��
        //����Ҫ�����zoze����size
        //���cdOffset��10����ô��coze���ķ�Χ����0-9����ֱ�ӻ���10���ֽھ�����
        byte[] bytes = new byte[cdOffset];
        try {
            apkFile.read(bytes);
            coze.setByteArr(bytes);
            return coze;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getV2Channel(String sourceApkPath) throws UnsupportedEncodingException {
        Apk apk = parse(new File(sourceApkPath));
        byte[] byteBuffer = apk.asb.idValueMap.get(Constants
                .APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);
        String channel = new String(byteBuffer, Constants.CHARSET);
        return channel;
    }
}

