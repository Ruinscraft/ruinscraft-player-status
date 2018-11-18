package com.ruinscraft.playerstatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListCommand implements CommandExecutor {

	private static final int MAX_CACHE_TIME_SECONDS = 3;

	private static List<String> cache;
	private long cacheTime;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean fetch = false;

		/* If cache older than some time */
		if ((System.currentTimeMillis() - cacheTime) > TimeUnit.SECONDS.toMillis(MAX_CACHE_TIME_SECONDS)) {
			fetch = true;
		}

		if (fetch) {
			PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
					try {
						cache = PlayerStatusPlugin.getAPI().getOnline();
					} catch (Exception e) {
						sender.sendMessage("No online player to send plugin message");
						return;
					}
					cacheTime = System.currentTimeMillis();
					send(sender);
			});

			return true;
		}

		send(sender);

		return true;
	}

	private static void send(CommandSender sender) {
		String listString = String.join(", ", cache);
		sender.sendMessage(listString);
	}

}
