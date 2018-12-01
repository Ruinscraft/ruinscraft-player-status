package com.ruinscraft.playerstatus;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			try {
				List<String> vanished = PlayerStatusPlugin.getAPI().getVanished().call();
				PlayerStatusPlugin.getInstance().getServer().getScheduler().runTask(PlayerStatusPlugin.getInstance(), () -> {
					handleVanished(vanished);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static void handleVanished(List<String> vanished) {
		for (String _vanished : vanished) {
			Player vanishedPlayer = Bukkit.getPlayer(_vanished);
			
			if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					if (onlinePlayer.equals(vanishedPlayer)) {
						continue;
					}
					onlinePlayer.hidePlayer(PlayerStatusPlugin.getInstance(), vanishedPlayer);
				}
			}
		}
	}

}
