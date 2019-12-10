/**
 * This package provides a Redis-backed feature store for the LaunchDarkly Java SDK.
 * <p>
 * For more details about how and why you can use a persistent feature store, see:
 * https://docs.launchdarkly.com/v2.0/docs/using-a-persistent-feature-store
 * <p>
 * To use the Redis feature store with the LaunchDarkly client, you will first obtain a
 * builder by calling {@link com.launchdarkly.client.redis.RedisComponents#redisFeatureStore()}, then optionally
 * modify its properties, and then include it in your client configuration. For example:
 * 
 * <pre>
 * import com.launchdarkly.client.*;
 * import com.launchdarkly.client.redis.*;

 * RedisFeatureStoreBuilder store = RedisComponents.redisFeatureStore()
 *     .caching(FeatureStoreCacheConfig.enabled().ttlSeconds(30));
 * LDConfig config = new LDConfig.Builder()
 *     .featureStoreFactory(store)
 *     .build();
 * </pre>
 * 
 * The default Redis configuration uses an address of localhost:6379. To customize any
 * properties of Redis, you can use the methods on {@link com.launchdarkly.client.redis.RedisFeatureStoreBuilder}.
 * <p>
 * If you are using the same Redis host as a feature store for multiple LaunchDarkly
 * environments, use the {@link com.launchdarkly.client.redis.RedisFeatureStoreBuilder#prefix(String)}
 * option and choose a different prefix string for each, so they will not interfere with each
 * other's data. 
 */
package com.launchdarkly.client.redis;