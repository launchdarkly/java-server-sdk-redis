package com.launchdarkly.client.redis;

/**
 * Entry point for using the Redis feature store.
 */
public abstract class RedisComponents {
  /**
   * Creates a builder for a Redis feature store. You can modify any of the store's properties with
   * {@link RedisFeatureStoreBuilder} methods before adding it to your client configuration with
   * {@link com.launchdarkly.client.LDConfig.Builder#featureStoreFactory(com.launchdarkly.client.FeatureStoreFactory)}.
   * 
   * @return the builder
   */
  public static RedisFeatureStoreBuilder redisFeatureStore() {
    return new RedisFeatureStoreBuilder();
  }
}
