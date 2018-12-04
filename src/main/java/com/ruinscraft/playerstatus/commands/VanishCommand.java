package com.ruinscraft.playerstatus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.ruinscraft.playerstatus.Constants;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;

public class VanishCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}

		Player player = (Player) sender;

		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			try {
				
				boolean vanished = PlayerStatusPlugin.getInstance().getAPI().isVanished(player.getName()).call();
				
				if (!player.isOnline()) {
					return;
				}
				
				if (vanished) {
					PlayerStatusPlugin.getInstance().getAPI().setVanished(player.getName(), false).call();
					player.sendMessage(Constants.COLOR_BASE + "Unvanished");
				} else {
					PlayerStatusPlugin.getInstance().getAPI().setVanished(player.getName(), true).call();
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
		
		if (vanished) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
		} else {
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
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
