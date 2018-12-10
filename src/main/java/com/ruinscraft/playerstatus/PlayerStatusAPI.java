package com.ruinscraft.playerstatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public final class PlayerStatusAPI implements PluginMessageListener, AutoCloseable {

	private static final long REFRESH_LIST_PERIOD_TICKS 	= 50L;
	private static final String MESSAGING_CHANNEL 			= "RedisBungee";

	private Multimap<String, String> listCache;
	private Map<String, PlayerStatus> playerStatusCache;
	//private BlockingQueue<Map.Entry<String, Long>> playerStatusQueue;
	private BlockingQueue<Multimap<String, String>> onlineListQueue;

	public PlayerStatusAPI() {
		this.listCache = HashMultimap.create();
		this.playerStatusCache = new ConcurrentHashMap<>();
		this.onlineListQueue = new ArrayBlockingQueue<>(32);

		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			if (PlayerStatusPlugin.getInstance() == null) {
				return;
			}

			if (!PlayerStatusPlugin.getInstance().isEnabled()) {
				return;
			}

			if (Bukkit.getOnlinePlayers().size() < 1) {
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
					return PlayerStatus.OFFLINE;
				}

				if (isVanished(username).call()) {
					return PlayerStatus.VANISHED;
				}

				ByteArrayDataOutput out = ByteStreams.newDataOutput();

				out.writeUTF("LastOnline");
				out.writeUTF(username);

				Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

				if (player.isPresent()) {
					player.get().sendPluginMessage(PlayerStatusPlugin.getInstance(), MESSAGING_CHANNEL, out.toByteArray());

					PlayerStatus playerStatus = new PlayerStatus();

					playerStatusCache.put(username, playerStatus);

					synchronized (playerStatus) {
						playerStatus.wait();
					}

					return playerStatus.getValue();
				}

				return PlayerStatus.OFFLINE;
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

	public Callable<Void> setVanished(String username, boolean vanished) {
		return PlayerStatusPlugin.getInstance().getPlayerStorage().setVanished(username, vanished);
	}

	public Callable<Boolean> isVanished(String username) {
		return PlayerStatusPlugin.getInstance().getPlayerStorage().isVanished(username);
	}

	public Callable<List<String>> getVanished() {
		return PlayerStatusPlugin.getInstance().getPlayerStorage().getVanished();
	}

	public Multimap<String, String> getOnline() {
		return listCache;
	}

	public List<String> getOnlyPlayers() {
		return Lists.newArrayList(getOnline().values());
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
			PlayerStatus playerStatus = playerStatusCache.get(argument);

			playerStatus.setValue(in.readLong());

			synchronized (playerStatus) {
				playerStatus.notify();
			}

			break;
		case "ServerPlayers":
			Multimap<String, String> list = HashMultimap.create();

			int serverCount = in.readInt();

			do {
				String serverName = in.readUTF();
				int serverPlayerCount = in.readInt();

				Collection<String> serverPlayers = new ArrayList<>();

				for (int i = 0; i < serverPlayerCount; i++) {
					String playerName = in.readUTF();
					serverPlayers.add(playerName);
				}

				list.putAll(serverName, serverPlayers);
			} while (--serverCount > 0);

			onlineListQueue.offer(list);

			break;
		}
	}

	@Override
	public void close() {
		listCache.clear();
		playerStatusCache.clear();
		onlineListQueue.clear();
	}

}
