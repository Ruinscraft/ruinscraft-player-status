package com.ruinscraft.playerstatus;

import java.util.concurrent.Callable;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

	private static PlayerStatusAPI api = PlayerStatusPlugin.getAPI();

	// TODO: add permission and stuff in plugin.yml....
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}

		Player player = (Player) sender;

		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			Callable<Boolean> checkCall = api.isVanished(player.getName());

			try {
				if (checkCall.call()) {
					api.setVanished(player.getName(), false).call();

					player.sendMessage("Unvanished");
				} else {
					api.setVanished(player.getName(), true).call();

					player.sendMessage("Vanished");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return true;
	}

}
