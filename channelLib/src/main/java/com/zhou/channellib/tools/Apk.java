package com.zhou.channellib.tools;

import com.zhou.channellib.tools.bean.ApkSigningBlock;
import com.zhou.channellib.tools.bean.CentralDirectory;
import com.zhou.channellib.tools.bean.ContentsOfZipEntries;
import com.zhou.channellib.tools.bean.EndOfCentralDirectory;

import java.io.File;

public class Apk {

    File apkFile;
    //一个apk文件其实是一个zip格式的压缩文件，只不过改了后缀名。其内部格式依然是遵循zip。
    //而一个zip文件，正常情况下包含3个部分。
    //1. 内容条目区 ContentsOfZipEntries
    //2. 中央目录区 CentralDirectory
    //3. 中央目录区末尾 EndOfCentralDirectory

    //那么我们的目的就是要把一个apk文件提取出这3个部分的ByteBuffer存在对象中

    //但是，经过了ApkV2签名之后，多了一个区，叫做
    //Apk签名块 ApkSigningBlock

    //于是就变成了这样
    //1内容条目区 ContentsOfZipEntries||2Apk签名块 ApkSigningBlock||3中央目录区 CentralDirectory||4. 中央目录区末尾 EndOfCentralDirectory

    //所以我们决定这里有4个字段
    public ContentsOfZipEntries coze;
    public ApkSigningBlock asb;
    public CentralDirectory cd;
    public EndOfCentralDirectory eocd;

    /**
     * 一个apk是否是v2签名
     *
     * @return
     */
    public boolean getIfV2() {
        return asb == null;
    }

    public Apk(File apkFile) {
        this.apkFile = apkFile;
    }
}


