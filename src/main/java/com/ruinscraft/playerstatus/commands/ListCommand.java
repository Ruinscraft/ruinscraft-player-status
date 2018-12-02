package com.ruinscraft.playerstatus.commands;

import java.util.ArrayList;
import java.util.List;

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

			List<String> foundStaff = new ArrayList<>();
			
			for (String server : players.keySet()) {
				if (!sender.hasPermission("server." + server)) {
					continue;
				}

				String serverName = ChatColor.GOLD + server + ChatColor.YELLOW + " (" + players.get(server).size() + ")" + ChatColor.GOLD + ": ";
				List<String> serverPlayers = new ArrayList<>();
				
				int maxPlayers = 25;
				
				for (String player : players.get(server)) {
					if (maxPlayers-- == 1) {
						serverPlayers.add(ChatColor.GRAY + " and " + (players.get(server).size() - 25) + " more");
						break;
					}
					
					boolean vip = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.vip");
					boolean builder = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.builder");
					boolean helper = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.helper");
					boolean mod = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.mod");
					boolean admin = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.admin");
					boolean owner = PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.owner");

					if (owner) {
						serverPlayers.add(ChatColor.DARK_RED + player);
						foundStaff.add(ChatColor.DARK_RED + player);
					}

					else if (admin) {
						serverPlayers.add(ChatColor.GOLD + player);
						foundStaff.add(ChatColor.GOLD + player);
					}

					else if (mod) {
						serverPlayers.add(ChatColor.BLUE + player);
						foundStaff.add(ChatColor.BLUE + player);
					}

					else if (helper) {
						serverPlayers.add(ChatColor.AQUA + player);
						foundStaff.add(ChatColor.AQUA + player);
					}
					
					else if (builder) {
						serverPlayers.add(ChatColor.GREEN + player);
					}
					
					else if (vip) {
						serverPlayers.add(ChatColor.DARK_PURPLE + player);
					} 
					
					else {
						serverPlayers.add(ChatColor.GRAY + player);
					}
				}
				
				sender.sendMessage(serverName + String.join(", ", serverPlayers));
			}
			
			sender.sendMessage(ChatColor.GOLD + "Staff online " + ChatColor.YELLOW + "(" + foundStaff.size() + ")" + ChatColor.GOLD + ": " + String.join(", ", foundStaff));
			sender.sendMessage(ChatColor.GOLD + "Total players online: " + ChatColor.YELLOW + PlayerStatusPlugin.getAPI().getOnlyPlayers().size());
		});

		return true;
	}

}
