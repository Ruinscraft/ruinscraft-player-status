package com.ruinscraft.playerstatus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		PlayerStatusPlugin.getAPI().getOnline();
		
		return true;
	}

}
