package dev.aari.playerreset;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class GUIListener implements Listener {
    private final PlayerReset plugin;

    public GUIListener() {
        this.plugin = PlayerReset.getInstance();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ResetGUIHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        String targetPlayer = holder.getTargetPlayer();

        switch (slot) {
            case 11 -> handleReset(player, targetPlayer, "kills");
            case 12 -> handleReset(player, targetPlayer, "deaths");
            case 13 -> handleReset(player, targetPlayer, "playtime");
            case 14 -> handleReset(player, targetPlayer, "money");
            case 15 -> handleReset(player, targetPlayer, "all");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ResetGUIHolder) {
            event.setCancelled(true);
        }
    }

    private void handleReset(Player executor, String target, String resetType) {
        executor.closeInventory();

        if (target.equals("*")) {
            resetOnlinePlayers(executor, resetType);
        } else if (target.equals("**")) {
            resetAllPlayers(executor, resetType);
        } else {
            resetSinglePlayer(executor, target, resetType);
        }
    }

    private void resetSinglePlayer(Player executor, String targetName, String resetType) {
        CompletableFuture.supplyAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                return null;
            }
            return target;
        }).thenAccept(target -> {
            if (target == null) {
                executor.sendMessage(plugin.getConfig().getString("messages.player_not_found", "&cPlayer not found!"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                performReset(target, resetType);
                plugin.getDatabaseManager().savePlayerReset(target, resetType);

                String message = plugin.getConfig().getString("messages.reset_success", "&aReset successful!")
                        .replace("{type}", resetType)
                        .replace("{player}", target.getName());
                executor.sendMessage(message);
            });
        });
    }

    private void resetOnlinePlayers(Player executor, String resetType) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        CompletableFuture.runAsync(() -> {
            for (Player player : players) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    performReset(player, resetType);
                    plugin.getDatabaseManager().savePlayerReset(player, resetType);
                });
            }
        }).thenRun(() -> {
            String message = plugin.getConfig().getString("messages.bulk_reset_success", "&aBulk reset successful!")
                    .replace("{type}", resetType)
                    .replace("{count}", String.valueOf(players.size()));
            executor.sendMessage(message);
        });
    }

    private void resetAllPlayers(Player executor, String resetType) {
        CompletableFuture.runAsync(() -> {
            OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();

            for (OfflinePlayer player : allPlayers) {
                if (player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        performReset(player, resetType);
                        plugin.getDatabaseManager().savePlayerReset(player, resetType);
                    });
                } else {
                    plugin.getDatabaseManager().savePlayerReset(player, resetType);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = plugin.getConfig().getString("messages.bulk_reset_success", "&aBulk reset successful!")
                        .replace("{type}", resetType)
                        .replace("{count}", String.valueOf(allPlayers.length));
                executor.sendMessage(message);
            });
        });
    }

    private void performReset(OfflinePlayer target, String resetType) {
        if (!target.isOnline()) return;

        Player player = target.getPlayer();
        if (player == null) return;

        switch (resetType.toLowerCase()) {
            case "kills" -> player.setStatistic(Statistic.PLAYER_KILLS, 0);
            case "deaths" -> player.setStatistic(Statistic.DEATHS, 0);
            case "playtime" -> {
                player.setStatistic(Statistic.PLAY_ONE_MINUTE, 0);
                player.setStatistic(Statistic.TIME_SINCE_DEATH, 0);
                player.setStatistic(Statistic.TIME_SINCE_REST, 0);
            }
            case "money" -> {
                Economy economy = plugin.getEconomy();
                if (economy != null) {
                    double balance = economy.getBalance(player);
                    if (balance > 0) {
                        economy.withdrawPlayer(player, balance);
                    }
                }
            }
            case "all" -> {
                player.setStatistic(Statistic.PLAYER_KILLS, 0);
                player.setStatistic(Statistic.DEATHS, 0);
                player.setStatistic(Statistic.PLAY_ONE_MINUTE, 0);
                player.setStatistic(Statistic.TIME_SINCE_DEATH, 0);
                player.setStatistic(Statistic.TIME_SINCE_REST, 0);

                Economy economy = plugin.getEconomy();
                if (economy != null) {
                    double balance = economy.getBalance(player);
                    if (balance > 0) {
                        economy.withdrawPlayer(player, balance);
                    }
                }
            }
        }
    }

    private String getFormattedMessage(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String message;
        if (path.startsWith("messages.")) {
            message = plugin.getConfig().getString(path, "");
        } else {
            message = path;
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
}
