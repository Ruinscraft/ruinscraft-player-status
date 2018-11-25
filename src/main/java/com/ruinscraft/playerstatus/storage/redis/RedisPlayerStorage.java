package com.ruinscraft.playerstatus.storage.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.bukkit.configuration.ConfigurationSection;

import com.ruinscraft.playerstatus.storage.PlayerStorage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisPlayerStorage implements PlayerStorage {

	private static final String VANISHED = "vanished";
	
	private JedisPool pool;
	
	public RedisPlayerStorage(ConfigurationSection redisSection) {
		String address = redisSection.getString("address");
		int port = redisSection.getInt("port");
		String password = redisSection.getString("password");

		pool = new JedisPool(
				new JedisPoolConfig(),
				address,
				port == 0 ? Protocol.DEFAULT_PORT : port,
						Protocol.DEFAULT_TIMEOUT,
						password);
	}
	
	@Override
	public Callable<Void> setVanished(String username, boolean vanished) {
		return new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try (Jedis jedis = pool.getResource()) {
					if (vanished) {
						jedis.sadd(VANISHED, username);
					} else {
						jedis.srem(VANISHED, username);
					}
				}
				return null;
			}
		};
	}

	@Override
	public Callable<Boolean> isVanished(String username) {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try (Jedis jedis = pool.getResource()) {
					return jedis.sismember(VANISHED, username);
				}
			}
		};
	}
	
	@Override
	public Callable<List<String>> getVanished() {
		return new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				List<String> vanished = new ArrayList<>();
				
				try (Jedis jedis = pool.getResource()) {
					vanished.addAll(jedis.smembers(VANISHED));
				}
				
				return vanished;
			}
		};
	}
	
	@Override
	public void close() {
		pool.close();
	}
	
}
