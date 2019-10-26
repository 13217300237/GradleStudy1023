package channel.data;

import java.io.File;

import channel.data.bean.ApkSigningBlock;
import channel.data.bean.EndOfCentralDirectory;

/**
 * Created by Lance on 2017/5/20.
 */

public class Apk {

    private File file;
    private EndOfCentralDirectory eocd;
    private ApkSigningBlock ApkSigningBlock;
    private boolean isV2;
    private boolean isV1;

    public Apk(File file) {
        this.file = file;
    }

    public void setEocd(EndOfCentralDirectory eocd) {
        this.eocd = eocd;
    }

    public EndOfCentralDirectory getEocd() {
        return eocd;
    }

    public void setApkSigningBlock(ApkSigningBlock ApkSigningBlock) {
        this.ApkSigningBlock = ApkSigningBlock;
        if (ApkSigningBlock != null) {
            isV2 = true;
        }
    }

    public ApkSigningBlock getApkSigningBlock() {
        return ApkSigningBlock;
    }

    public boolean isV2() {
        return isV2;
    }

    public void setV2(boolean v2) {
        isV2 = v2;
    }

    public boolean isV1() {
        return isV1;
    }

    public void setV1(boolean v1) {
        isV1 = v1;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "Apk{" +
                "file=" + file +
                ", eocd=" + eocd +
                ", ApkSigningBlock=" + ApkSigningBlock +
                ", isV2=" + isV2 +
                ", isV1=" + isV1 +
                '}';
    }
}
