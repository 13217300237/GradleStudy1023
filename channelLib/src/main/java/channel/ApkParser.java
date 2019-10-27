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
     * 把一个apk文件解析成我们自定义的Apk类的对象
     *
     * 1.拿到一个apk的File，我们使用RandomAccessFile将它封装，然后我们就能够访问这个文件的任意位置
     * 2.apk 文件有4个区，最后一个区是Eocd(EndOfCentralDirectory) ，这个区由于有一个comment，它的长度是不固定的，所以，整个eocd区有一个最小长度是22字节。
     * 我们要确定这个区的真实长度，就只能用假设法。
     * 首先，我们假设comment的长度是 0，那么整个区的长度就是22，要验证我们的假设，只需要在这个区的头部，读一个int，看看是否与 zip文件格式定义的`核心目录结束标志`的值相同，如果相同，那就假设成立、
     * 可是不过不相同，那么只能再假设 comment的长度是 short的最大值0xffff(因为表示comment-length的字段是short类型，short是2字节，16位，而一个16进制的位数f，只能表示4位，所以short的最大值就是 0xffff)
     * 那么，整个区的长度最大值就是22+0xffff（我没有转成10进制，不要在意这些细节），那么从头部再去读一个int，看看和核心目录结束标志`的值 是否相同，如果相同，就说明，找到了真正的头部。
     * 然后就能推导出整个区的真实长度.
     * 3. 找到整个区的byte数据之后，保存为eocd
     * 4. 从eocd中找到 核心目录的偏移位置的字段 ，它处于整个ecod区的16，然后读int 4字节，得出一个 数值，它表示 apk的 核心目录区的偏移位置.
     * 5. 利用上面的apk的 核心目录区的偏移位置 找到ApkSigningBlock的字节数组 并且保存起来
     * 6. apk 解析结束
     * @param file
     * @return
     * @throws Exception
     */
    public static Apk parser(File file) throws Exception {
        Apk apk = new Apk(file);
        RandomAccessFile apkFile = new RandomAccessFile(file, "r");
        //查找eocd数据
        ByteBuffer eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET);
        //先假设注释的长度是0，则整个eocd区的长度就是22，此时去头部寻找核心目录结束标记，此时刚好找到，那么说明eocd区的大小确实是0
        if (null == eocdBuffer) {//但是如果查找失败，那么就假设注释的长度是short的最大值，short2字节，16位，一个16进制的数表示4位，所以需要4个16进制的数，最大值则是0xffff
            eocdBuffer = findEocd(apkFile, Constants.EOCD_COMMENT_OFFSET + Constants
                    .EOCD_COMMENT_MAX_LEN);
        }
        if (null == eocdBuffer) {
            apkFile.close();
            throw new Exception(file.getPath() + " 不是一个标准的apk文件");
        }
        apk.setEocd(new EndOfCentralDirectory(eocdBuffer));
        int cdOffset = apk.getEocd().getCdOffset();//核心目录相对于整个apk的偏移位置

        //查找v2签名块
        apk.setApkSigningBlock(findV2SignBlock(apkFile, cdOffset));
        if (!apk.isV2()) {
            apk.setV1(isV1(file));
        }
        if (!apk.isV1() && !apk.isV2()) {
            apkFile.close();
            throw new Exception(file.getPath() + " 没有签名");
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
        //第二个v2签名块中的block of size 的偏移
        int block_2size_offset = cdOffset - 24;//-24是为了 -8-16
        apkFile.seek(block_2size_offset); //seek
        ByteBuffer v2BlockMagicBuffer = ByteBuffer.allocate(24);//给一个24大小的数组
        v2BlockMagicBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //读取v2签名块中第二个block size 与magic
        apkFile.read(v2BlockMagicBuffer.array());//往数组里面读24字节
        //读取blocksize
        long block2_size = v2BlockMagicBuffer.getLong();//然后从数组中先读一个long，这个值表示签名块的大小
        //读取magic
        byte[] block_magic = new byte[16];
        v2BlockMagicBuffer.get(block_magic);//再读16个字节的magic
        if (Arrays.equals(Constants.APK_SIGNING_BLOCK_MAGIC, block_magic)) {//如果拿到的magic刚好等于签名块规定的magic
            //就说明存在V2签名
            //根据block size 读取所有的v2 block数据
            apkFile.seek(cdOffset - block2_size - 8);//既然存在V2签名，那么，我seek到签名块的头部去
            ByteBuffer v2BlockBuffer = ByteBuffer.allocate((int) (block2_size + 8));//然后我要建立一个刚好装下签名块所有数据的数组
            v2BlockBuffer.order(ByteOrder.LITTLE_ENDIAN);
            apkFile.read(v2BlockBuffer.array());//把字节数据读到数组中去
            //保存id-value
            Map pair = new LinkedHashMap<Integer, ByteBuffer>();
            //如果第一个blocksize等于第二个
            if (v2BlockBuffer.getLong() == block2_size) {//读一个long，并且移动location,将long值与之前获得的blockSize对比，只有相同，才是正确的文件格式
                //这里加一个校验是因为，只要文件格式不对，apk就无法安装，为了避免安装的时候才发现问题，不如早点自己校验
                //循环获取 id-value
                while (v2BlockBuffer.position() < v2BlockBuffer.capacity() - 8 - 16) {//-8-16 是因为最后这24的字节并不是id-value的内容
                    //读取id value数据的总长度
                    long id_value_size = v2BlockBuffer.getLong();//id+value的长度。不包括自身
                    //读取id
                    int id = v2BlockBuffer.getInt();//id 是int占4字节
                    //读取value
                    ByteBuffer value = ByteBuffer.allocate((int) (id_value_size - 4)); //value就占他们的差值
                    value.order(ByteOrder.LITTLE_ENDIAN);
                    v2BlockBuffer.get(value.array());//读进去
                    pair.put(id, value);//保存起来
                }
                if (pair.containsKey(Constants.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {//再次校验，因为原有apk文件中，必定包含一个固定的id-value，这里先检查，然后再
                    v2BlockBuffer.flip();
                    return new ApkSigningBlock(pair, v2BlockBuffer);
                }
            }
        }

        return null;
    }

    /**
     * @param apkFile RandomAccessFile随机访问文件流，可以随意访问文件的任何位置
     * @param offset  偏移量
     * @return
     * @throws Exception
     */
    static ByteBuffer findEocd(RandomAccessFile apkFile, int offset) throws Exception {
        apkFile.seek(apkFile.length() - offset);
        //读取offset长的数据
        ByteBuffer eocd_buffer = ByteBuffer.allocate(offset);//这时候，如果eocd区的长度是最大值，即 注释长度最大值0xffff + 22
        eocd_buffer.order(ByteOrder.LITTLE_ENDIAN);
        apkFile.read(eocd_buffer.array());
        //循环查找eocd数据
        for (int current_offset = 0;
             current_offset + Constants.EOCD_COMMENT_OFFSET <= offset;
             current_offset++) {//循环查找，code区的开始的int
            int eocd_tag = eocd_buffer.getInt(current_offset);
            if (eocd_tag == Constants.EOCD_TAG) {//如果找到
                int comment_index = current_offset + Constants.EOCD_COMMENT_LEN_OFFSET;//找到注释长度属性的index
                short comment_len = eocd_buffer.getShort(comment_index);//然后拿到这个值,这个值就是注释的长度
                if (comment_len == offset - comment_index - 2) {//判定数据格式是否正确
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
