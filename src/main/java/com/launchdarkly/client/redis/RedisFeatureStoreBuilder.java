package com.launchdarkly.client.redis;

import com.launchdarkly.client.FeatureStore;
import com.launchdarkly.client.FeatureStoreCacheConfig;
import com.launchdarkly.client.FeatureStoreFactory;
import com.launchdarkly.client.utils.CachingStoreWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.JedisURIHelper;

/**
 * Builder/factory class for the Redis feature store.
 * <p>
 * Create this builder by calling {@link RedisComponents#redisFeatureStore()}, then
 * optionally modify its properties with builder methods, and then include it in your client
 * configuration with {@link com.launchdarkly.client.LDConfig.Builder#featureStoreFactory(FeatureStoreFactory)}.
 */
public class RedisFeatureStoreBuilder implements FeatureStoreFactory {
  private static final Logger logger = LoggerFactory.getLogger(RedisFeatureStoreBuilder.class);

  /**
   * The default value for the Redis URI: {@code redis://localhost:6379}
   */
  public static final URI DEFAULT_URI = URI.create("redis://localhost:6379");
  
  /**
   * The default value for {@link #prefix(String)}.
   */
  public static final String DEFAULT_PREFIX = "launchdarkly";
    
  private URI uri = DEFAULT_URI;
  private String prefix = DEFAULT_PREFIX;
  private int connectTimeout = Protocol.DEFAULT_TIMEOUT;
  private int socketTimeout = Protocol.DEFAULT_TIMEOUT;
  private Integer database = null;
  private String password = null;
  private boolean tls = false;
  private FeatureStoreCacheConfig caching = FeatureStoreCacheConfig.DEFAULT;
  private JedisPoolConfig poolConfig = null;
  
  RedisFeatureStoreBuilder() {
  }
  
  @Override
  public FeatureStore createFeatureStore() {
    // There is no builder for JedisPool, just a large number of constructor overloads. Unfortunately,
    // the overloads that accept a URI do not accept the other parameters we need to set, so we need
    // to decompose the URI.
    String host = uri.getHost();
    int port = uri.getPort();
    String password = this.password == null ? JedisURIHelper.getPassword(uri) : this.password;
    int database = this.database == null ? JedisURIHelper.getDBIndex(uri): this.database.intValue();
    boolean tls = this.tls || uri.getScheme().equals("rediss");
    
    String extra = tls ? " with TLS" : "";
    if (password != null) {
      extra = extra + (extra.isEmpty() ? " with" : " and") + " password";
    }
    logger.info(String.format("Connecting to Redis feature store at %s:%d/%d%s", host, port, database, extra));

    JedisPoolConfig poolConfig = (this.poolConfig != null) ? this.poolConfig : new JedisPoolConfig();    
    JedisPool pool = new JedisPool(poolConfig,
        host,
        port,
        connectTimeout,
        socketTimeout,
        password,
        database,
        null, // clientName
        tls,
        null, // sslSocketFactory
        null, // sslParameters
        null  // hostnameVerifier
        );

    String prefix = (this.prefix == null || this.prefix.isEmpty()) ?
        RedisFeatureStoreBuilder.DEFAULT_PREFIX :
        this.prefix;
    
    RedisFeatureStoreCore core = new RedisFeatureStoreCore(pool, prefix);
    CachingStoreWrapper wrapper = CachingStoreWrapper.builder(core).caching(caching).build();
    return wrapper;
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
  public RedisFeatureStoreBuilder database(Integer database) {
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
  public RedisFeatureStoreBuilder password(String password) {
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
  public RedisFeatureStoreBuilder tls(boolean tls) {
    this.tls = tls;
    return this;
  }

  /**
   * Specifies the URI of the Redis instance to connect to.
   * 
   * @param uri the URI of the Redis server
   * @return the builder
   */
  public RedisFeatureStoreBuilder uri(URI uri) {
    this.uri = uri;
    return this;
  }
  
  /**
   * Specifies whether local caching should be enabled and if so, sets the cache properties. Local
   * caching is enabled by default; see {@link FeatureStoreCacheConfig#DEFAULT}. To disable it, pass
   * {@link FeatureStoreCacheConfig#disabled()} to this method.
   * 
   * @param caching a {@link FeatureStoreCacheConfig} object specifying caching parameters
   * @return the builder
   */
  public RedisFeatureStoreBuilder caching(FeatureStoreCacheConfig caching) {
    this.caching = caching;
    return this;
  }
  
  /**
   * Optionally configures the namespace prefix for all keys stored in Redis.
   *
   * @param prefix the namespace prefix
   * @return the builder
   */
  public RedisFeatureStoreBuilder prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * Optional override if you wish to specify your own configuration to the underlying Jedis pool.
   *
   * @param poolConfig the Jedis pool configuration.
   * @return the builder
   */
  public RedisFeatureStoreBuilder poolConfig(JedisPoolConfig poolConfig) {
    this.poolConfig = poolConfig;
    return this;
  }

  /**
   * Optional override which sets the connection timeout for the underlying Jedis pool which otherwise defaults to
   * {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
   *
   * @param connectTimeout the timeout
   * @param timeUnit the time unit for the timeout
   * @return the builder
   */
  public RedisFeatureStoreBuilder connectTimeout(int connectTimeout, TimeUnit timeUnit) {
    this.connectTimeout = (int) timeUnit.toMillis(connectTimeout);
    return this;
  }

  /**
   * Optional override which sets the connection timeout for the underlying Jedis pool which otherwise defaults to
   * {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
   *
   * @param socketTimeout the socket timeout
   * @param timeUnit the time unit for the timeout
   * @return the builder
   */
  public RedisFeatureStoreBuilder socketTimeout(int socketTimeout, TimeUnit timeUnit) {
    this.socketTimeout = (int) timeUnit.toMillis(socketTimeout);
    return this;
  }
}
