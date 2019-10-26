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
            ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);//第一步，找到EOCD在整个apk的偏移位置
            if (null == eocdBuffer) {
                eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                        .EOCD_COMMENT_MAX_LEN);
            }
            if (null == eocdBuffer) {
                apkFile.close();
                throw new Exception(fileIn.getPath() + " 不是一个标准的apk文件");
            }

            int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);//在16字节开始，读4个字节为int, 这个值就是CD - CentralDirectory 的开始index

            V2SignBlock v2SignBlock = findV2SignBlock(apkFile, cdOffset);//第二步，查找是否存在v2签名块,我们必须从cd的开始位置去倒推，看看
            print(v2SignBlock);//打印试试看到底有多少个idv-value

//            既然呢，现在已经确定了这个就是V2签名
            if (v2SignBlock != null) {//V2签名块不为空，才说明是V2签名的apk
                //那么，我们就在V2签名块儿中注入渠道信息
                generateV2Channel(fileIn, eocdBuffer, flavorName, v2SignBlock, fileOut);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void print(V2SignBlock v2SignBlock) {
        Map<Integer, ByteBuffer> map = v2SignBlock.getPair();
        //那我现在需要读一下
        Iterator<Map.Entry<Integer, ByteBuffer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ByteBuffer> next = iterator.next();
            Integer key = next.getKey();
            ByteBuffer value = next.getValue();
            value.flip();
            byte[] b = new byte[value.remaining()];
            value.get(b);
            String decodeValue = new String(b);
            System.out.println("id-value键值对    " + key + ":" + decodeValue);
        }
    }

    /**
     * 第一步：
     * 从文件中读取出EndOfCentralDirectory的字节数据
     *
     * @param apkFile
     * @param offset
     * @return
     * @throws Exception
     */
    static ByteBuffer findEocd(RandomAccessFile apkFile, int offset) throws Exception {
        apkFile.seek(apkFile.length() - offset); //这是整个apk文件，我先从末尾，后退到 EndOfCentralDirectory的开始位置
        //读取offset长的数据
        ByteBuffer eocd_buffer = ByteBuffer.allocate(offset);  // 这里的offset值是22，这一句意思是，把这22个字节读取缓存
        eocd_buffer.order(ByteOrder.LITTLE_ENDIAN);//MMP ??这没注释看不懂
        apkFile.read(eocd_buffer.array());// ???看不懂
        //循环查找eocd数据

        System.out.println("看看这一轮循环到底做了什么...");
        //然后这里有一个for循环, 这里还真的只有一轮循环,虽然不知道
        for (int current_offset = 0; current_offset + Constants.EOCD_COMMENT_OFFSET <= offset; current_offset++) {
            System.out.println("经过试验，他还真的只有一轮循环");
            int eocd_tag = eocd_buffer.getInt(current_offset);//一个int是4个字节 ，按照EOCD的格式图，取一个int4字节，表示，核心目录的结束标志
            if (eocd_tag == Constants.EOCD_TAG) {//检查是否真的是核心目录结束标志
                int comment_index = current_offset + Constants
                        .EOCD_COMMENT_LEN_OFFSET; //找到comment的index
                short comment_len = eocd_buffer.getShort(comment_index);// 找到注释长度的值
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
        //第二个v2签名块中的block of size 的偏移
        int block_2size_offset = offset - 24;
        apkFile.seek(block_2size_offset);
        ByteBuffer v2BlockMagicBuffer = ByteBuffer.allocate(24);
        v2BlockMagicBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //读取v2签名块中第二个block size 与magic
        apkFile.read(v2BlockMagicBuffer.array());
        //读取blocksize
        long block2_size = v2BlockMagicBuffer.getLong();// 读出sizeOfBlock
        //读取magic
        byte[] block_magic = new byte[16];
        v2BlockMagicBuffer.get(block_magic);
        //如果magic等于v2签名的magic
        System.out.println("====111111111======");
        if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, block_magic)) {//对比magic块的值是否相同，如果相同，那么就能确定这是v2签名，
            System.out.println("====确定这是V2签名的apk包=======");
            //根据block size 读取所有的v2 block数据

            //那么下一步，找到 2 ApkSigningBlock的起始位置
            apkFile.seek(offset - block2_size - 8);// 这个值计算出来，就是整个 2ApkSigningBlock的起始位置
            ByteBuffer v2BlockBuffer = ByteBuffer.allocate((int) (block2_size + 8));//先划分空间，这里已经+8，也就是跳过了
            v2BlockBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(v2BlockBuffer.array());// 再把整个2ApkSigningBlock的byte数据都写入到缓存中
            // 保存id-value
            Map pair = new LinkedHashMap<Integer, ByteBuffer>();
            // 如果第一个blocksize等于第二个
            if (v2BlockBuffer.getLong() == block2_size) {//第一个sizeOfBlock的值与第二个sizeOfBlock的值如果相同， 注意，这里getLong一下，position就开始移动了,实际上就+8了
                //循环获取 id-value
                while (v2BlockBuffer.position() < v2BlockBuffer.capacity() - 8 - 16) {// 这里，排除了最后8字节第二个sizeOfBlock和magic
                    //读取id value数据的总长度
                    long id_value_size = v2BlockBuffer.getLong();//得到这一个id-value键值对结构的总长度
                    //读取id
                    int id = v2BlockBuffer.getInt();// 拿到id
                    //读取value
                    ByteBuffer value = ByteBuffer.allocate((int) (id_value_size - 4));//为什么你去拿value的时候，总是会先去用ByteBuffer.allocate分配空间呢
                    value.order(ByteOrder.LITTLE_ENDIAN);
                    v2BlockBuffer.get(value.array());
                    pair.put(id, value);//这样就把id-value键值对拿到了
                }
                if (pair.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//如果包含id为APK_SIGNATURE_SCHEME_V2_BLOCK_ID V2签名块的默认值的一个id
                    v2BlockBuffer.flip(); // 把缓冲区position置为头部
                    System.out.println("====return new V2SignBlock(pair, v2BlockBuffer);=======");
                    return new V2SignBlock(pair, v2BlockBuffer);
                }
            } else {
                //也就是说，还存在 两个地方的sizeOfBlock不相同的情况

            }
        }
        System.out.println("====This is not a V2 signed Apk====");

        return null;
    }

    static void generateV2Channel(File fileIn, ByteBuffer eocdBuffer, String channel, V2SignBlock v2SignBlock, File fileOut) throws Exception {
        FileOutputStream fos = new FileOutputStream(fileOut);
        /**
         * 1、写入第一部分内容
         */
        int v2SigningBlockSize = v2SignBlock.getData().capacity();//V2签名块的大小
        int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);//取得centralDirectory区的起始位置
        int cdSize = eocdBuffer.getInt(Constants.EOCD_CD_SIZE_OFFSET);//取得centralDirectory区的size
        int coze_len = cdOffset - v2SigningBlockSize;//获得 1 ContentsOfZipEntries 的长度 （用第3部分的起始index 减去 第二部分的长度，就得到了第一部分长度）
        //第三部分的起始index，其实也就是第二部分的结束位置
        RandomAccessFile apkInFile = new RandomAccessFile(fileIn, "r");
        byte[] cozeBytes = new byte[coze_len];//建立缓冲数组
        apkInFile.read(cozeBytes);// 将数据读到缓冲数组中
        //写入到输出文件
        fos.write(cozeBytes);//把缓冲数组写到 输出流中

        /**
         * 2、写入签名块
         */
        byte[] channelBytes = channel.getBytes(Constants.CHARSET);//用utf-8的格式，读出去到信息
        //新签名块总大小
        int block_size = v2SigningBlockSize + 8 + 4 + channelBytes.length;//既然我们要写渠道信息到签名块中，那么就要先算出加进去之后，签名块应该是多大
        //如果已经存在了我们要添加的渠道信息的id
        if (v2SignBlock.getPair().containsKey(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID)) {//这玩意是我自定义的渠道id,那么如果已经存在
            //获取已经存在的v2渠道数据
            ByteBuffer v2ChannelValue = v2SignBlock.getPair().get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//那就把值拿出来
            block_size = block_size - 8 - 4 - v2ChannelValue.capacity();//由于我们要写入新的渠道信息，那就要把旧的长度除掉，不要同时存在两份一样的id-value
        }
        ByteBuffer newV2Block = ByteBuffer.allocate(block_size);// 来啊，分配新的签名块大小的缓冲区
        newV2Block.order(ByteOrder.LITTLE_ENDIAN);
        long blockSizeFieldValue = block_size - 8;//-8 是为了减去第一个字段，sizeOfBlock,它表示，除自己之外，整个签名块还剩下多少
        newV2Block.putLong(blockSizeFieldValue);//ok，这个值就是我们要的值
        Set<Integer> ids = v2SignBlock.getPair().keySet();//然后，我们来遍历之前就存在的id-value结构
        for (Integer id : ids) {
            //跳过已经存在的渠道id数据
            if (id != Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID) {//旧的渠道信息就不要写入了
                ByteBuffer value = v2SignBlock.getPair().get(id);// value
                newV2Block.putLong(4 + value.capacity()); //第一小块，表示这一对id-value的大小
                newV2Block.putInt(id);//放入id
                newV2Block.put(value);//放入value
            }
        }
        //添加渠道信息
        newV2Block.putLong(4 + channelBytes.length); //渠道id-value也一样
        newV2Block.putInt(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//id
        newV2Block.put(channelBytes);//value 就是入参的channelName

        newV2Block.putLong(blockSizeFieldValue); //最后还有别忘了封口，sizeOfBlock和magic
        newV2Block.put(Constants.APK_SIGNING_BLOCK_MAGIC);//和magic

        fos.write(newV2Block.array());//把缓冲区的数据写入到输出流

        /**
         * 3、写入cd 3centralDirectory核心目录区
         */
        apkInFile.seek(cdOffset);//那我seek到cd区的起始位置
        byte[] cdBytes = new byte[cdSize];//建立缓存数组
        apkInFile.read(cdBytes);//把原文件这一块的内容读取到cdByte数组
        fos.write(cdBytes);//再用输出流写入到outFile文件中

        /**
         * 4、修改eocd中的内容 ,为什么要修改它呢？ 因为：我们在第二区域内加了渠道信息，导致了第三区域的开始位置发生了变化，我们必须把这种变化写入到 eocd的16往后的4个字节中
         */
        byte[] bytes = new byte[16];
        eocdBuffer.get(bytes);//往后走16个字节,把数据读到bytes中
        eocdBuffer.getInt();//再走4个字节
        byte[] comment = new byte[eocdBuffer.capacity() - eocdBuffer.position()];//comment和comment-length数据的总长度，用byte数组装起来
        eocdBuffer.get(comment);//

        ByteBuffer newEocd = ByteBuffer.allocate(eocdBuffer.capacity());//构建一个新的eocd区，当然，大小要一模一样
        newEocd.order(ByteOrder.LITTLE_ENDIAN);
        newEocd.put(bytes);//原本存在的16字节，原样放进去
        newEocd.putInt(newV2Block.capacity() + coze_len);//第1,2部分的长度加起来，才是第三部分的起始偏移量
        newEocd.put(comment);//最后这个注释也别忘了

        fos.write(newEocd.array());//同样，写入到输出流中
        System.out.println("v2签名文件，渠道注入完成");
        eocdBuffer.flip();
        apkInFile.close();
        fos.flush();
        fos.close();
    }

    public static String getV2Channel(String sourceDir) {
        File f = new File(sourceDir);
        try {
            RandomAccessFile apkFile = new RandomAccessFile(f, "r");
            ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);//第一步，找到EOCD在整个apk的偏移位置
            if (null == eocdBuffer) {
                eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                        .EOCD_COMMENT_MAX_LEN);
            }
            if (null == eocdBuffer) {
                apkFile.close();
                throw new Exception(sourceDir + " 不是一个标准的apk文件");
            }
            //找到签名块
            int cdOffset = eocdBuffer.getInt(Constants.EOCD_CD_OFFSET);
            V2SignBlock v2SignBlock = findV2SignBlock(apkFile, cdOffset);
            if (null == v2SignBlock)
                throw new RuntimeException("v2SignBlock 是空");
            //找到id-value对
            Map<Integer, ByteBuffer> map = v2SignBlock.getPair();
            if (map != null) {
                ByteBuffer byteBuffer = map.get(Constants.APK_SIGNATURE_SCHEME_V2_CHANNEL_ID);//从id-value对中找到渠道信息
                byte[] array = byteBuffer.array();
                String str = new String(array, Constants.CHARSET); //将渠道信息组成字符串，返回出去
                return str;
            } else {
                throw new RuntimeException("getPair是空");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
