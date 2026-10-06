package dev.lands;

import dev.lands.model.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

import static dev.lands.LandsPlugin.msg;

/** コマンドとGUIの両方から使う土地操作 */
public final class LandActions {
    private LandActions() {}

    private static LandManager lm() { return LandsPlugin.get().lands(); }
    private static Economy eco() { return LandsPlugin.get().economy(); }

    public static Land requireLand(Player p) {
        Land land = lm().getLandOf(p.getUniqueId());
        if (land == null) msg(p, "§cあなたはどの土地にも所属していません");
        return land;
    }

    private static boolean requireManage(Player p, Land land) {
        if (land.getRole(p.getUniqueId()).canManage() || p.hasPermission("lands.admin")) return true;
        msg(p, "§cこの操作には信頼メンバー以上の権限が必要です");
        return false;
    }

    private static boolean requireOwner(Player p, Land land) {
        if (land.getOwner().equals(p.getUniqueId())) return true;
        msg(p, "§cこの操作はオーナーのみ可能です");
        return false;
    }

    public static boolean create(Player p, String name) {
        int max = LandsPlugin.get().getConfig().getInt("max-name-length", 16);
        if (lm().getLandOf(p.getUniqueId()) != null) { msg(p, "§c既に土地に所属しています"); return false; }
        if (!name.matches("[\\p{L}\\p{N}_-]+") || name.length() > max) {
            msg(p, "§c土地名は" + max + "文字以内の英数字・日本語・_-で入力してください"); return false;
        }
        if (lm().getLand(name) != null) { msg(p, "§cその名前の土地は既に存在します"); return false; }
        ChunkPos pos = ChunkPos.of(p.getLocation());
        if (lm().getLandAt(pos) != null || LandsPlugin.get().getConfig().getStringList("disabled-worlds").contains(pos.world())) {
            msg(p, "§c未保護のチャンクで作成してください"); return false;
        }
        double cost = eco().createCost();
        if (!eco().withdraw(p, cost)) { msg(p, "§c所持金が足りません（必要: " + eco().format(cost) + "）"); return false; }
        Land land = lm().create(name, p.getUniqueId());
        land.setSpawn(p.getLocation());
        lm().claim(land, pos); // 最初の1チャンクは作成費用に含む
        msg(p, "§a土地「" + land.getName() + "」を作成しました！" + (cost > 0 ? " §7(-" + eco().format(cost) + ")" : ""));
        if (eco().claimCost() > 0) msg(p, "§7チャンクを増やすには土地の銀行に入金してください");
        return true;
    }

    public static boolean claim(Player p, ChunkPos pos) {
        Land land = requireLand(p);
        if (land == null || !requireManage(p, land)) return false;
        if (LandsPlugin.get().getConfig().getStringList("disabled-worlds").contains(pos.world())) {
            msg(p, "§cこのワールドでは土地を保護できません"); return false;
        }
        Land at = lm().getLandAt(pos);
        if (at == land) { msg(p, "§eこのチャンクは既にあなたの土地です"); return false; }
        if (at != null) { msg(p, "§cこのチャンクは「" + at.getName() + "」の土地です"); return false; }
        int max = LandsPlugin.get().getConfig().getInt("max-chunks", 64);
        if (land.getChunks().size() >= max && !p.hasPermission("lands.admin")) {
            msg(p, "§cチャンク数の上限(" + max + ")に達しています"); return false;
        }
        if (land.getSpawn() == null && ChunkPos.of(p.getLocation()).equals(pos)) land.setSpawn(p.getLocation());
        double cost = eco().claimCost();
        if (land.getBank() < cost) {
            msg(p, "§c土地の銀行残高が足りません（必要: " + eco().format(cost) + " / 残高: " + eco().format(land.getBank()) + "）"); return false;
        }
        land.setBank(land.getBank() - cost);
        lm().claim(land, pos);
        if (cost > 0) msg(p, "§7銀行から " + eco().format(cost) + " を支払いました");
        msg(p, "§aチャンク(" + pos.x() + ", " + pos.z() + ")を保護しました §7[" + land.getChunks().size() + "/" + max + "]");
        return true;
    }

    public static boolean unclaim(Player p, ChunkPos pos) {
        Land land = requireLand(p);
        if (land == null || !requireManage(p, land)) return false;
        if (lm().getLandAt(pos) != land) { msg(p, "§cこのチャンクはあなたの土地ではありません"); return false; }
        if (land.getChunks().size() <= 1) { msg(p, "§c最後のチャンクは解除できません。土地を削除してください"); return false; }
        land.setBank(land.getBank() + eco().unclaimRefund());
        lm().unclaim(land, pos);
        if (land.getSpawn() != null && ChunkPos.of(land.getSpawn()).equals(pos)) land.setSpawn(null);
        msg(p, "§eチャンク(" + pos.x() + ", " + pos.z() + ")の保護を解除しました");
        return true;
    }

    public static void invite(Player p, Player target) {
        Land land = requireLand(p);
        if (land == null || !requireManage(p, land)) return;
        if (lm().getLandOf(target.getUniqueId()) != null) { msg(p, "§cそのプレイヤーは既に土地に所属しています"); return; }
        land.getInvites().add(target.getUniqueId());
        lm().save();
        msg(p, "§a" + target.getName() + " を招待しました");
        msg(target, "§a土地「" + land.getName() + "」に招待されました！ §e/lands §aのメニューから承認できます");
    }

    public static void accept(Player p, Land land) {
        if (lm().getLandOf(p.getUniqueId()) != null) { msg(p, "§c既に土地に所属しています"); return; }
        if (!land.getInvites().remove(p.getUniqueId())) { msg(p, "§c招待が見つかりません"); return; }
        land.getMembers().put(p.getUniqueId(), Role.MEMBER);
        land.getBanned().remove(p.getUniqueId());
        lm().save();
        msg(p, "§a土地「" + land.getName() + "」に参加しました");
        broadcast(land, "§a" + p.getName() + " が土地に参加しました");
    }

    public static void decline(Player p, Land land) {
        land.getInvites().remove(p.getUniqueId());
        lm().save();
        msg(p, "§e招待を拒否しました");
    }

    public static void leave(Player p) {
        Land land = requireLand(p);
        if (land == null) return;
        if (land.getOwner().equals(p.getUniqueId())) { msg(p, "§cオーナーは脱退できません。譲渡か削除をしてください"); return; }
        land.getMembers().remove(p.getUniqueId());
        lm().save();
        msg(p, "§e土地「" + land.getName() + "」から脱退しました");
        broadcast(land, "§e" + p.getName() + " が土地から脱退しました");
    }

    public static void kick(Player p, Land land, UUID target) {
        if (!requireManage(p, land)) return;
        Role r = land.getRole(target);
        if (r == null) return;
        if (r == Role.OWNER || (r == Role.TRUSTED && !land.getOwner().equals(p.getUniqueId()))) {
            msg(p, "§cそのメンバーを追放する権限がありません"); return;
        }
        land.getMembers().remove(target);
        lm().save();
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        msg(p, "§e" + op.getName() + " を追放しました");
        if (op.getPlayer() != null) msg(op.getPlayer(), "§c土地「" + land.getName() + "」から追放されました");
    }

    public static void setRole(Player p, Land land, UUID target, Role role) {
        if (!requireOwner(p, land)) return;
        if (role == Role.OWNER) {
            land.transferOwnership(target);
            broadcast(land, "§6" + Bukkit.getOfflinePlayer(target).getName() + " が新しいオーナーになりました");
        } else {
            land.getMembers().put(target, role);
            msg(p, "§a" + Bukkit.getOfflinePlayer(target).getName() + " の役職を " + role.display + " に変更しました");
        }
        lm().save();
    }

    public static void toggleBan(Player p, Land land, UUID target) {
        if (!requireManage(p, land)) return;
        if (land.isMember(target)) { msg(p, "§cメンバーは訪問禁止にできません"); return; }
        String name = Bukkit.getOfflinePlayer(target).getName();
        if (land.getBanned().remove(target)) {
            msg(p, "§a" + name + " の訪問禁止を解除しました");
        } else {
            land.getBanned().add(target);
            msg(p, "§c" + name + " を訪問禁止にしました");
            Player t = Bukkit.getPlayer(target);
            if (t != null && lm().getLandAt(t.getLocation()) == land) {
                t.teleport(t.getWorld().getSpawnLocation());
                msg(t, "§cあなたは土地「" + land.getName() + "」から追い出されました");
            }
        }
        lm().save();
    }

    public static void toggleFlag(Player p, Land land, Flag flag) {
        if (!requireManage(p, land)) return;
        land.setFlag(flag, !land.getFlag(flag));
        lm().save();
    }

    public static void setSpawn(Player p) {
        Land land = requireLand(p);
        if (land == null || !requireManage(p, land)) return;
        if (lm().getLandAt(p.getLocation()) != land) { msg(p, "§cスポーン地点は自分の土地の中に設定してください"); return; }
        land.setSpawn(p.getLocation());
        lm().save();
        msg(p, "§aスポーン地点を設定しました");
    }

    public static void teleport(Player p, Land land) {
        if (land.getSpawn() == null) { msg(p, "§cこの土地にはスポーン地点がありません"); return; }
        if (land.getBanned().contains(p.getUniqueId())) { msg(p, "§cあなたはこの土地への訪問を禁止されています"); return; }
        p.closeInventory();
        p.teleport(land.getSpawn());
        msg(p, "§a「" + land.getName() + "」へテレポートしました");
    }

    public static void rename(Player p, String name) {
        Land land = requireLand(p);
        if (land == null || !requireOwner(p, land)) return;
        int max = LandsPlugin.get().getConfig().getInt("max-name-length", 16);
        if (!name.matches("[\\p{L}\\p{N}_-]+") || name.length() > max) {
            msg(p, "§c土地名は" + max + "文字以内の英数字・日本語・_-で入力してください"); return;
        }
        if (lm().getLand(name) != null) { msg(p, "§cその名前の土地は既に存在します"); return; }
        lm().rename(land, name);
        msg(p, "§a土地名を「" + name + "」に変更しました");
    }

    public static void delete(Player p, Land land) {
        if (!land.getOwner().equals(p.getUniqueId()) && !p.hasPermission("lands.admin")) {
            msg(p, "§cこの操作はオーナーのみ可能です"); return;
        }
        broadcast(land, "§c土地「" + land.getName() + "」は削除されました");
        if (land.getBank() > 0) {
            eco().deposit(Bukkit.getOfflinePlayer(land.getOwner()), land.getBank());
            broadcast(land, "§7銀行残高 " + eco().format(land.getBank()) + " はオーナーに返金されました");
        }
        lm().delete(land);
    }

    public static void deposit(Player p, Land land, double amount) {
        if (!eco().enabled()) { msg(p, "§c経済プラグインが導入されていません"); return; }
        if (amount <= 0) { msg(p, "§c正の金額を入力してください"); return; }
        if (!eco().withdraw(p, amount)) { msg(p, "§c所持金が足りません"); return; }
        land.setBank(land.getBank() + amount);
        lm().save();
        msg(p, "§a" + eco().format(amount) + " を入金しました §7(残高: " + eco().format(land.getBank()) + ")");
    }

    public static void withdraw(Player p, Land land, double amount) {
        if (!eco().enabled()) { msg(p, "§c経済プラグインが導入されていません"); return; }
        if (!requireManage(p, land)) return;
        if (amount <= 0) { msg(p, "§c正の金額を入力してください"); return; }
        if (land.getBank() < amount) { msg(p, "§c銀行残高が足りません"); return; }
        land.setBank(land.getBank() - amount);
        eco().deposit(p, amount);
        lm().save();
        msg(p, "§a" + eco().format(amount) + " を引き出しました §7(残高: " + eco().format(land.getBank()) + ")");
    }

    public static void broadcast(Land land, String m) {
        for (UUID u : land.getMembers().keySet()) {
            Player pl = Bukkit.getPlayer(u);
            if (pl != null) msg(pl, m);
        }
    }
}
