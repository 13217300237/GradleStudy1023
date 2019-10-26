package channel.data.bean;

import java.nio.ByteBuffer;
import java.util.Map;

public class ApkSigningBlock {

    private Map<Integer, ByteBuffer> pair;

    private ByteBuffer data;

    public ApkSigningBlock(Map pair, ByteBuffer data) {
        this.pair = pair;
        this.data = data;
    }

    public Map<Integer, ByteBuffer> getPair() {
        return pair;
    }

    public ByteBuffer getData() {
        return data;
    }
}
