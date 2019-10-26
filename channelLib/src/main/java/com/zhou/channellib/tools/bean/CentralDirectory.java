package com.zhou.channellib.tools.bean;

public class CentralDirectory {
    public byte[] byteArr;
    public int offset;
    int size;

    public void setByteArr(byte[] cdBytes) {
        this.byteArr = cdBytes;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
