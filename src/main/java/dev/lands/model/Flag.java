package dev.lands.model;

import org.bukkit.Material;

public enum Flag {
    VISITOR_BUILD("訪問者の建築・破壊", Material.GRASS_BLOCK, false),
    VISITOR_INTERACT("訪問者のドア・ボタン使用", Material.OAK_DOOR, false),
    VISITOR_CONTAINER("訪問者のチェスト使用", Material.CHEST, false),
    PVP("PvP", Material.IRON_SWORD, false),
    MONSTER_SPAWN("モンスターのスポーン", Material.ZOMBIE_HEAD, true),
    EXPLOSIONS("爆発による破壊", Material.TNT, false),
    FIRE_SPREAD("火の延焼", Material.FLINT_AND_STEEL, false),
    ANIMAL_DAMAGE("訪問者の動物への攻撃", Material.WHEAT, false);

    public final String display;
    public final Material icon;
    public final boolean def;

    Flag(String display, Material icon, boolean def) {
        this.display = display;
        this.icon = icon;
        this.def = def;
    }
}
