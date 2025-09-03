package dev.aari.playerreset;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ResetGUIHolder implements InventoryHolder {
    private final String targetPlayer;

    public ResetGUIHolder(String targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }
}
