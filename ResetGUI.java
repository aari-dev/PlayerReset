package dev.aari.playerreset;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResetGUI {
    private final String targetPlayer;
    private final PlayerReset plugin;

    public ResetGUI(String targetPlayer) {
        this.targetPlayer = targetPlayer;
        this.plugin = PlayerReset.getInstance();
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "Reset GUI"));
        int size = plugin.getConfig().getInt("gui.size", 27);

        Inventory inventory = Bukkit.createInventory(new ResetGUIHolder(targetPlayer), size, title);

        loadGUIItems(inventory).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
        });
    }

    private CompletableFuture<Void> loadGUIItems(Inventory inventory) {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemsSection == null) return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
            if (itemConfig == null) continue;

            int slot = itemConfig.getInt("slot", 0);
            String materialName = itemConfig.getString("material", "STONE");
            String name = ChatColor.translateAlternateColorCodes('&', itemConfig.getString("name", ""));
            List<String> lore = itemConfig.getStringList("lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());

            Material material = Material.valueOf(materialName);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            if (material == Material.PLAYER_HEAD) {
                String skullOwner = itemConfig.getString("skull_owner", "");
                if (!skullOwner.isEmpty()) {
                    future = future.thenCompose(v -> createPlayerSkull(skullOwner, name, lore)
                            .thenAccept(skull -> inventory.setItem(slot, skull)));
                } else {
                    inventory.setItem(slot, item);
                }
            } else {
                inventory.setItem(slot, item);
            }
        }

        return future;
    }

    private CompletableFuture<ItemStack> createPlayerSkull(String playerName, String displayName, List<String> lore) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();

                PlayerProfile profile = Bukkit.createProfile(playerName);
                profile.complete();

                meta.setPlayerProfile(profile);
                meta.setDisplayName(displayName);
                meta.setLore(lore);
                skull.setItemMeta(meta);

                return skull;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create skull for " + playerName + ": " + e.getMessage());
                ItemStack fallback = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = fallback.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    meta.setLore(lore);
                    fallback.setItemMeta(meta);
                }
                return fallback;
            }
        });
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }
}
