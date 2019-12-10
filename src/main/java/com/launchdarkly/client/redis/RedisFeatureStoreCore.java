package com.launchdarkly.client.redis;

import com.launchdarkly.client.VersionedData;
import com.launchdarkly.client.VersionedDataKind;
import com.launchdarkly.client.utils.FeatureStoreCore;
import com.launchdarkly.client.utils.FeatureStoreHelpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * Internal implementation of the Redis feature store.
 * <p>
 * Implementation notes:
 * <ul>
 * <li> Feature flags, segments, and any other kind of entity the LaunchDarkly client may wish
 * to store, are stored as individual items with the key "{prefix}/features/{flag-key}",
 * "{prefix}/segments/{segment-key}", etc.
 * <li> The special key "{prefix}/$inited" indicates that the store contains a complete data set.
 * <li> Since Consul has limited support for transactions (they can't contain more than 64
 * operations), the Init method-- which replaces the entire data store-- is not guaranteed to
 * be atomic, so there can be a race condition if another process is adding new data via
 * Upsert. To minimize this, we don't delete all the data at the start; instead, we update
 * the items we've received, and then delete all other items. That could potentially result in
 * deleting new data from another process, but that would be the case anyway if the Init
 * happened to execute later than the Upsert; we are relying on the fact that normally the
 * process that did the Init will also receive the new data shortly and do its own Upsert.
 * </ul>
 */
class RedisFeatureStoreCore implements FeatureStoreCore {
  private static final Logger logger = LoggerFactory.getLogger(RedisFeatureStoreCore.class);

  private final JedisPool pool;
  private final String prefix;
  private UpdateListener updateListener;
  
  RedisFeatureStoreCore(JedisPool pool, String prefix) {
    this.pool = pool;
    this.prefix = prefix;
  }
  
  @Override
  public VersionedData getInternal(VersionedDataKind<?> kind, String key) {
    try (Jedis jedis = pool.getResource()) {
      VersionedData item = getRedis(kind, key, jedis);
      if (item != null) {
        logger.debug("[get] Key: {} with version: {} found in \"{}\".", key, item.getVersion(), kind.getNamespace());
      }
      return item;
    }
  }

  @Override
  public Map<String, VersionedData> getAllInternal(VersionedDataKind<?> kind) {
    try (Jedis jedis = pool.getResource()) {
      Map<String, String> allJson = jedis.hgetAll(itemsKey(kind));
      Map<String, VersionedData> result = new HashMap<>();

      for (Map.Entry<String, String> entry : allJson.entrySet()) {
        VersionedData item = FeatureStoreHelpers.unmarshalJson(kind, entry.getValue());
        result.put(entry.getKey(), item);
      }
      return result;
    }
  }
  
  @Override
  public void initInternal(Map<VersionedDataKind<?>, Map<String, VersionedData>> allData) {
    try (Jedis jedis = pool.getResource()) {
      Transaction t = jedis.multi();

      for (Map.Entry<VersionedDataKind<?>, Map<String, VersionedData>> entry: allData.entrySet()) {
        String baseKey = itemsKey(entry.getKey()); 
        t.del(baseKey);
        for (VersionedData item: entry.getValue().values()) {
          t.hset(baseKey, item.getKey(), FeatureStoreHelpers.marshalJson(item));
        }
      }

      t.set(initedKey(), "");
      t.exec();
    }
  }
  
  @Override
  public VersionedData upsertInternal(VersionedDataKind<?> kind, VersionedData newItem) {
    while (true) {
      Jedis jedis = null;
      try {
        jedis = pool.getResource();
        String baseKey = itemsKey(kind);
        jedis.watch(baseKey);
  
        if (updateListener != null) {
          updateListener.aboutToUpdate(baseKey, newItem.getKey());
        }
        
        VersionedData oldItem = getRedis(kind, newItem.getKey(), jedis);
  
        if (oldItem != null && oldItem.getVersion() >= newItem.getVersion()) {
          logger.debug("Attempted to {} key: {} version: {}" +
              " with a version that is the same or older: {} in \"{}\"",
              newItem.isDeleted() ? "delete" : "update",
              newItem.getKey(), oldItem.getVersion(), newItem.getVersion(), kind.getNamespace());
          return oldItem;
        }
  
        Transaction tx = jedis.multi();
        tx.hset(baseKey, newItem.getKey(), FeatureStoreHelpers.marshalJson(newItem));
        List<Object> result = tx.exec();
        if (result.isEmpty()) {
          // if exec failed, it means the watch was triggered and we should retry
          logger.debug("Concurrent modification detected, retrying");
          continue;
        }
  
        return newItem;
      } finally {
        if (jedis != null) {
          jedis.unwatch();
          jedis.close();
        }
      }
    }
  }
  
  @Override
  public boolean initializedInternal() {
    try (Jedis jedis = pool.getResource()) {
      return jedis.exists(initedKey());
    }
  }
  
  @Override
  public void close() throws IOException {
    logger.info("Closing LaunchDarkly RedisFeatureStore");
    pool.destroy();
  }

  // exposed for testing
  void setUpdateListener(UpdateListener updateListener) {
    this.updateListener = updateListener;
  }
  
  private String itemsKey(VersionedDataKind<?> kind) {
    return prefix + ":" + kind.getNamespace();
  }
  
  private String initedKey() {
    return prefix + ":$inited";
  }
  
  private <T extends VersionedData> T getRedis(VersionedDataKind<T> kind, String key, Jedis jedis) {
    String json = jedis.hget(itemsKey(kind), key);

    if (json == null) {
      logger.debug("[get] Key: {} not found in \"{}\". Returning null", key, kind.getNamespace());
      return null;
    }

    return FeatureStoreHelpers.unmarshalJson(kind, json);
  }

  static interface UpdateListener {
    void aboutToUpdate(String baseKey, String itemKey);
  }
}
