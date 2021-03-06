package com.ruinscraft.playerstatus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;
import java.util.concurrent.*;

public final class PlayerStatusAPI implements PluginMessageListener, AutoCloseable {

    private static final long REFRESH_LIST_PERIOD_TICKS = 60L;

    private Multimap<String, String> listCache;
    private final Map<String, PlayerStatus> playerStatusCache;
    private final BlockingQueue<Multimap<String, String>> onlineListQueue;

    public PlayerStatusAPI() {
        this.listCache = HashMultimap.create();
        this.playerStatusCache = new ConcurrentHashMap<>();
        this.onlineListQueue = new ArrayBlockingQueue<>(8);

        PlayerStatusPlugin.get().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.get(), () -> {
            if (PlayerStatusPlugin.get() == null
                    || !PlayerStatusPlugin.get().isEnabled()
                    || Bukkit.getOnlinePlayers().size() < 1) {
                return;
            }

            try {
                Multimap<String, String> temp = getOnlineForce().call();
                Multimap<String, String> nonVanished = HashMultimap.create();
                List<String> vanishedPlayers = PlayerStatusPlugin.get().getPlayerStorage().getVanished().call();

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
        }, REFRESH_LIST_PERIOD_TICKS, REFRESH_LIST_PERIOD_TICKS);
    }

    public Callable<Long> getPlayerStatus(String username) {
        return () -> {
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

            /*
             * There has to be a player present to send a plugin message
             * https://wiki.vg/Plugin_channels
             */
            if (player.isPresent()) {
                player.get().sendPluginMessage(PlayerStatusPlugin.get(), PlayerStatusPlugin.getPluginChannel(), out.toByteArray());

                PlayerStatus status = new PlayerStatus();

                playerStatusCache.put(username, status);

                /*
                 * Wait for notify() in the plugin message listener
                 * Timeout after 3 seconds
                 */
                synchronized (status) {
                    status.wait(TimeUnit.SECONDS.toMillis(3));
                }

                /* Return the value set by the plugin message listener */
                return status.getValue();
            }

            /* If all else fails, return offline */
            return PlayerStatus.OFFLINE;
        };
    }

    public Callable<Multimap<String, String>> getOnlineForce() {
        return () -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ServerPlayers");
            out.writeUTF("PLAYERS");

            Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().findFirst();

            if (player.isPresent()) {
                player.get().sendPluginMessage(PlayerStatusPlugin.get(), PlayerStatusPlugin.getPluginChannel(), out.toByteArray());

                try {
                    return onlineListQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return listCache == null ? listCache = HashMultimap.create() : listCache;
        };
    }

    public Callable<Void> setVanished(String username, boolean vanished) {
        return PlayerStatusPlugin.get().getPlayerStorage().setVanished(username, vanished);
    }

    public Callable<Boolean> isVanished(String username) {
        return PlayerStatusPlugin.get().getPlayerStorage().isVanished(username);
    }

    public Callable<List<String>> getVanished() {
        return PlayerStatusPlugin.get().getPlayerStorage().getVanished();
    }

    public Multimap<String, String> getOnline() {
        return listCache;
    }

    public List<String> getOnlyPlayers() {
        return Lists.newArrayList(getOnline().values());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(PlayerStatusPlugin.getPluginChannel())) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);

        String command = in.readUTF();
        String argument = in.readUTF();

        switch (command) {
            case "LastOnline":
                /*
                 * https://github.com/minecrafter/RedisBungee/wiki/API#plugin-messaging-bukkit
                 *
                 * -1		never joined
                 * 0		currently online
                 * +Z		last seen epoch time
                 */
                PlayerStatus status = playerStatusCache.get(argument);

                status.setValue(in.readLong());

                /* Notify that the status has been set */
                synchronized (status) {
                    status.notify();
                }

                break;
            case "ServerPlayers":
                /*
                 * int : server count
                 * for n of server count
                 *		string : server name
                 *		int : server player count
                 *			for n of player count
                 *				string : player name
                 */
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
