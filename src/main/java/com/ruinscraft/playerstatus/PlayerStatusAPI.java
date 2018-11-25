package com.ruinscraft.playerstatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public final class PlayerStatusAPI implements PluginMessageListener, AutoCloseable {

	private static final String MESSAGING_CHANNEL = "RedisBungee";
	private static final long REFRESH_PERIOD_TICKS = 50L;
	private static final PlayerStatusPlugin plugin = PlayerStatusPlugin.getInstance();

	/* Multimap<String, String> <=> Map<String, List<String>>*/
	private Multimap<String, String> listCache;
	private BlockingQueue<PlayerStatus> playerStatusQueue;
	private BlockingQueue<Multimap<String, String>> onlineListQueue;

	public PlayerStatusAPI() {
		this.playerStatusQueue = new ArrayBlockingQueue<>(64);
		this.onlineListQueue = new ArrayBlockingQueue<>(64);

		BukkitScheduler sch = plugin.getServer().getScheduler();
		
		sch.runTaskTimerAsynchronously(plugin, () -> {
			try {
				listCache = getOnlineForce().call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 5L, REFRESH_PERIOD_TICKS);
	}

	public Callable<PlayerStatus> getPlayerStatus(String username) {
		return new Callable<PlayerStatus>() {
			@Override
			public PlayerStatus call() throws Exception {
				if (username == null) {
					return new InvalidPlayerStatus();
				}
				
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("LastOnline");
				out.writeUTF(username);

				Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

				if (player.isPresent()) {
					player.get().sendPluginMessage(plugin, MESSAGING_CHANNEL, out.toByteArray());

					try {
						return playerStatusQueue.take();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				return new InvalidPlayerStatus();
			}
		};
	}

	public Callable<Multimap<String, String>> getOnlineForce() {
		return new Callable<Multimap<String, String>>() {
			@Override
			public Multimap<String, String> call() throws Exception {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("ServerPlayers");
				out.writeUTF("PLAYERS");

				Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

				if (player.isPresent()) {
					player.get().sendPluginMessage(plugin, MESSAGING_CHANNEL, out.toByteArray());

					try {
						return onlineListQueue.take();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				return listCache == null ? listCache = HashMultimap.create() : listCache;
			}
		};
	}

	public Multimap<String, String> getOnline() {
		return listCache;
	}
	
	public List<String> getOnlyPlayers() {
		List<String> players = new ArrayList<>();
		
		Multimap<String, String> playersWithServers = getOnline();
		
		for (String server : playersWithServers.keySet()) {
			players.addAll(playersWithServers.get(server));
		}

		return players;
	}
	
	public Callable<Void> setVanished(String username, boolean vanished) {
		return plugin.getPlayerStorage().setVanished(username, vanished);
	}
	
	public Callable<Boolean> isVanished(String username) {
		return plugin.getPlayerStorage().isVanished(username);
	}

	public Callable<List<String>> getVanished() {
		return plugin.getPlayerStorage().getVanished();
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals(MESSAGING_CHANNEL)) {
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
			Multimap<String, String> allPlayers = HashMultimap.create();
			
			int serverCount = in.readInt();

			do {
				String serverName = in.readUTF();
				int serverPlayerCount = in.readInt();

				Collection<String> serverPlayers = new ArrayList<>();
				
				for (int i = 0; i < serverPlayerCount; i++) {
					String playerName = in.readUTF();
					serverPlayers.add(playerName);
				}

				allPlayers.putAll(serverName, serverPlayers);
				
			} while (--serverCount > 0);
			
			try {
				onlineListQueue.put(allPlayers);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			break;
		}
	}

	@Override
	public void close() {
		listCache.clear();
		playerStatusQueue.clear();
		onlineListQueue.clear();
	}

}
