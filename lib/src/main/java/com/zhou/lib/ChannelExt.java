package com.zhou.lib;

/**
 * �Զ���Ķ�����Ϣ
 */
public class ChannelExt {

    private String baseApkPath;//��׼apk��·��
    private String channelConfigPath;//�����������ļ���λ��
    private String outDir;//����ļ���·��

    public boolean isOk() {
        return baseApkPath != null && channelConfigPath != null && outDir != null;
    }

    // �؄eע�⣬�������д��private��Ȼ������get/set����,��Ȼ���ô����Ե�ʱ��ᱨ��

    public String getBaseApkPath() {
        return baseApkPath;
    }

    public void setBaseApkPath(String baseApkPath) {
        this.baseApkPath = baseApkPath;
    }

    public String getChannelConfigPath() {
        return channelConfigPath;
    }

    public void setChannelConfigPath(String channelConfigPath) {
        this.channelConfigPath = channelConfigPath;
    }

    public String getOutDir() {
        return outDir;
    }

    public void setOutDir(String outDir) {
        this.outDir = outDir;
    }
}
