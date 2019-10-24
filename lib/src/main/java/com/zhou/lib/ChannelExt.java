package com.zhou.lib;

/**
 * �Զ���Ķ�����Ϣ
 */
public class ChannelExt {

    private String baseApkPath;//��׼apk��·��
    private String channelConfigPath;//�����������ļ���λ��
    private String themeConfigPath;//��������
    private String outDir;//����ļ���·��

    /**
     * ���ⲿȡ�õĲ����Ƿ񶼲��ǿ�
     *
     * @return
     */
    public boolean isOk() {
        return baseApkPath != null && channelConfigPath != null && outDir != null && themeConfigPath != null;
    }

    // �؄eע�⣬�������д��private��Ȼ������get/set����,��Ȼ���ô����Ե�ʱ��ᱨ��
    public String getThemeConfigPath() {
        return themeConfigPath;
    }

    public void setThemeConfigPath(String themeConfigPath) {

        this.themeConfigPath = themeConfigPath;
    }


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
