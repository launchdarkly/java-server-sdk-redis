# LaunchDarkly SDK for Java - Redis integration

[![Circle CI](https://circleci.com/gh/launchdarkly/java-server-sdk-redis.svg?style=shield)](https://circleci.com/gh/launchdarkly/java-server-sdk-redis)
[![Javadocs](http://javadoc.io/badge/com.launchdarkly/launchdarkly-server-sdk-redis-store.svg)](http://javadoc.io/doc/com.launchdarkly/launchdarkly-server-sdk-redis-store)

This library provides a Redis-backed persistence mechanism (feature store) for the [LaunchDarkly Java SDK](https://github.com/launchdarkly/java-server-sdk), replacing the default in-memory feature store. The Redis API implementation it uses is [Jedis](https://github.com/xetorthio/jedis).

This version of the library requires at least version 4.6.4 of the LaunchDarkly Java SDK. The minimum Java version is 7.

For more information, see also: [Using a persistent feature store](https://docs.launchdarkly.com/v2.0/docs/using-a-persistent-feature-store).

## Quick setup

This assumes that you have already installed the LaunchDarkly Java SDK.

1. Add this library to your project:

        <dependency>
          <groupId>com.launchdarkly</groupId>
          <artifactId>launchdarkly-java-server-sdk-redis-store</artifactId>
          <version>1.0.0</version>
        </dependency>

3. If you do not already have the Redis client in your project, add it. (This needs to be added separately, rather than being included in the LaunchDarkly jar, because some of its classes are exposed in the public interface and some applications might have a different version of it.)

        <dependency>
          <groupId>redis.clients</groupId>
          <artifactId>jedis</artifactId>
          <version>2.9.0</version>
        </dependency>

4. Import the LaunchDarkly package and the package for this library:

        import com.launchdarkly.client.*;
        import com.launchdarkly.client.redis.*;

5. When configuring your SDK client, add the Redis feature store:

        RedisFeatureStoreBuilder store = RedisComponents.redisFeatureStore()
            .caching(FeatureStoreCacheConfig.enabled().ttlSeconds(30));
        
        LDConfig config = new LDConfig.Builder()
            .featureStoreFactory(store)
            .build();
        
        LDClient client = new LDClient("YOUR SDK KEY", config);

By default, the store will try to connect to a local Redis instance on port 6379. There are methods in `RedisFeatureStoreBuilder` for changing the configuration options.

## Caching behavior

To reduce traffic to Redis, there is an optional in-memory cache that retains the last known data for a configurable amount of time. This is on by default; to turn it off (and guarantee that the latest feature flag data will always be retrieved from Consul for every flag evaluation), configure the store as follows:

        RedisFeatureStoreBuilder store = RedisComponents.redisFeatureStore()
            .caching(FeatureStoreCacheConfig.disabled());

For other ways to control the behavior of the cache, see `RedisFeatureStoreBuilder.caching()`.

## About LaunchDarkly

* LaunchDarkly is a continuous delivery platform that provides feature flags as a service and allows developers to iterate quickly and safely. We allow you to easily flag your features and manage them from the LaunchDarkly dashboard.  With LaunchDarkly, you can:
    * Roll out a new feature to a subset of your users (like a group of users who opt-in to a beta tester group), gathering feedback and bug reports from real-world use cases.
    * Gradually roll out a feature to an increasing percentage of users, and track the effect that the feature has on key metrics (for instance, how likely is a user to complete a purchase if they have feature A versus feature B?).
    * Turn off a feature that you realize is causing performance problems in production, without needing to re-deploy, or even restart the application with a changed configuration file.
    * Grant access to certain features based on user attributes, like payment plan (eg: users on the ‘gold’ plan get access to more features than users in the ‘silver’ plan). Disable parts of your application to facilitate maintenance, without taking everything offline.
* LaunchDarkly provides feature flag SDKs for a wide variety of languages and technologies. Check out [our documentation](https://docs.launchdarkly.com/docs) for a complete list.
* Explore LaunchDarkly
    * [launchdarkly.com](https://www.launchdarkly.com/ "LaunchDarkly Main Website") for more information
    * [docs.launchdarkly.com](https://docs.launchdarkly.com/  "LaunchDarkly Documentation") for our documentation and SDK reference guides
    * [apidocs.launchdarkly.com](https://apidocs.launchdarkly.com/  "LaunchDarkly API Documentation") for our API documentation
    * [blog.launchdarkly.com](https://blog.launchdarkly.com/  "LaunchDarkly Blog Documentation") for the latest product updates
    * [Feature Flagging Guide](https://github.com/launchdarkly/featureflags/  "Feature Flagging Guide") for best practices and strategies
