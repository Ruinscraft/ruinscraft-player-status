package com.ruinscraft.playerstatus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.google.common.collect.Multimap;

public class ListCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();

		for (String server : players.keySet()) {
			sender.sendMessage(server + ":");

			for (String player : players.get(server)) {
				sender.sendMessage(player);
			}
		}

		return true;
	}

}
