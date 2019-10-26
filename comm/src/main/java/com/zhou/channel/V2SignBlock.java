package com.zhou.channel;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by Lance on 2017/5/20.
 */

public class V2SignBlock {

    private Map<Integer,ByteBuffer> pair;

    private ByteBuffer data;

    public V2SignBlock(Map pair, ByteBuffer data) {
        this.pair = pair;
        this.data = data;
    }

    public Map<Integer,ByteBuffer> getPair() {
        return pair;
    }

    public ByteBuffer getData() {
        return data;
    }
}
