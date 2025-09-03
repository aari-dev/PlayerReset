package dev.aari.playerreset;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("playerreset.use")) {
            player.sendMessage(PlayerReset.getInstance().getConfig().getString("messages.no_permission", "&cNo permission!"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("&cUsage: /reset <player|*|**>");
            return true;
        }

        String target = args[0];

        if (target.equals("*")) {
            if (!player.hasPermission("playerreset.admin")) {
                player.sendMessage(PlayerReset.getInstance().getConfig().getString("messages.no_permission", "&cNo permission!"));
                return true;
            }
            openGUI(player, "*");
            return true;
        }

        if (target.equals("**")) {
            if (!player.hasPermission("playerreset.admin")) {
                player.sendMessage(PlayerReset.getInstance().getConfig().getString("messages.no_permission", "&cNo permission!"));
                return true;
            }
            openGUI(player, "**");
            return true;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            player.sendMessage(PlayerReset.getInstance().getConfig().getString("messages.player_not_found", "&cPlayer not found!"));
            return true;
        }

        openGUI(player, targetPlayer.getName());
        return true;
    }

    private void openGUI(Player player, String target) {
        ResetGUI gui = new ResetGUI(target);
        gui.open(player);
    }
}
