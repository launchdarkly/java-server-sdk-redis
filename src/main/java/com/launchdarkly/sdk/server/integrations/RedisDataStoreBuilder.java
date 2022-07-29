package com.launchdarkly.sdk.server.integrations;

import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.interfaces.BasicConfiguration;
import com.launchdarkly.sdk.server.interfaces.BigSegmentStore;
import com.launchdarkly.sdk.server.interfaces.BigSegmentStoreFactory;
import com.launchdarkly.sdk.server.interfaces.ClientContext;
import com.launchdarkly.sdk.server.interfaces.DiagnosticDescription;
import com.launchdarkly.sdk.server.interfaces.PersistentDataStore;
import com.launchdarkly.sdk.server.interfaces.PersistentDataStoreFactory;

import java.net.URI;
import java.time.Duration;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * A <a href="http://en.wikipedia.org/wiki/Builder_pattern">builder</a> for configuring the
 * Redis-based persistent data store.
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
 * @since 4.12.0
 */
public final class RedisDataStoreBuilder
    implements PersistentDataStoreFactory, BigSegmentStoreFactory, DiagnosticDescription {
  /**
   * The default value for the Redis URI: {@code redis://localhost:6379}
   */
  public static final URI DEFAULT_URI = URI.create("redis://localhost:6379");
  
  /**
   * The default value for {@link #prefix(String)}.
   */
  public static final String DEFAULT_PREFIX = "launchdarkly";
  
  URI uri = DEFAULT_URI;
  String prefix = DEFAULT_PREFIX;
  Duration connectTimeout = Duration.ofMillis(Protocol.DEFAULT_TIMEOUT);
  Duration socketTimeout = Duration.ofMillis(Protocol.DEFAULT_TIMEOUT);
  Integer database = null;
  String password = null;
  boolean tls = false;
  JedisPoolConfig poolConfig = null;

  // These constructors are called only from Implementations
  RedisDataStoreBuilder() {
  }
  
  /**
   * Specifies the database number to use.
   * <p>
   * The database number can also be specified in the Redis URI, in the form {@code redis://host:port/NUMBER}. Any
   * non-null value that you set with {@link #database(Integer)} will override the URI.
   * 
   * @param database the database number, or null to fall back to the URI or the default
   * @return the builder
   */
  public RedisDataStoreBuilder database(Integer database) {
    this.database = database;
    return this;
  }
  
  /**
   * Specifies a password that will be sent to Redis in an AUTH command.
   * <p>
   * It is also possible to include a password in the Redis URI, in the form {@code redis://:PASSWORD@host:port}. Any
   * password that you set with {@link #password(String)} will override the URI.
   * 
   * @param password the password
   * @return the builder
   */
  public RedisDataStoreBuilder password(String password) {
    this.password = password;
    return this;
  }
  
  /**
   * Optionally enables TLS for secure connections to Redis.
   * <p>
   * This is equivalent to specifying a Redis URI that begins with {@code rediss:} rather than {@code redis:}.
   * <p>
   * Note that not all Redis server distributions support TLS.
   * 
   * @param tls true to enable TLS
   * @return the builder
   */
  public RedisDataStoreBuilder tls(boolean tls) {
    this.tls = tls;
    return this;
  }
  
  /**
   * Specifies a Redis host URI other than {@link #DEFAULT_URI}.
   * 
   * @param redisUri the URI of the Redis host
   * @return the builder
   */
  public RedisDataStoreBuilder uri(URI redisUri) {
    this.uri = redisUri;
    return this;
  }
    
  /**
   * Optionally configures the namespace prefix for all keys stored in Redis.
   *
   * @param prefix the namespace prefix
   * @return the builder
   */
  public RedisDataStoreBuilder prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * Optional override if you wish to specify your own configuration to the underlying Jedis pool.
   *
   * @param poolConfig the Jedis pool configuration.
   * @return the builder
   */
  public RedisDataStoreBuilder poolConfig(JedisPoolConfig poolConfig) {
    this.poolConfig = poolConfig;
    return this;
  }

  /**
   * Optional override which sets the connection timeout for the underlying Jedis pool which otherwise defaults to
   * {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT} milliseconds.
   *
   * @param connectTimeout the timeout
   * @return the builder
   */
  public RedisDataStoreBuilder connectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout == null ? Duration.ofMillis(Protocol.DEFAULT_TIMEOUT) : connectTimeout;
    return this;
  }

  /**
   * Optional override which sets the connection timeout for the underlying Jedis pool which otherwise defaults to
   * {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT} milliseconds.
   *
   * @param socketTimeout the socket timeout
   * @return the builder
   */
  public RedisDataStoreBuilder socketTimeout(Duration socketTimeout) {
    this.socketTimeout = socketTimeout == null ? Duration.ofMillis(Protocol.DEFAULT_TIMEOUT) : socketTimeout;
    return this;
  }

  @Override
  public PersistentDataStore createPersistentDataStore(ClientContext context) {
    return new RedisDataStoreImpl(this, context.getBasic().getBaseLogger());
  }

  @Override
  public BigSegmentStore createBigSegmentStore(ClientContext context) {
    return new RedisBigSegmentStoreImpl(this, context.getBasic().getBaseLogger());
  }

  @Override
  public LDValue describeConfiguration(BasicConfiguration config) {
    return LDValue.of("Redis");
  }
}
