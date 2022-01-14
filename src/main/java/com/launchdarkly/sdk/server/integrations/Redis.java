package com.launchdarkly.sdk.server.integrations;

import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.interfaces.PersistentDataStoreFactory;
import com.launchdarkly.sdk.server.interfaces.BigSegmentStoreFactory;

/**
 * Integration between the LaunchDarkly SDK and Redis.
 * 
 * @since 4.12.0
 */
public abstract class Redis {
  /**
   * Returns a builder object for creating a Redis-backed data store.
   * <p>
   * This can be used either for the main data store that holds feature flag data, or for the Big
   * Segment store, or both. If you are using both, they do not have to have the same parameters.
   * For instance, in this example the main data store uses a Redis host called "host1" and the Big
   * Segment store uses a Redis host called "host2":
   * <pre><code>
   *     LDConfig config = new LDConfig.Builder()
   *         .dataStore(
   *             Components.persistentDataStore(
   *                 Redis.dataStore().uri(URI.create("redis://host1:6379")
   *             )
   *         )
   *         .bigSegments(
   *             Components.bigSegments(
   *                 Redis.dataStore().uri(URI.create("redis://host2:6379")
   *             )
   *         )
   *         .build();
   * </code></pre>
   * <p>
   * Note that the builder is passed to one of two methods,
   * {@link Components#persistentDataStore(PersistentDataStoreFactory)} or
   * {@link Components#bigSegments(BigSegmentStoreFactory)}, depending on the context in which it is
   * being used. This is because each of those contexts has its own additional configuration options
   * that are unrelated to the Redis options. For instance, the
   * {@link Components#persistentDataStore(PersistentDataStoreFactory)} builder has options for
   * caching:
   * <pre><code>
   *     LDConfig config = new LDConfig.Builder()
   *         .dataStore(
   *             Components.persistentDataStore(
   *                 Redis.dataStore().uri(URI.create("redis://my-redis-host"))
   *             ).cacheSeconds(15)
   *         )
   *         .build();
   * </code></pre>
   * 
   * @return a data store configuration object
   */
  public static RedisDataStoreBuilder dataStore() {
    return new RedisDataStoreBuilder();
  }
  
  private Redis() {}
}
