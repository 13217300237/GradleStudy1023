package com.zhou.channellib.tools;

import com.zhou.channellib.tools.bean.ApkSigningBlock;
import com.zhou.channellib.tools.bean.CentralDirectory;
import com.zhou.channellib.tools.bean.ContentsOfZipEntries;
import com.zhou.channellib.tools.bean.EndOfCentralDirectory;

import java.io.File;

public class Apk {

    File apkFile;
    //һ��apk�ļ���ʵ��һ��zip��ʽ��ѹ���ļ���ֻ�������˺�׺�������ڲ���ʽ��Ȼ����ѭzip��
    //��һ��zip�ļ�����������°���3�����֡�
    //1. ������Ŀ�� ContentsOfZipEntries
    //2. ����Ŀ¼�� CentralDirectory
    //3. ����Ŀ¼��ĩβ EndOfCentralDirectory

    //��ô���ǵ�Ŀ�ľ���Ҫ��һ��apk�ļ���ȡ����3�����ֵ�ByteBuffer���ڶ�����

    //���ǣ�������ApkV2ǩ��֮�󣬶���һ����������
    //Apkǩ���� ApkSigningBlock

    //���Ǿͱ��������
    //1������Ŀ�� ContentsOfZipEntries||2Apkǩ���� ApkSigningBlock||3����Ŀ¼�� CentralDirectory||4. ����Ŀ¼��ĩβ EndOfCentralDirectory

    //�������Ǿ���������4���ֶ�
    public ContentsOfZipEntries coze;
    public ApkSigningBlock asb;
    public CentralDirectory cd;
    public EndOfCentralDirectory eocd;

    /**
     * һ��apk�Ƿ���v2ǩ��
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


