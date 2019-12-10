package com.launchdarkly.client.redis;

import com.launchdarkly.client.FeatureStore;
import com.launchdarkly.client.FeatureStoreCacheConfig;
import com.launchdarkly.client.FeatureStoreDatabaseTestBase;
import com.launchdarkly.client.redis.RedisFeatureStoreCore.UpdateListener;
import com.launchdarkly.client.utils.CachingStoreWrapper;
import redis.clients.jedis.Jedis;

import java.net.URI;

/**
 * Runs the standard database feature store test suite that's defined in the Java SDK.
 * <p>
 * Note that you must be running a local Redis instance to run these tests.
 */
public class RedisFeatureStoreTest extends FeatureStoreDatabaseTestBase<FeatureStore> {

  private static final URI REDIS_URI = URI.create("redis://localhost:6379");
  
  public RedisFeatureStoreTest(boolean cached) {
    super(cached);
  }

  @Override
  protected FeatureStore makeStore() {
    return RedisComponents.redisFeatureStore()
        .uri(REDIS_URI)
        .caching(cached ? FeatureStoreCacheConfig.enabled().ttlSeconds(30) : FeatureStoreCacheConfig.disabled())
        .createFeatureStore();
  }
  
  @Override
  protected FeatureStore makeStoreWithPrefix(String prefix) {
    return RedisComponents.redisFeatureStore()
        .uri(REDIS_URI)
        .caching(FeatureStoreCacheConfig.disabled())
        .prefix(prefix)
        .createFeatureStore();
  }

  @Override
  protected void clearAllData() {
    try (Jedis client = new Jedis("localhost")) {
      client.flushDB();
    }
  }
  
  @Override
  protected boolean setUpdateHook(FeatureStore storeUnderTest, final Runnable hook) {
    RedisFeatureStoreCore core = (RedisFeatureStoreCore)((CachingStoreWrapper)storeUnderTest).getCore();
    core.setUpdateListener(new UpdateListener() {
      @Override
      public void aboutToUpdate(String baseKey, String itemKey) {
        hook.run();
      }
    });
    return true;
  }
}
