package com.example.lighteco;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * 「人が置いた / 骨粉で育てた」ブロックの記録。チャンクのデータに保存するので再起動しても消えない。
 */
public final class Placed {

    private static final int MAX_PER_CHUNK = 4096;
    private final NamespacedKey key;

    public Placed(LightEco plugin) {
        this.key = new NamespacedKey(plugin, "placed");
    }

    private static int local(Block b) {
        return ((b.getY() + 2048) << 8) | ((b.getX() & 15) << 4) | (b.getZ() & 15);
    }

    private int[] read(PersistentDataContainer pdc) {
        int[] a = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        return a == null ? new int[0] : a;
    }

    public void mark(Block b) {
        Chunk c = b.getChunk();
        PersistentDataContainer pdc = c.getPersistentDataContainer();
        int[] a = read(pdc);
        int k = local(b);
        for (int v : a) if (v == k) return;
        int[] n;
        if (a.length >= MAX_PER_CHUNK) { // 古いものから捨てる
            n = Arrays.copyOfRange(a, 1, a.length + 1);
        } else {
            n = Arrays.copyOf(a, a.length + 1);
        }
        n[n.length - 1] = k;
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, n);
    }

    /** 記録があれば消して true を返す。 */
    public boolean remove(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        int[] a = read(pdc);
        int k = local(b);
        for (int i = 0; i < a.length; i++) {
            if (a[i] != k) continue;
            int[] n = new int[a.length - 1];
            System.arraycopy(a, 0, n, 0, i);
            System.arraycopy(a, i + 1, n, i, a.length - i - 1);
            if (n.length == 0) pdc.remove(key);
            else pdc.set(key, PersistentDataType.INTEGER_ARRAY, n);
            return true;
        }
        return false;
    }
}
