# Change log

All notable changes to the LaunchDarkly Java SDK Redis integration will be documented in this file. This project adheres to [Semantic Versioning](http://semver.org).

## [1.1.0] - 2022-01-28
### Added:
- Added support for Big Segments. An Early Access Program for creating and syncing Big Segments from customer data platforms is available to enterprise customers.

## [1.0.1] - 2021-08-06
### Fixed:
- This integration now works with Jedis 3.x as well as Jedis 2.9.x. The default dependency is still 2.9.x, but an application can override this with a dependency on a 3.x version. (Thanks, [robotmlg](https://github.com/launchdarkly/java-server-sdk-redis/pull/3)!)

## [1.0.0] - 2020-06-02
Initial release, corresponding to the 5.0.0 release of [`launchdarkly-java-server-sdk`](https://github.com/launchdarkly/java-server-sdk).

Prior to that release, the Redis integration was built into the main SDK library. For more information about changes in the SDK database integrations, see the [4.x to 5.0 migration guide](https://docs-stg.launchdarkly.com/252/sdk/server-side/java/migration-4-to-5/).

