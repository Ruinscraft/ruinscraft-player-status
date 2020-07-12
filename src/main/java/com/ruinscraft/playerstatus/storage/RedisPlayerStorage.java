package com.ruinscraft.playerstatus.storage;

import org.bukkit.configuration.ConfigurationSection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class RedisPlayerStorage implements PlayerStorage {

    private static final String VANISHED = "vanished";
    private static final String GROUP = "%s.group";

    private JedisPool pool;

    public RedisPlayerStorage(ConfigurationSection redisSection) {
        String address = redisSection.getString("address");
        int port = redisSection.getInt("port");
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(32);
        config.setMaxTotal(64);
        pool = new JedisPool(config, address, port == 0 ? Protocol.DEFAULT_PORT : port);
    }

    @Override
    public Callable<Void> setVanished(String username, boolean vanished) {
        return () -> {
            try (Jedis jedis = pool.getResource()) {
                if (vanished) {
                    jedis.sadd(VANISHED, username);
                } else {
                    jedis.srem(VANISHED, username);
                }
            }
            return null;
        };
    }

    @Override
    public Callable<Boolean> isVanished(String username) {
        return () -> {
            try (Jedis jedis = pool.getResource()) {
                return jedis.sismember(VANISHED, username);
            }
        };
    }

    @Override
    public Callable<List<String>> getVanished() {
        return () -> {
            List<String> vanished = new ArrayList<>();

            try (Jedis jedis = pool.getResource()) {
                vanished.addAll(jedis.smembers(VANISHED));
            }

            return vanished;
        };
    }

    @Override
    public Callable<Void> setGroup(String username, String group) {
        return () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.set(String.format(GROUP, username), group);
            }
            return null;
        };
    }

    @Override
    public Callable<String> getGroup(String username) {
        return () -> {
            try (Jedis jedis = pool.getResource()) {
                return jedis.get(String.format(GROUP, username));
            }
        };
    }

    @Override
    public void close() {
        if (!pool.isClosed()) {
            pool.close();
        }
    }

}
