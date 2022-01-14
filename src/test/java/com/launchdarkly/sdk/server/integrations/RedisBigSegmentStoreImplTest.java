package com.launchdarkly.sdk.server.integrations;

import static org.junit.Assume.assumeTrue;

import com.launchdarkly.sdk.server.interfaces.BigSegmentStoreFactory;
import com.launchdarkly.sdk.server.interfaces.BigSegmentStoreTypes;

import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;

@SuppressWarnings("javadoc")
public class RedisBigSegmentStoreImplTest extends BigSegmentStoreTestBase {
  @BeforeClass
  public static void maybeSkipDatabaseTests() {
    String skipParam = System.getenv("LD_SKIP_DATABASE_TESTS");
    assumeTrue(skipParam == null || skipParam.equals(""));
  }

  @Override
  protected BigSegmentStoreFactory makeStore(String prefix) {
    return Redis.dataStore().prefix(prefix);
  }

  @Override
  protected void clearData(String prefix) {
    prefix = prefix == null || prefix.isEmpty() ? RedisDataStoreBuilder.DEFAULT_PREFIX : prefix;
    try (Jedis client = new Jedis("localhost")) {
      for (String key : client.keys(prefix + ":*")) {
        client.del(key);
      }
    }
  }

  @Override
  protected void setMetadata(String prefix, BigSegmentStoreTypes.StoreMetadata storeMetadata) {
    try (Jedis client = new Jedis("localhost")) {
      client.set(prefix + ":big_segments_synchronized_on",
          storeMetadata != null ? Long.toString(storeMetadata.getLastUpToDate()) : "");
    }
  }

  @Override
  protected void setSegments(String prefix,
                             String userHashKey,
                             Iterable<String> includedSegmentRefs,
                             Iterable<String> excludedSegmentRefs) {
    try (Jedis client = new Jedis("localhost")) {
      String includeKey = prefix + ":big_segment_include:" + userHashKey;
      String excludeKey = prefix + ":big_segment_exclude:" + userHashKey;
      for (String includedSegmentRef : includedSegmentRefs) {
        client.sadd(includeKey, includedSegmentRef);
      }
      for (String excludedSegmentRef : excludedSegmentRefs) {
        client.sadd(excludeKey, excludedSegmentRef);
      }
    }
  }
}
