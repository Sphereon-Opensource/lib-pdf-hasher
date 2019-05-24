package com.sphereon.libs.pdfhasher;

import com.sphereon.libs.blockchain.commons.Digest;

import java.util.ArrayList;
import java.util.List;

import static com.sphereon.libs.blockchain.commons.Utils.Hex;


public class MerkleTree {

    private final Digest digest = Digest.getInstance();

    private boolean _hashLeafs;

    private List<byte[]> _leafs = new ArrayList<>();

    private List<List<byte[]>> _levels = new ArrayList<>();


    public MerkleTree(boolean hashLeafs) {
        this._hashLeafs = hashLeafs;
    }


    public void reset() {
        this._leafs.clear();
        this._levels.clear();
    }


    public byte[] getMerkleRoot() {
        if (this._levels.size() == 0) {
            return null;
        }
        return this._levels.get(0).get(0);
    }


    public String getMerkleRootAsHex() {
        return Hex.encodeAsString(getMerkleRoot());
    }


    public MerkleTree addLeaf(byte[] data) {
        if (this._hashLeafs == true) {
            data = digest.getSHA256Hash(data);
        }
        this._leafs.add(data);
        return this;
    }


    public MerkleTree addLeafs(List<byte[]> leafs) {
        for (int i = 0; i < leafs.size(); i++) {
            this.addLeaf(leafs.get(i));
        }
        return this;
    }


    public MerkleTree build() {
        if (this._leafs.size() > 0) {
            this._levels.add(0, this._leafs);
            while (this._levels.get(0).size() > 1) {
                this._levels.add(0, this.calculateLevel());
            }
        }
        return this;
    }


    private List<byte[]> calculateLevel() {
        List<byte[]> nodes = new ArrayList<>();
        List<byte[]> level = this._levels.get(0);
        int count = level.size();
        for (int i = 0; i < count; i += 2) {
            if (i + 1 <= count - 1) {
                final byte[] joined = this.join(level.get(i), level.get(i + 1));
                nodes.add(digest.getSHA256Hash(joined));
            } else {
                nodes.add(level.get(i));
            }
        }
        return nodes;
    }


    private byte[] join(byte[] leftBytes, byte[] rightBytes) {
        byte[] out = new byte[leftBytes.length + rightBytes.length];
        System.arraycopy(leftBytes, 0, out, 0, leftBytes.length);
        System.arraycopy(rightBytes, 0, out, leftBytes.length, rightBytes.length);
        return out;
    }
}


