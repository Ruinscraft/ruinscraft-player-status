package com.ruinscraft.playerstatus.commands;

import java.util.concurrent.Callable;

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
			Callable<Boolean> checkCall = api.isVanished(player.getName());

			try {
				if (checkCall.call()) {
					api.setVanished(player.getName(), false).call();

					player.sendMessage(Constants.COLOR_BASE + "Unvanished");
				} else {
					api.setVanished(player.getName(), true).call();

					player.sendMessage(Constants.COLOR_BASE + "Vanished");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return true;
	}

}
