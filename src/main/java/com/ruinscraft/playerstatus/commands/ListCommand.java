package com.ruinscraft.playerstatus.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.google.common.collect.Multimap;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;

public class ListCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		/* Running async because the vault permissions lookup may require DB access by LuckPerms, etc */
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			/* This is actually run sync, it is always cached */
			Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();

			for (String server : players.keySet()) {
				if (!sender.hasPermission("server." + server)) {
					continue;
				}

				sender.sendMessage(server + ":");

				for (String player : players.get(server)) {
					boolean vip = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.vip");
					boolean builder = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.builder");
					boolean helper = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.helper");
					boolean mod = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.mod");
					boolean admin = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.admin");
					boolean owner = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.owner");

					if (owner) {
						player = ChatColor.DARK_RED + player;
					}

					else if (admin) {
						player = ChatColor.GOLD + player;
					}

					else if (mod) {
						player = ChatColor.BLUE + player;
					}

					else if (helper) {
						player = ChatColor.AQUA + player;
					}
					
					else if (builder) {
						player = ChatColor.GREEN + player;
					}
					
					else if (vip) {
						player = ChatColor.DARK_PURPLE + player;
					}

					sender.sendMessage(player);
				}
			}
		});

		return true;
	}

}
