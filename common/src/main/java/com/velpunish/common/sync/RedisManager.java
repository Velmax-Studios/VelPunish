package com.velpunish.common.sync;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RedisManager {
    private final RedisConfig config;
    private JedisPool jedisPool;
    private final ExecutorService executor;
    private static final String CHANNEL = "velpunish:sync";
    private Consumer<String> messageHandler;
    private JedisPubSub pubSub;

    public RedisManager(RedisConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(2);
    }

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void connect() {
        if (!config.isEnabled())
            return;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleDuration(Duration.ofSeconds(60));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);

        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            jedisPool = new JedisPool(poolConfig, config.getHost(), config.getPort(), 2000, config.getPassword());
        } else {
            jedisPool = new JedisPool(poolConfig, config.getHost(), config.getPort(), 2000);
        }

        startSubscriber();
    }

    private void startSubscriber() {
        executor.submit(() -> {
            boolean broken = false;
            while (!executor.isShutdown()) {
                if (broken) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                try (Jedis jedis = jedisPool.getResource()) {
                    broken = false;
                    pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            if (CHANNEL.equals(channel) && messageHandler != null) {
                                messageHandler.accept(message);
                            }
                        }
                    };
                    jedis.subscribe(pubSub, CHANNEL);
                } catch (Exception e) {
                    broken = true;
                    e.printStackTrace();
                }
            }
        });
    }

    public void publishMessage(String message) {
        if (!config.isEnabled() || jedisPool == null)
            return;

        executor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(CHANNEL, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void close() {
        if (pubSub != null) {
            pubSub.unsubscribe();
        }
        executor.shutdown();
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
