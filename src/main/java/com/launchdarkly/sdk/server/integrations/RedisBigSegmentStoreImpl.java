package com.launchdarkly.sdk.server.integrations;

import com.launchdarkly.sdk.server.interfaces.BigSegmentStore;
import com.launchdarkly.sdk.server.interfaces.BigSegmentStoreTypes;

import java.util.Set;

import redis.clients.jedis.Jedis;

final class RedisBigSegmentStoreImpl extends RedisStoreImplBase implements BigSegmentStore {
  private static final String LOGGER_NAME = "com.launchdarkly.sdk.server.LDClient.BigSegments.Redis";

  private final String syncTimeKey;
  private final String includedKeyPrefix;
  private final String excludedKeyPrefix;

  RedisBigSegmentStoreImpl(RedisDataStoreBuilder builder) {
    super(builder, LOGGER_NAME);
    syncTimeKey = prefix + ":big_segments_synchronized_on";
    includedKeyPrefix = prefix + ":big_segment_include:";
    excludedKeyPrefix = prefix + ":big_segment_exclude:";
  }

  @Override
  public BigSegmentStoreTypes.Membership getMembership(String userHash) {
    try (Jedis jedis = pool.getResource()) {
      Set<String> includedRefs = jedis.smembers(includedKeyPrefix + userHash);
      Set<String> excludedRefs = jedis.smembers(excludedKeyPrefix + userHash);
      return BigSegmentStoreTypes.createMembershipFromSegmentRefs(includedRefs, excludedRefs);
    }
  }

  @Override
  public BigSegmentStoreTypes.StoreMetadata getMetadata() {
    try (Jedis jedis = pool.getResource()) {
      String value = jedis.get(syncTimeKey);
      if (value == null || value.isEmpty()) {
        return null;
      }
      return new BigSegmentStoreTypes.StoreMetadata(Long.parseLong(value));
    }
  }
}
