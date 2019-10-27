package channel;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import channel.data.Apk;
import channel.data.Constants;
import channel.data.bean.ApkSigningBlock;
import channel.data.bean.EndOfCentralDirectory;

/**
 * Created by Lance on 2017/5/20.
 */

public class ApkParser {

    public static Apk parser(String path) throws Exception {
        return parser(new File(path));
    }

    /**
     * ��һ��apk�ļ������������Զ����Apk��Ķ���
     *
     * 1.�õ�һ��apk��File������ʹ��RandomAccessFile������װ��Ȼ�����Ǿ��ܹ���������ļ�������λ��
     * 2.apk �ļ���4���������һ������Eocd(EndOfCentralDirectory) �������������һ��comment�����ĳ����ǲ��̶��ģ����ԣ�����eocd����һ����С������22�ֽڡ�
     * ����Ҫȷ�����������ʵ���ȣ���ֻ���ü��跨��
     * ���ȣ����Ǽ���comment�ĳ����� 0����ô�������ĳ��Ⱦ���22��Ҫ��֤���ǵļ��裬ֻ��Ҫ���������ͷ������һ��int�������Ƿ��� zip�ļ���ʽ�����`����Ŀ¼������־`��ֵ��ͬ�������ͬ���Ǿͼ��������
     * ���ǲ�������ͬ����ôֻ���ټ��� comment�ĳ����� short�����ֵ0xffff(��Ϊ��ʾcomment-length���ֶ���short���ͣ�short��2�ֽڣ�16λ����һ��16���Ƶ�λ��f��ֻ�ܱ�ʾ4λ������short�����ֵ���� 0xffff)
     * ��ô���������ĳ������ֵ����22+0xffff����û��ת��10���ƣ���Ҫ������Щϸ�ڣ�����ô��ͷ����ȥ��һ��int�������ͺ���Ŀ¼������־`��ֵ �Ƿ���ͬ�������ͬ����˵�����ҵ���������ͷ����
     * Ȼ������Ƶ�������������ʵ����.
     * 3. �ҵ���������byte����֮�󣬱���Ϊeocd
     * 4. ��eocd���ҵ� ����Ŀ¼��ƫ��λ�õ��ֶ� ������������ecod����16��Ȼ���int 4�ֽڣ��ó�һ�� ��ֵ������ʾ apk�� ����Ŀ¼����ƫ��λ��.
     * 5. ���������apk�� ����Ŀ¼����ƫ��λ�� �ҵ�ApkSigningBlock���ֽ����� ���ұ�������
     * 6. apk ��������
     * @param file
     * @return
     * @throws Exception
     */
    public static Apk parser(File file) throws Exception {
        Apk apk = new Apk(file);
        RandomAccessFile apkFile = new RandomAccessFile(file, "r");
        //����eocd����
        ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);
        //�ȼ���ע�͵ĳ�����0��������eocd���ĳ��Ⱦ���22����ʱȥͷ��Ѱ�Һ���Ŀ¼������ǣ���ʱ�պ��ҵ�����ô˵��eocd���Ĵ�Сȷʵ��0
        if (null == eocdBuffer) {//�����������ʧ�ܣ���ô�ͼ���ע�͵ĳ�����short�����ֵ��short2�ֽڣ�16λ��һ��16���Ƶ�����ʾ4λ��������Ҫ4��16���Ƶ��������ֵ����0xffff
            eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                    .EOCD_COMMENT_MAX_LEN);
        }
        if (null == eocdBuffer) {
            apkFile.close();
            throw new Exception(file.getPath() + " ����һ����׼��apk�ļ�");
        }
        apk.setEocd(new EndOfCentralDirectory(eocdBuffer));
        int cdOffset = apk.getEocd().getCdOffset();//����Ŀ¼���������apk��ƫ��λ��

        //����v2ǩ����
        apk.setApkSigningBlock(findV2SignBlock(apkFile, cdOffset));
        if (!apk.isV2()) {
            apk.setV1(isV1(file));
        }
        if (!apk.isV1() && !apk.isV2()) {
            apkFile.close();
            throw new Exception(file.getPath() + " û��ǩ��");
        }
        apkFile.close();
        return apk;
    }

    static boolean isV1(File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {
            JarEntry manifest = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            if (null == manifest) {
                return false;
            }
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith("META-INF/") && name.endsWith(".SF")) {
                    return true;
                }
            }
            return false;
        } finally {
            jarFile.close();
        }
    }


    static ApkSigningBlock findV2SignBlock(RandomAccessFile apkFile, int cdOffset) throws IOException {
        //�ڶ���v2ǩ�����е�block of size ��ƫ��
        int block_2size_offset = cdOffset - 24;//-24��Ϊ�� -8-16
        apkFile.seek(block_2size_offset); //seek
        ByteBuffer v2BlockMagicBuffer = ByteBuffer.allocate(24);//��һ��24��С������
        v2BlockMagicBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //��ȡv2ǩ�����еڶ���block size ��magic
        apkFile.read(v2BlockMagicBuffer.array());//�����������24�ֽ�
        //��ȡblocksize
        long block2_size = v2BlockMagicBuffer.getLong();//Ȼ����������ȶ�һ��long�����ֵ��ʾǩ����Ĵ�С
        //��ȡmagic
        byte[] block_magic = new byte[16];
        v2BlockMagicBuffer.get(block_magic);//�ٶ�16���ֽڵ�magic
        if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, block_magic)) {//����õ���magic�պõ���ǩ����涨��magic
            //��˵������V2ǩ��
            //����block size ��ȡ���е�v2 block����
            apkFile.seek(cdOffset - block2_size - 8);//��Ȼ����V2ǩ������ô����seek��ǩ�����ͷ��ȥ
            ByteBuffer v2BlockBuffer = ByteBuffer.allocate((int) (block2_size + 8));//Ȼ����Ҫ����һ���պ�װ��ǩ�����������ݵ�����
            v2BlockBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(v2BlockBuffer.array());//���ֽ����ݶ���������ȥ
            //����id-value
            Map pair = new LinkedHashMap<Integer, ByteBuffer>();
            //�����һ��blocksize���ڵڶ���
            if (v2BlockBuffer.getLong() == block2_size) {//��һ��long�������ƶ�location,��longֵ��֮ǰ��õ�blockSize�Աȣ�ֻ����ͬ��������ȷ���ļ���ʽ
                //�����һ��У������Ϊ��ֻҪ�ļ���ʽ���ԣ�apk���޷���װ��Ϊ�˱��ⰲװ��ʱ��ŷ������⣬��������Լ�У��
                //ѭ����ȡ id-value
                while (v2BlockBuffer.position() < v2BlockBuffer.capacity() - 8 - 16) {//-8-16 ����Ϊ�����24���ֽڲ�����id-value������
                    //��ȡid value���ݵ��ܳ���
                    long id_value_size = v2BlockBuffer.getLong();//id+value�ĳ��ȡ�����������
                    //��ȡid
                    int id = v2BlockBuffer.getInt();//id ��intռ4�ֽ�
                    //��ȡvalue
                    ByteBuffer value = ByteBuffer.allocate((int) (id_value_size - 4)); //value��ռ���ǵĲ�ֵ
                    value.order(ByteOrder.LITTLE_ENDIAN);
                    v2BlockBuffer.get(value.array());//����ȥ
                    pair.put(id, value);//��������
                }
                if (pair.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//�ٴ�У�飬��Ϊԭ��apk�ļ��У��ض�����һ���̶���id-value�������ȼ�飬Ȼ����
                    v2BlockBuffer.flip();
                    return new ApkSigningBlock(pair, v2BlockBuffer);
                }
            }
        }

        return null;
    }

    /**
     * @param apkFile RandomAccessFile��������ļ�����������������ļ����κ�λ��
     * @param offset  ƫ����
     * @return
     * @throws Exception
     */
    static ByteBuffer findEocd(RandomAccessFile apkFile, int offset) throws Exception {
        apkFile.seek(apkFile.length() - offset);
        //��ȡoffset��������
        ByteBuffer eocd_buffer = ByteBuffer.allocate(offset);//��ʱ�����eocd���ĳ��������ֵ���� ע�ͳ������ֵ0xffff + 22
        eocd_buffer.order(ByteOrder.LITTLE_ENDIAN);
        apkFile.read(eocd_buffer.array());
        //ѭ������eocd����
        for (int current_offset = 0;
             current_offset + Constants.EOCD_COMMENT_OFFSET <= offset;
             current_offset++) {//ѭ�����ң�code���Ŀ�ʼ��int
            int eocd_tag = eocd_buffer.getInt(current_offset);
            if (eocd_tag == Constants.EOCD_TAG) {//����ҵ�
                int comment_index = current_offset + Constants.EOCD_COMMENT_LEN_OFFSET;//�ҵ�ע�ͳ������Ե�index
                short comment_len = eocd_buffer.getShort(comment_index);//Ȼ���õ����ֵ,���ֵ����ע�͵ĳ���
                if (comment_len == offset - comment_index - 2) {//�ж����ݸ�ʽ�Ƿ���ȷ
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
}
