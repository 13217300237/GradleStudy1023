package channel.data;

/**
 * Created by Lance on 2017/5/20.
 */

public class Constants {
    //eocd的除了注释内容的长度 可能的最小eocd的长度
    public static final int EOCD_COMMENT_OFFSET = 22;

    //eocd长度属性的偏移
    public static final int EOCD_COMMENT_LEN_OFFSET = 20;

    //eocd中cd偏移的属性位置
    public static final int EOCD_CD_OFFSET = 16;

    //eocd中cd大小的属性位置
    public static final int EOCD_CD_SIZE_OFFSET = 12;

    //eocd注释内容最大长度
    public static final int EOCD_COMMENT_MAX_LEN = 0xffff;

    //eocd标示
    public static final int EOCD_TAG = 0x06054b50;


    //v2签名块的magic
    public static final byte[] APK_SIGNING_BLOCK_MAGIC =
            new byte[]{
                    0x41, 0x50, 0x4b, 0x20, 0x53, 0x69, 0x67, 0x20,
                    0x42, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x34, 0x32,
            };

    //v2签名的id
    public static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a;

    //渠道信息id
    public static final int APK_SIGNATURE_SCHEME_V2_CHANNEL_ID = 0x7109871f;

    public static final String CHARSET = "utf-8";
}
