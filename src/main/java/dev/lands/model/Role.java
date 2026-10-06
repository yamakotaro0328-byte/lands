package dev.lands.model;

import org.bukkit.Material;

public enum Role {
    MEMBER("メンバー", Material.IRON_INGOT, "建築・アイテム使用が可能"),
    TRUSTED("信頼メンバー", Material.GOLD_INGOT, "メンバー権限 + 招待・チャンク保護が可能"),
    OWNER("オーナー", Material.DIAMOND, "全ての操作が可能");

    public final String display;
    public final Material icon;
    public final String description;

    Role(String display, Material icon, String description) {
        this.display = display;
        this.icon = icon;
        this.description = description;
    }

    public boolean canManage() {
        return this == TRUSTED || this == OWNER;
    }
}
