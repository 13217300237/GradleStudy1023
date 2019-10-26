package com.zhou.channellib.tools.bean;

import java.util.Map;

public class ApkSigningBlock {
    public int offset;

    public byte[] byteArr;
    public Map<Integer, byte[]> idValueMap;

    public Map<Integer, byte[]> getIdValueMap() {
        return idValueMap;
    }

    public void setIdValueMap(Map<Integer, byte[]> idValueMap) {
        this.idValueMap = idValueMap;
    }

    public void setByteArr(byte[] byteBuffer) {
        this.byteArr = byteBuffer;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
