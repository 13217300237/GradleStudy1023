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
 * apk解析器,将一个apk文件解读成为我们自定义的apk类的对象
 */
public class ApkParser {
    /**
     * 把一个apk文件，解析成Apk对象
     *
     * @param f
     */
    public static Apk parse(File f) {
        Apk apk = new Apk(f);
        RandomAccessFile apkFile = null;
        try {
            apkFile = new RandomAccessFile(apk.apkFile, "r");
            //先找到屁股4 EndOfCentralDirectory
            EndOfCentralDirectory eocd = findEndOfCentralDirectory(apkFile);
            // 然后从屁股中，找到3 CentralDirectory的偏移位置以及大小
            CentralDirectory cd = findCentralDirectory(apkFile, eocd); //然后确定了cd区的byte数据
            // 既然找到了第三部分3 CentralDirectory,那么第三部分紧挨着的2签名块，也就找到了
            ApkSigningBlock asb = findApkSigningBlock(apkFile, cd.getOffset());
            //最后找第一部分
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
     * 找屁股的过程非常的有想法
     * <p>
     * 打开屁股图：
     * 这一部分处于整个apk文件字节的末尾处，从0到22字节，都是固定长度，但是最后的comment它是一个不确定的长度，所以我们无法简单通过减法运算找到这个区。
     * 但是也有方法。
     * 判断这个区是不是4 EndOfCentralDirectory ，只需要找到这个区的 头部4个字节，这4个字节表示，3CentralDirectory的结束标记，这是一个固定值，就是给开发者判定这个区的开始位置的
     * <p>
     * ok，我们假设comment的长度是0，那么全区长度就是22，我们seek到apkLength-22，再往后读4个字节的int，判断它和 “3CentralDirectory的结束标记固定值”是否相同，
     * 如果相同，就说明comment长度确实是0.
     * 那么如果不相同呢？就说明，长度不是0，我们通过循环的方式，依次往前推进，22不够，那就23,24,25...直到读出的4个字节是 “3CentralDirectory的结束标记固定值” 为止
     * 如果循环停止，发现了“3CentralDirectory的结束标记固定值”，我们就找到了屁股的起始位置,于是我们就能够用RandomAccessFile.seek/read 将它写到ByteBuffer中去
     *
     * @param apkFile
     * @return
     */
    private static EndOfCentralDirectory findEndOfCentralDirectory(RandomAccessFile apkFile) {
        EndOfCentralDirectory endOfCentralDirectory = new EndOfCentralDirectory();
        int commentLength = 0;
        try {
            //假设 comment长度就是0 ... 为什么假设是0呢？这是因为comment为空的情况相当多。。
            apkFile.seek(apkFile.length() - Constants.EOCD_COMMENT_OFFSET - commentLength);//seek到倒数22
            ByteBuffer eocdBuffer = ByteBuffer.allocate(Constants.EOCD_COMMENT_OFFSET + commentLength);//22不管怎么滴，先给22个字节的空间
            eocdBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(eocdBuffer.array());//apk开始写入数据到数组
            if (eocdBuffer.getInt(0) == Constants.EOCD_TAG) {//看看是不是这个标记
                endOfCentralDirectory.setByteArr(eocdBuffer.array());
                endOfCentralDirectory.setCdOffset(eocdBuffer.getInt(Constants.EOCD_CD_OFFSET));
                endOfCentralDirectory.setCdSize(eocdBuffer.getInt(Constants.EOCD_CD_SIZE_OFFSET));
                return endOfCentralDirectory;
            } else {
                //但是如果我们发现假设不成立，那么？我们就再假设comment长度是 short的最大值0xffff.
                int commentLengthMax = 0xffff;
                //循环
                for (commentLength = 0; commentLength <= commentLengthMax; commentLength++) {
                    apkFile.seek(apkFile.length() - Constants.EOCD_CD_OFFSET - commentLength);
                    eocdBuffer = ByteBuffer.allocate(Constants.EOCD_CD_OFFSET + commentLength);
                    eocdBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    apkFile.read(eocdBuffer.array());//apk开始写入数据到数组
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
     * 找中央目录区 CentralDirectory
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
     * 找apk签名区
     *
     * @param apkFile
     * @param cdOffset centralDirectory开始位置
     * @return
     */
    private static ApkSigningBlock findApkSigningBlock(RandomAccessFile apkFile, int cdOffset) {
        ApkSigningBlock apkSigningBlock = new ApkSigningBlock();
        try {
            apkFile.seek(cdOffset - 8 - 16);//这里的8是签名块的sizeOfBlock8字节，16是magic的16字节
            //我必须把这个24个字节读出来，然后才能获得sizeOfBlock的值，也就获得了签名块的size
            ByteBuffer byteBuffer = ByteBuffer.allocate(8 + 16);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(byteBuffer.array());//那我就读出来了
            long blockSize = byteBuffer.getLong();//先读一个8字节的long
            //再读一个16字节的magic
            byte[] magic = new byte[16];
            byteBuffer.get(magic);
            //按照这个规则，去读了之后，我要判断是不是V2签名啊，如果是，那这一块magic应该和官方给的值一模一样
            if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, magic)) {
                apkSigningBlock.setOffset((int) (cdOffset - blockSize - 8));//保存签名区的开始位置
                apkFile.seek(cdOffset - blockSize - 8);//这个时候我就seek到了签名块的头部

                ByteBuffer bb = ByteBuffer.allocate((int) (blockSize + 8));//
                bb.order(ByteOrder.LITTLE_ENDIAN);
                apkFile.read(bb.array());//现在我得到 整个签名块的byteBuffer
                apkSigningBlock.setByteArr(bb.array());

                Map<Integer, byte[]> idValueMap = new LinkedHashMap<>();
                //但是我仍然需要从签名块中读出一些重要数据
                long xxx = bb.getLong();
                System.out.println("对比两个sizeOfBlock的值:" + xxx + "- " + blockSize);
                if (xxx == blockSize) {//这里做一个预防，如果两者相同，才说明是正常的
                    //我需要读出
                    //循环，从当前location开始，每一次往后读8+4+N，最后的8+16不要读
                    while (bb.position() < bb.capacity() - 8 - 16) {
                        long idValueLength = bb.getLong();
                        int id = bb.getInt();
                        byte[] valueByte = new byte[(int) idValueLength - 4];
                        bb.get(valueByte);
                        //键值对，后面有用，先存起来
                        idValueMap.put(id, valueByte);
                    }
                    if (idValueMap.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//存完之后呢？检查格式
                        apkSigningBlock.setIdValueMap(idValueMap);
                    } else {
                        throw new RuntimeException("发生异常，签名块的idValueMap 中没有找到id为0x7109871a的键值对...");
                    }
                } else {
                    throw new RuntimeException("发生异常，签名块的2个sizeOfBlock值不同...");
                }
                return apkSigningBlock;
            } else {
                throw new RuntimeException("这个不是v2签名的apk");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ContentsOfZipEntries findContentsOfZipEntries(RandomAccessFile apkFile, ApkSigningBlock apkSigningBlock) {
        ContentsOfZipEntries coze = new ContentsOfZipEntries();
        int cdOffset = apkSigningBlock.getOffset();//签名区的开始位置
        //我需要计算出zoze区的size
        //如果cdOffset是10，那么，coze区的范围就是0-9，我直接划分10个字节就行了
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

