package com.ruinscraft.playerstatus.commands;

import com.ruinscraft.playerstatus.PlayerStatusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VanishCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        PlayerStatusPlugin.get().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.get(), () -> {
            try {

                boolean vanished = PlayerStatusPlugin.get().getAPI().isVanished(player.getName()).call();

                if (!player.isOnline()) {
                    return;
                }

                if (vanished) {
                    PlayerStatusPlugin.get().getAPI().setVanished(player.getName(), false).call();
                    player.sendMessage(ChatColor.GOLD + "Unvanished");
                } else {
                    PlayerStatusPlugin.get().getAPI().setVanished(player.getName(), true).call();
                    player.sendMessage(ChatColor.GOLD + "Vanished");
                }

                PlayerStatusPlugin.get().getServer().getScheduler().runTask(PlayerStatusPlugin.get(), () -> {
                    handleVanish(player, !vanished);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return true;
    }

    private static void handleVanish(Player player, boolean vanished) {
        if (!player.isOnline()) {
            return;
        }

        if (vanished) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (vanished) {
                onlinePlayer.hidePlayer(PlayerStatusPlugin.get(), player);
            } else {
                onlinePlayer.showPlayer(PlayerStatusPlugin.get(), player);
            }
        }
    }

}
