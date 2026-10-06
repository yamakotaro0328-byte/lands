package dev.lands;

import dev.lands.model.ChunkPos;
import dev.lands.model.Land;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/** 1分ごとに確認し、時間が来たら全ての土地から税金を徴収する */
public class TaxTask extends BukkitRunnable {
    private final LandsPlugin plugin;

    public TaxTask(LandsPlugin plugin) { this.plugin = plugin; }

    private long intervalMs() {
        return (long) (plugin.getConfig().getDouble("tax.interval-hours", 24) * 3600_000L);
    }

    @Override
    public void run() {
        Economy eco = plugin.economy();
        LandManager lm = plugin.lands();
        if (!eco.taxEnabled()) return;
        long now = System.currentTimeMillis();
        if (lm.getNextTax() == 0) { lm.setNextTax(now + intervalMs()); lm.save(); return; }
        if (now < lm.getNextTax()) return;
        lm.setNextTax(now + intervalMs());
        collectAll();
    }

    public void collectAll() {
        Economy eco = plugin.economy();
        LandManager lm = plugin.lands();
        double perChunk = plugin.getConfig().getDouble("tax.per-chunk");
        for (Land land : new ArrayList<>(lm.getLands())) {
            double tax = eco.taxOf(land);
            if (tax <= 0) continue;
            if (land.getBank() >= tax) {
                land.setBank(land.getBank() - tax);
                LandActions.broadcast(land, "§e税金 " + eco.format(tax) + " を支払いました §7(残高: " + eco.format(land.getBank()) + ")");
                continue;
            }
            // 払えない分だけチャンクを没収（新しく保護した順）
            double shortfall = tax - land.getBank();
            land.setBank(0);
            int lose = perChunk > 0 ? (int) Math.ceil(shortfall / perChunk) : land.getChunks().size();
            List<ChunkPos> chunks = new ArrayList<>(land.getChunks());
            int lost = 0;
            for (int i = chunks.size() - 1; i >= 0 && lost < lose; i--, lost++) {
                ChunkPos pos = chunks.get(i);
                lm.unclaim(land, pos);
                if (land.getSpawn() != null && ChunkPos.of(land.getSpawn()).equals(pos)) land.setSpawn(null);
            }
            LandActions.broadcast(land, "§c税金を払えなかったため、" + lost + " チャンクの保護が解除されました！ 土地の銀行に入金してください");
            if (land.getChunks().isEmpty() && plugin.getConfig().getBoolean("tax.delete-when-empty", true)) {
                LandActions.broadcast(land, "§4全てのチャンクを失ったため、土地「" + land.getName() + "」は削除されました");
                lm.delete(land);
            }
        }
        lm.save();
    }
}
