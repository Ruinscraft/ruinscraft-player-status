package com.ruinscraft.playerstatus;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class JoinListener implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		String group = PlayerStatusPlugin.getVaultPerms().getPrimaryGroup(event.getPlayer());
		
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			try {
				List<String> vanished = PlayerStatusPlugin.getInstance().getAPI().getVanished().call();
				PlayerStatusPlugin.getInstance().getServer().getScheduler().runTask(PlayerStatusPlugin.getInstance(), () -> {
					handleVanished(vanished);
					
					try {
						PlayerStatusPlugin.getInstance().getPlayerStorage().setGroup(event.getPlayer().getName(), group).call();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static void handleVanished(List<String> vanished) {
		for (Player vanishedPlayer : vanished.stream().map(t -> Bukkit.getPlayer(t)).collect(Collectors.toSet())) {
			if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					if (onlinePlayer.equals(vanishedPlayer)) {
						vanishedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
						continue;
					}
					onlinePlayer.hidePlayer(PlayerStatusPlugin.getInstance(), vanishedPlayer);
				}
			}
		}
	}

}
