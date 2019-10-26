package channel.data.bean;

import java.nio.ByteBuffer;

import channel.data.Constants;

public class EndOfCentralDirectory {

    private ByteBuffer data;

    public EndOfCentralDirectory(ByteBuffer data) {
        this.data = data;
    }

    public int getCdOffset() {
        return data.getInt(Constants.EOCD_CD_OFFSET);
    }

    public int getCdSize() {
        return data.getInt(Constants.EOCD_CD_SIZE_OFFSET);
    }

    public ByteBuffer getData() {
        return data;
    }

}
