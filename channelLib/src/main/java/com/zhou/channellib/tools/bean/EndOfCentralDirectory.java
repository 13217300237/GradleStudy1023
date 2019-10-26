package com.zhou.channellib.tools.bean;

public class EndOfCentralDirectory {

    public byte[] byteArr;
    public int cdOffset;
    public int cdSize;

    public void setByteArr(byte[] byteBuffer) {
        this.byteArr = byteBuffer;
    }

    public void setCdOffset(int cdOffset) {
        this.cdOffset = cdOffset;
    }

    public void setCdSize(int cdSize) {
        this.cdSize = cdSize;
    }

    /**
     * 获得cd区的偏移位置
     *
     * @return
     */
    public int getCdOffset() {
        return cdOffset;
    }

    /**
     * 获得cd区的大小
     *
     * @return
     */
    public int getCdSize() {
        return cdSize;
    }

}
