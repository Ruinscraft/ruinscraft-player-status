package com.ruinscraft.playerstatus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        /* temporarily remove any invis effect */
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) && player.hasPermission("ruinscraft.command.vanish")) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        String group = PlayerStatusPlugin.getVaultPerms().getPrimaryGroup(player);

        PlayerStatusPlugin.get().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.get(), () -> {
            try {
                List<String> vanished = PlayerStatusPlugin.get().getAPI().getVanished().call();
                PlayerStatusPlugin.get().getServer().getScheduler().runTask(PlayerStatusPlugin.get(), () -> {
                    handleVanished(vanished);

                    try {
                        PlayerStatusPlugin.get().getPlayerStorage().setGroup(player.getName(), group).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void handleVanished(final List<String> vanished) {
        PlayerStatusPlugin.get().getServer().getScheduler().runTask(PlayerStatusPlugin.get(), () -> {
            for (Player vanishedPlayer : vanished.stream().map(t -> Bukkit.getPlayer(t)).collect(Collectors.toSet())) {
                if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.equals(vanishedPlayer)) {
                            vanishedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
                            continue;
                        }
                        onlinePlayer.hidePlayer(PlayerStatusPlugin.get(), vanishedPlayer);
                    }
                }
            }
        });
    }

}
