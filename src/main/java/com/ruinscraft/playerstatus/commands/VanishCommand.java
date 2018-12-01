package com.ruinscraft.playerstatus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ruinscraft.playerstatus.Constants;
import com.ruinscraft.playerstatus.PlayerStatusAPI;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;

public class VanishCommand implements CommandExecutor {

	private static PlayerStatusAPI api = PlayerStatusPlugin.getAPI();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}

		Player player = (Player) sender;

		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			try {
				
				boolean vanished = api.isVanished(player.getName()).call();
				
				if (!player.isOnline()) {
					return;
				}
				
				if (vanished) {
					api.setVanished(player.getName(), false).call();
					player.sendMessage(Constants.COLOR_BASE + "Unvanished");
				} else {
					api.setVanished(player.getName(), true).call();
					player.sendMessage(Constants.COLOR_BASE + "Vanished");
				}
				
				PlayerStatusPlugin.getInstance().getServer().getScheduler().runTask(PlayerStatusPlugin.getInstance(), () -> {
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
		
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (vanished) {
				onlinePlayer.hidePlayer(PlayerStatusPlugin.getInstance(), player);
			} else {
				onlinePlayer.showPlayer(PlayerStatusPlugin.getInstance(), player);
			}
		}
	}
	
}
