package com.ruinscraft.playerstatus;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public final class PlayerStatusAPI implements PluginMessageListener {

	private static final long REFRESH_PERIOD_TICKS = 30L;

	private final JavaPlugin plugin;

	private Multimap<String, String> listCache;
	private BlockingQueue<PlayerStatus> playerStatusQueue;
	private BlockingQueue<Multimap<String, String>> onlineListQueue;

	public PlayerStatusAPI(JavaPlugin plugin) {
		this.plugin = plugin;
		playerStatusQueue = new ArrayBlockingQueue<>(64);
		onlineListQueue = new ArrayBlockingQueue<>(64);

		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			try {
				listCache = getOnlineForce();
			} catch (Exception e) {
				return; // no players online to send the plugin message
			}
		}, 5L, REFRESH_PERIOD_TICKS);
	}

	public void cleanup() {
		playerStatusQueue.clear();
		onlineListQueue.clear();
	}

	public PlayerStatus getPlayerStatus(String username) throws Exception {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("LastOnline");
		out.writeUTF(username);

		Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

		if (player.isPresent()) {
			player.get().sendPluginMessage(plugin, "RedisBungee", out.toByteArray());
			return playerStatusQueue.take();
		}

		throw new Exception("No player to send plugin message");
	}

	public Multimap<String, String> getOnline() {
		return listCache == null ? listCache = HashMultimap.create() : listCache;
	}

	public synchronized Multimap<String, String> getOnlineForce() throws Exception {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("ServerPlayers");
		out.writeUTF("PLAYERS");

		Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

		if (player.isPresent()) {
			player.get().sendPluginMessage(plugin, "RedisBungee", out.toByteArray());
			/* Blocking (wait for the message to come back from the proxy) */
			return onlineListQueue.take();
		}

		throw new Exception("No player to send plugin message");
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("RedisBungee")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(message);

		String command = in.readUTF();
		String argument = in.readUTF();

		switch (command) {
		case "LastOnline":
			/*	
			 * 	https://github.com/minecrafter/RedisBungee/wiki/API#plugin-messaging-bukkit
			 * 
			 * 	-1		never joined 
			 *	0		currently online
			 * 	+Z		last seen epoch time
			 */
			long online = in.readLong();

			try {
				playerStatusQueue.put(new PlayerStatus(argument, online));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			break;
		case "ServerPlayers":
			/*
			 * 	https://github.com/minecrafter/RedisBungee/wiki/API#plugin-messaging-bukkit
			 * 
			 * 	CSV of player names, no spaces
			 * 	Eg: "Notch,Dinnerbone,md_5"
			 */

			// TODO: figure out how to deserialize a multimap

			String csv = in.readUTF();

			//onlineListQueue.put(Arrays.asList(csv.split(",")));

			break;
		}
	}

}
