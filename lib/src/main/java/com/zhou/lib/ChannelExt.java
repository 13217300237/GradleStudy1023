package com.zhou.lib;

/**
 * 自定义的额外信息
 */
public class ChannelExt {

    private String baseApkPath;//基准apk的路径
    private String channelConfigPath;//多渠道配置文件的位置
    private String themeConfigPath;//多主题打包
    private String outDir;//输出文件的路径

    /**
     * 从外部取得的参数是否都不是空
     *
     * @return
     */
    public boolean isOk() {
        return baseApkPath != null && channelConfigPath != null && outDir != null && themeConfigPath != null;
    }

    // 特別注意，这里必须写成private，然后生成get/set方法,不然引用此属性的时候会报错
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
