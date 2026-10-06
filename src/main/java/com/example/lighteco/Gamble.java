package com.example.lighteco;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** ギャンブル（コインフリップ / スロット）。 */
public final class Gamble {

    private final LightEco plugin;
    private final Map<UUID, Long> last = new HashMap<>();

    public Gamble(LightEco plugin) {
        this.plugin = plugin;
    }

    public void forget(UUID id) {
        last.remove(id);
    }

    public void play(Player p, String game, long bet) {
        var cfg = plugin.getConfig();
        long max = cfg.getLong("gamble.max-bet", 100000);
        if (bet <= 0) {
            plugin.msg(p, "<red>金額は1以上で指定してください。");
            return;
        }
        if (bet > max) {
            plugin.msg(p, "<red>1回の上限は " + plugin.data.fmt(max) + " です。");
            return;
        }

        long now = System.currentTimeMillis();
        long wait = (long) (cfg.getDouble("gamble.cooldown-seconds", 2) * 1000);
        if (now - last.getOrDefault(p.getUniqueId(), 0L) < wait) {
            plugin.msg(p, "<red>少し待ってから遊んでください。");
            return;
        }

        if (!plugin.data.take(p.getUniqueId(), bet)) {
            plugin.msg(p, "<red>所持金が足りません。 (所持金: " + plugin.data.fmt(plugin.data.get(p.getUniqueId())) + ")");
            return;
        }
        last.put(p.getUniqueId(), now);

        double r = ThreadLocalRandom.current().nextDouble();
        double mult;
        String text;
        if (game.equals("coin")) {
            mult = r < 0.47 ? 2.0 : 0;
            text = mult > 0 ? "<green>表！" : "<red>裏…";
        } else {
            if (r < 0.03) {
                mult = 10;
                text = "<gold>💎💎💎 ジャックポット！";
            } else if (r < 0.13) {
                mult = 3;
                text = "<yellow>⭐⭐⭐ 大当たり！";
            } else if (r < 0.38) {
                mult = 1.5;
                text = "<green>🍒🍒 小当たり！";
            } else {
                mult = 0;
                text = "<red>ハズレ…";
            }
        }

        long payout = (long) (bet * mult);
        if (payout > 0) plugin.data.add(p.getUniqueId(), payout);

        String result = payout > 0
                ? "<green>+" + plugin.data.fmt(payout - bet) + " <gray>(x" + mult + ")"
                : "<red>-" + plugin.data.fmt(bet);
        plugin.msg(p, text + " " + result + " <gray>所持金: <white>" + plugin.data.fmt(plugin.data.get(p.getUniqueId())));
    }
}
