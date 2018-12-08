package com.ruinscraft.playerstatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public final class PlayerStatusAPI implements PluginMessageListener, AutoCloseable {

	public static final long ONLINE 						= 0L;
	public static final long OFFLINE 						= -1L;
	public static final long VANISHED 						= -2L;
	private static final long REFRESH_LIST_PERIOD_TICKS 	= 50L;
	private static final String MESSAGING_CHANNEL 			= "RedisBungee";

	private Multimap<String, String> listCache;
	private BlockingQueue<Map.Entry<String, Long>> playerStatusQueue;
	private BlockingQueue<Multimap<String, String>> onlineListQueue;

	public PlayerStatusAPI() {
		this.playerStatusQueue = new LinkedBlockingQueue<>();
		this.onlineListQueue = new LinkedBlockingQueue<>();

		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			if (!PlayerStatusPlugin.getInstance().isEnabled()) {
				return;
			}

			try {
				Multimap<String, String> temp = getOnlineForce().call();
				Multimap<String, String> nonVanished = HashMultimap.create();
				List<String> vanishedPlayers = PlayerStatusPlugin.getInstance().getPlayerStorage().getVanished().call();

				for (String server : temp.keySet()) {
					for (String player : temp.get(server)) {
						if (vanishedPlayers.contains(player)) {
							continue;
						}

						nonVanished.put(server, player);
					}
				}

				listCache = nonVanished;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 20L, REFRESH_LIST_PERIOD_TICKS);
	}

	public Callable<Long> getPlayerStatus(String username) {
		return new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				if (username == null) {
					return OFFLINE;
				}

				if (isVanished(username).call()) {
					return VANISHED;
				}

				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("LastOnline");
				out.writeUTF(username);

				Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

				if (player.isPresent()) {
					player.get().sendPluginMessage(PlayerStatusPlugin.getInstance(), MESSAGING_CHANNEL, out.toByteArray());

					Map.Entry<String, Long> status = playerStatusQueue.take();

					while (!status.getKey().equalsIgnoreCase(username)) {
						/* Put it back */
						playerStatusQueue.offer(status);

						if (playerStatusQueue.peek() != status) {
							/* Get the next */
							status = playerStatusQueue.take();
						}
					}

					return status.getValue();
				}

				return OFFLINE;
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
					player.get().sendPluginMessage(PlayerStatusPlugin.getInstance(), MESSAGING_CHANNEL, out.toByteArray());

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
		return PlayerStatusPlugin.getInstance().getPlayerStorage().setVanished(username, vanished);
	}

	public Callable<Boolean> isVanished(String username) {
		return PlayerStatusPlugin.getInstance().getPlayerStorage().isVanished(username);
	}

	public Callable<List<String>> getVanished() {
		return PlayerStatusPlugin.getInstance().getPlayerStorage().getVanished();
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

			playerStatusQueue.offer(Maps.immutableEntry(argument, online));

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

			onlineListQueue.offer(allPlayers);

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
