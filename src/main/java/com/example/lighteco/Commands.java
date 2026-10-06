package com.example.lighteco;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

public final class Commands implements CommandExecutor, TabCompleter {

    private static final Pattern NATION_NAME = Pattern.compile("^[\\p{L}\\p{N}_]{2,12}$");

    private final LightEco plugin;
    private final Data d;
    private final Map<UUID, String> invites = new HashMap<>();
    private final Map<UUID, Long> inviteExpire = new HashMap<>();

    public Commands(LightEco plugin) {
        this.plugin = plugin;
        this.d = plugin.data;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        switch (cmd.getName().toLowerCase()) {
            case "money" -> money(s, a);
            case "pay" -> pay(s, a);
            case "baltop" -> baltop(s);
            case "menu" -> claim(s);
            case "gamble" -> gamble(s, a);
            case "l" -> land(s, a);
            case "jobs" -> jobs(s, a);
            case "nation" -> nation(s, a);
            case "leco" -> admin(s, a);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ---------- 共通 ----------
    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        plugin.msg(s, "<red>ゲーム内のプレイヤーのみ使えます。");
        return null;
    }

    private static long amount(String s) {
        try {
            long v = Long.parseLong(s.replace(",", ""));
            return v > 0 ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    // ---------- お金 ----------
    private void money(CommandSender s, String[] a) {
        if (a.length >= 1) {
            OfflinePlayer t = find(a[0]);
            if (t == null) {
                plugin.msg(s, "<red>プレイヤーが見つかりません。");
                return;
            }
            plugin.msg(s, LightEco.esc(a[0]) + " の所持金: <white>" + d.fmt(d.get(t.getUniqueId())));
            return;
        }
        Player p = player(s);
        if (p != null) plugin.msg(p, "所持金: <white>" + d.fmt(d.get(p.getUniqueId())));
    }

    private void pay(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 2) {
            plugin.msg(p, "<red>使い方: /pay <プレイヤー> <金額>");
            return;
        }
        OfflinePlayer t = find(a[0]);
        long amt = amount(a[1]);
        if (t == null) {
            plugin.msg(p, "<red>プレイヤーが見つかりません。");
        } else if (t.getUniqueId().equals(p.getUniqueId())) {
            plugin.msg(p, "<red>自分には送れません。");
        } else if (amt < 0) {
            plugin.msg(p, "<red>金額は1以上の整数で指定してください。");
        } else if (!d.take(p.getUniqueId(), amt)) {
            plugin.msg(p, "<red>所持金が足りません。");
        } else {
            d.add(t.getUniqueId(), amt);
            plugin.msg(p, LightEco.esc(a[0]) + " に <white>" + d.fmt(amt) + "</white> を送りました。");
            if (t instanceof Player tp) plugin.msg(tp, LightEco.esc(p.getName()) + " から <white>" + d.fmt(amt) + "</white> を受け取りました。");
        }
    }

    private void baltop(CommandSender s) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(d.money.entrySet());
        list.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
        plugin.msg(s, "<gold>所持金ランキング");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            var e = list.get(i);
            plugin.msg(s, "<yellow>" + (i + 1) + ".</yellow> " + LightEco.esc(d.nameOf(e.getKey())) + " <gray>- <white>" + d.fmt(e.getValue()));
        }
    }

    private void claim(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        for (var it : p.getInventory().getContents()) { // getContents はオフハンド・防具欄も含む
            if (plugin.isMenuItem(it)) {
                plugin.msg(p, "<yellow>すでにメニューアイテムを持っています。");
                return;
            }
        }
        var left = p.getInventory().addItem(plugin.menuItem());
        if (!left.isEmpty()) plugin.msg(p, "<red>インベントリに空きがありません。");
        else plugin.msg(p, "<green>メニューアイテムを受け取りました。右クリックでメニューが開きます。");
    }

    private void gamble(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 2 || !(a[0].equalsIgnoreCase("coin") || a[0].equalsIgnoreCase("slot"))) {
            plugin.msg(p, "<red>使い方: /gamble <coin|slot> <金額>");
            return;
        }
        long amt = amount(a[1]);
        if (amt < 0) {
            plugin.msg(p, "<red>金額は1以上の整数で指定してください。");
            return;
        }
        plugin.gamble.play(p, a[0].toLowerCase(), amt);
    }

    // ---------- 土地 ----------
    private void land(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            plugin.msg(p, "使い方: /l <claim|unclaim|info|trust|untrust|list>");
            return;
        }
        UUID id = p.getUniqueId();
        var loc = p.getLocation();
        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;

        switch (a[0].toLowerCase()) {
            case "claim" -> plugin.claimHere(p);
            case "unclaim" -> {
                UUID owner = d.landOwner(world, cx, cz);
                if (owner == null || !owner.equals(id)) {
                    plugin.msg(p, "<red>ここはあなたの土地ではありません。");
                    return;
                }
                long refund = (long) (d.landPrice(d.landCount(id) - 1) * plugin.getConfig().getDouble("land.refund-rate", 0.5));
                d.unclaimLand(id, world, cx, cz);
                d.add(id, refund);
                plugin.msg(p, "<green>土地を手放しました。 <gray>返金: " + d.fmt(refund));
            }
            case "info" -> {
                UUID owner = d.landOwner(world, cx, cz);
                plugin.msg(p, owner == null
                        ? "<green>ここは誰の土地でもありません。 価格: " + d.fmt(d.landPrice(d.landCount(id)))
                        : "ここは <white>" + LightEco.esc(d.nameOf(owner)) + "</white> の土地です。");
            }
            case "list" -> plugin.msg(p, "所有チャンク数: <white>" + d.landCount(id) + "</white> / 次の価格: <white>" + d.fmt(d.landPrice(d.landCount(id))));
            case "trust", "untrust" -> {
                if (a.length < 2) {
                    plugin.msg(p, "<red>使い方: /l " + a[0] + " <プレイヤー>");
                    return;
                }
                OfflinePlayer t = find(a[1]);
                if (t == null) {
                    plugin.msg(p, "<red>プレイヤーが見つかりません。");
                    return;
                }
                Set<UUID> set = d.trusted.computeIfAbsent(id, k -> new HashSet<>());
                if (a[0].equalsIgnoreCase("trust")) {
                    set.add(t.getUniqueId());
                    plugin.msg(p, LightEco.esc(a[1]) + " にあなたの全土地での建築を許可しました。");
                } else {
                    set.remove(t.getUniqueId());
                    plugin.msg(p, LightEco.esc(a[1]) + " の許可を取り消しました。");
                }
                d.touch();
            }
            default -> plugin.msg(p, "<red>使い方: /l <claim|unclaim|info|trust|untrust|list>");
        }
    }

    // ---------- 職業 ----------
    private void jobs(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            plugin.gui.openJobs(p);
            return;
        }
        UUID id = p.getUniqueId();
        Set<String> act = plugin.jobs.active(id);
        switch (a[0].toLowerCase()) {
            case "join" -> {
                if (a.length < 2 || !Jobs.IDS.contains(a[1].toLowerCase())) {
                    plugin.msg(p, "<red>職業: " + String.join(", ", Jobs.IDS));
                } else if (act.contains(a[1].toLowerCase())) {
                    plugin.msg(p, "<yellow>すでにその職業に就いています。");
                } else if (act.size() >= plugin.getConfig().getInt("jobs.max-active", 2)) {
                    plugin.msg(p, "<red>同時に就ける職業の上限です。");
                } else {
                    act.add(a[1].toLowerCase());
                    d.touch();
                    plugin.msg(p, "<green>" + Jobs.label(a[1].toLowerCase()) + " に就職しました！");
                }
            }
            case "leave" -> {
                if (a.length >= 2 && act.remove(a[1].toLowerCase())) {
                    d.touch();
                    plugin.msg(p, "退職しました。");
                } else {
                    plugin.msg(p, "<red>使い方: /jobs leave <職業>");
                }
            }
            case "info" -> {
                for (String job : Jobs.IDS) {
                    plugin.msg(p, (act.contains(job) ? "<green>● " : "<gray>○ ") + Jobs.label(job)
                            + " <gray>Lv" + plugin.jobs.level(p, job) + " - " + Jobs.hint(job));
                }
            }
            default -> plugin.gui.openJobs(p);
        }
    }

    // ---------- 町・国 ----------
    private String typeOf(Data.Nation n) {
        return n.members.size() > plugin.getConfig().getInt("nation.town-max-members", 4) ? "国" : "町";
    }

    private void nation(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        UUID id = p.getUniqueId();
        Data.Nation mine = d.nationOfPlayer(id);
        String sub = a.length == 0 ? "info" : a[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (a.length < 2 || !NATION_NAME.matcher(a[1]).matches()) {
                    plugin.msg(p, "<red>使い方: /nation create <名前> (2〜12文字、記号・空白なし)");
                } else if (mine != null) {
                    plugin.msg(p, "<red>すでに " + LightEco.esc(mine.name) + " に所属しています。");
                } else if (d.nations.containsKey(a[1].toLowerCase())) {
                    plugin.msg(p, "<red>その名前は使われています。");
                } else {
                    long cost = plugin.getConfig().getLong("nation.create-cost", 50000);
                    if (!d.take(id, cost)) {
                        plugin.msg(p, "<red>作成には " + d.fmt(cost) + " 必要です。");
                        return;
                    }
                    Data.Nation n = new Data.Nation();
                    n.name = a[1];
                    n.leader = id;
                    n.members.add(id);
                    d.nations.put(a[1].toLowerCase(), n);
                    d.nationOf.put(id, a[1].toLowerCase());
                    d.touch();
                    Bukkit.broadcast(LightEco.MM.deserialize("<gold>" + LightEco.esc(a[1]) + " <yellow>が誕生しました！ (建国者: " + LightEco.esc(p.getName()) + ")"));
                }
            }
            case "info" -> {
                Data.Nation n = a.length >= 2 ? d.nations.get(a[1].toLowerCase()) : mine;
                if (n == null) {
                    plugin.msg(p, a.length >= 2 ? "<red>見つかりません。" : "<yellow>どこにも所属していません。 /nation create <名前>");
                    return;
                }
                List<String> names = new ArrayList<>();
                for (UUID m : n.members) names.add(d.nameOf(m));
                plugin.msg(p, "<gold>" + LightEco.esc(n.name) + " <gray>(" + typeOf(n) + ")");
                plugin.msg(p, "リーダー: <white>" + LightEco.esc(d.nameOf(n.leader)));
                plugin.msg(p, "メンバー(" + n.members.size() + "): <white>" + LightEco.esc(String.join(", ", names)));
                plugin.msg(p, "国庫: <white>" + d.fmt(n.bank));
            }
            case "list" -> {
                List<Data.Nation> list = new ArrayList<>(d.nations.values());
                list.sort((x, y) -> Integer.compare(y.members.size(), x.members.size()));
                if (list.isEmpty()) plugin.msg(p, "<gray>まだ町・国はありません。");
                for (Data.Nation n : list) {
                    plugin.msg(p, "<gold>" + LightEco.esc(n.name) + " <gray>(" + typeOf(n) + ", " + n.members.size() + "人)");
                }
            }
            case "invite" -> {
                if (mine == null || !mine.leader.equals(id)) {
                    plugin.msg(p, "<red>リーダーのみ使えます。");
                    return;
                }
                Player t = a.length >= 2 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) {
                    plugin.msg(p, "<red>オンラインのプレイヤー名を指定してください。");
                } else if (d.nationOf.containsKey(t.getUniqueId())) {
                    plugin.msg(p, "<red>そのプレイヤーはすでに所属しています。");
                } else {
                    invites.put(t.getUniqueId(), mine.name.toLowerCase());
                    inviteExpire.put(t.getUniqueId(), System.currentTimeMillis() + 60_000);
                    plugin.msg(p, LightEco.esc(t.getName()) + " を招待しました。");
                    plugin.msg(t, "<gold>" + LightEco.esc(mine.name) + "</gold> から招待されました。 60秒以内に <white>/nation accept</white>");
                }
            }
            case "accept" -> {
                String key = invites.remove(id);
                Long exp = inviteExpire.remove(id);
                Data.Nation n = key == null ? null : d.nations.get(key);
                if (n == null || exp == null || exp < System.currentTimeMillis()) {
                    plugin.msg(p, "<red>有効な招待がありません。");
                } else if (mine != null) {
                    plugin.msg(p, "<red>すでに所属しています。");
                } else {
                    n.members.add(id);
                    d.nationOf.put(id, key);
                    d.touch();
                    plugin.msg(p, "<green>" + LightEco.esc(n.name) + " に参加しました！");
                    Player leader = Bukkit.getPlayer(n.leader);
                    if (leader != null) plugin.msg(leader, LightEco.esc(p.getName()) + " が参加しました。");
                }
            }
            case "leave" -> {
                if (mine == null) {
                    plugin.msg(p, "<red>どこにも所属していません。");
                } else if (mine.leader.equals(id) && mine.members.size() > 1) {
                    plugin.msg(p, "<red>リーダーは抜けられません。 /nation disband で解散してください。");
                } else if (mine.leader.equals(id)) {
                    disband(mine);
                    plugin.msg(p, "解散しました。");
                } else {
                    mine.members.remove(id);
                    d.nationOf.remove(id);
                    d.touch();
                    plugin.msg(p, LightEco.esc(mine.name) + " を抜けました。");
                }
            }
            case "kick" -> {
                if (mine == null || !mine.leader.equals(id)) {
                    plugin.msg(p, "<red>リーダーのみ使えます。");
                    return;
                }
                OfflinePlayer t = a.length >= 2 ? find(a[1]) : null;
                if (t == null || !mine.members.contains(t.getUniqueId()) || t.getUniqueId().equals(id)) {
                    plugin.msg(p, "<red>追放できるメンバーを指定してください。");
                    return;
                }
                mine.members.remove(t.getUniqueId());
                d.nationOf.remove(t.getUniqueId());
                d.touch();
                plugin.msg(p, LightEco.esc(a[1]) + " を追放しました。");
            }
            case "disband" -> {
                if (mine == null || !mine.leader.equals(id)) {
                    plugin.msg(p, "<red>リーダーのみ使えます。");
                    return;
                }
                String name = mine.name;
                disband(mine);
                Bukkit.broadcast(LightEco.MM.deserialize("<gold>" + LightEco.esc(name) + " <yellow>は解散しました。"));
            }
            case "deposit", "withdraw" -> {
                if (mine == null) {
                    plugin.msg(p, "<red>どこにも所属していません。");
                    return;
                }
                long amt = a.length >= 2 ? amount(a[1]) : -1;
                if (amt < 0) {
                    plugin.msg(p, "<red>使い方: /nation " + sub + " <金額>");
                } else if (sub.equals("deposit")) {
                    if (!d.take(id, amt)) {
                        plugin.msg(p, "<red>所持金が足りません。");
                        return;
                    }
                    mine.bank += amt;
                    d.touch();
                    plugin.msg(p, "国庫に " + d.fmt(amt) + " 入金しました。");
                } else if (!mine.leader.equals(id)) {
                    plugin.msg(p, "<red>引き出しはリーダーのみです。");
                } else if (mine.bank < amt) {
                    plugin.msg(p, "<red>国庫の残高が足りません。");
                } else {
                    mine.bank -= amt;
                    d.add(id, amt);
                    plugin.msg(p, "国庫から " + d.fmt(amt) + " 引き出しました。");
                }
            }
            default -> plugin.msg(p, "使い方: /nation <create|info|list|invite|accept|leave|kick|disband|deposit|withdraw>");
        }
    }

    private void disband(Data.Nation n) {
        d.add(n.leader, n.bank); // 国庫はリーダーに返す
        for (UUID m : n.members) d.nationOf.remove(m);
        d.nations.remove(n.name.toLowerCase());
        d.touch();
    }

    // ---------- 管理者 ----------
    private void admin(CommandSender s, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            plugin.msg(s, "<green>config.yml を再読み込みしました。");
            return;
        }
        if (a.length < 3) {
            plugin.msg(s, "<red>使い方: /leco <give|take|set> <プレイヤー> <金額> | /leco reload");
            return;
        }
        OfflinePlayer t = find(a[1]);
        long amt;
        try {
            amt = Long.parseLong(a[2]);
        } catch (NumberFormatException e) {
            amt = -1;
        }
        if (t == null || amt < 0) {
            plugin.msg(s, "<red>プレイヤー名と0以上の金額を指定してください。");
            return;
        }
        UUID id = t.getUniqueId();
        switch (a[0].toLowerCase()) {
            case "give" -> d.add(id, amt);
            case "take" -> d.set(id, d.get(id) - amt);
            case "set" -> d.set(id, amt);
            default -> {
                plugin.msg(s, "<red>give / take / set / reload のいずれかです。");
                return;
            }
        }
        plugin.msg(s, LightEco.esc(a[1]) + " の所持金: <white>" + d.fmt(d.get(id)));
    }

    // ---------- タブ補完 ----------
    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase();
        if (a.length == 1) {
            switch (name) {
                case "l" -> out.addAll(List.of("claim", "unclaim", "info", "trust", "untrust", "list"));
                case "jobs" -> out.addAll(List.of("join", "leave", "info"));
                case "gamble" -> out.addAll(List.of("coin", "slot"));
                case "leco" -> out.addAll(List.of("give", "take", "set", "reload"));
                case "nation" -> out.addAll(List.of("create", "info", "list", "invite", "accept", "leave", "kick", "disband", "deposit", "withdraw"));
                case "money", "pay" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        } else if (a.length == 2) {
            if (name.equals("jobs")) out.addAll(Jobs.IDS);
            else if (name.equals("leco") || (name.equals("l") && a[0].toLowerCase().contains("trust"))
                    || (name.equals("nation") && List.of("invite", "kick").contains(a[0].toLowerCase()))) {
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            }
        }
        String prefix = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(prefix));
        return out;
    }
}
