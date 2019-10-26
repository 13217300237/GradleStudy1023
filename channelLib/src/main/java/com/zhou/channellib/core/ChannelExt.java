package com.zhou.channellib.core;

public class ChannelExt {

    private String baseApkPath;
    private String channelConfigPath;
    private String themeConfigPath;
    private String outDir;

    public boolean isOk() {
        return baseApkPath != null && channelConfigPath != null && outDir != null && themeConfigPath != null;
    }

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
