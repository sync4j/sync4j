# sync4j

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/sync4j)](https://search.maven.org/artifact/com.fathzer/sync4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/sync4j.svg)](https://www.javadoc.io/doc/com.fathzer/sync4j)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=sync4j_sync4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sync4j_sync4j)

sync4j is a Java library for synchronizing files and folders.

It is designed as a replacement of the famous [rclone](https://rclone.org/) tool as a reusable library.  
*rclone* is a very impressive command line program to manage files on various cloud storages ... but has some drawbacks when launched from an application.

Typically, the root cause of this alternative project is *rclone* command line tool stops sending progress data on stdout when it is used, for instance in a Docker container with no tty. It's http API (launched with `rclone rcd`) misses clear documentation and some important features like file exclusions remains unsupported (or at least undocumented).

This makes it unusable in application development if you need to track progress of long synchronizations and exclude files, which seems to be a common use case.

## Features

- Synchronize files and folders
- Support for local files and folders included in this project (see [LocalProvider](https://github.com/fathzer/sync4j/blob/main/src/main/java/com/fathzer/sync4j/file/LocalProvider.java))
- Support for memory files and folders included in this project (see [MemoryFileProvider](https://github.com/fathzer/sync4j/blob/main/src/main/java/com/fathzer/sync4j/memory/MemoryFileProvider.java))
- Support for remote files and folders:
    Currently there's only one remote provider supported:
    - [pCloud](https://www.pcloud.com/) -> [pCloudProvider](https://github.com/fathzer/sync4j-pcloud)

## Requirements

- Java 17+

## Installation

Import with Maven:

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>sync4j</artifactId>
    <version>0.0.1</version>
</dependency>
```

If you need to use a provider other than local or memory, you should add the corresponding dependency.

## Usage

Here are some basic examples.  
Please have a look at the [javadoc](https://www.javadoc.io/doc/com.fathzer/sync4j) for more details about configuration and available functionnalities.  
Please note that these examples requires the [pCloud provider](https://github.com/fathzer/sync4j-pcloud).

Copy a file from pCloud to a local folder:
```java
    try (PCloudProvider provider = new PCloudProvider(Zone.US, System.getenv("PCLOUD_TOKEN"))) {
        // Warning: File below is com.fathzer.sync4j.File instance.
        File source = provider.get("/old.txt").asFile();
        Folder target = LocalProvider.INSTANCE.get("/home/jma/tmp").asFolder();
        target.copy(source.getName(), source, null);
    }
```

A basic synchronization example (synchronizes a pCloud folder to a local folder):
```java
    SyncParameters params = new SyncParameters();
    // TODO: configure parameters (dryRun, fileComparator, eventListener, errorManager, performance) if you need to
    try (PCloudProvider provider = new PCloudProvider(Zone.US, "your-pcloud-token")) {
        Folder source = provider.get("/PhotosJM/2002").asFolder();
        Folder target = LocalProvider.INSTANCE.get("/home/jma/tmp/photosTest/2002").asFolder();
        try (Synchronization synchronizer = new Synchronization(source, target, params)) {
            final long start = System.currentTimeMillis();
            synchronizer.start();
            synchronizer.waitFor();
            final long end = System.currentTimeMillis();
            System.out.println("All tasks finished at " + end + " in " + (end - start) + " ms");
            System.out.println("Final Stats: " + synchronizer.getStatistics());
        }
    }
```

If the remote storage throttles you, you can also limit the number of operations (listing a folder, comparing or copying a file, creating or deleting an entry) per second, whatever the number of threads: `params.performance().maxOperationsPerSecond(10)`. Retries count as operations. Please note that an operation can send several requests to the remote storage, so this is not a limit of requests per second. By default, there is no limit.

Please note that by default, the synchronization will use 1 thread to walk folders, 1 thread to copy files and 1 thread to compare files. Increasing the number of threads can speed up the synchronization, but it may cause some issues with the remote storage (depending on the provider's policies).

If a provider does not support concurrent operations, it is recommended to use a single thread by setting the [performance](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/SyncParameters.java#L50) parameter to zero copy threads and zero compare threads. Compare and copy operations will then be performed sequentially with the folder walking.

## Errors and retries

A failed operation is retried by the synchronization (never by the provider) when:
- the provider reports a transient error (throttling, timeout, temporary server error...) by throwing a `RetryableIOException`,
- or the provider is not [strongly consistent](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/FileProvider.java) (`FileProvider.isStronglyConsistent()`) and the operation fails in a way that can be a propagation delay: writing in a folder that was just created, or creating an entry right after the synchronization deleted one with the same name.

By default, an operation is attempted 3 times in total (the first attempt plus 2 retries), waiting 1s before the first retry and 2s before the second one. If you allow more attempts, the delay doubles after each failure, without exceeding 30s. If the provider tells how long to wait (Retry-After), this delay is used instead, unless it is longer than the maximum delay.
```java
    params.retry().maxAttempts(5).initialDelay(Duration.ofMillis(500)).maxDelay(Duration.ofSeconds(10));
```
The local and in-memory providers are strongly consistent, so their errors are never retried.

### Passes

If some operations still fail after their retries, the synchronization can run again (a new *pass*). A new pass compares the source and the destination again, so it only does what remains to be done. This helps with eventually consistent storages, where something that was created or deleted during a pass may only be visible a bit later.

- A new pass is started only if an error of the previous pass can be transient (a `RetryableIOException`, or an error that can be a propagation delay on a destination that is not strongly consistent). Definitive errors (authentication failure, read-only destination, etc.) never start a new pass.
- By default, a synchronization runs at most 3 passes, with the same exponential delay as above between the passes (`params.retry().maxPasses(n)`, 1 means never run again).
- **The default error manager cancels the synchronization on the first error, and a cancelled synchronization never starts a new pass.** To use passes, set an error manager that lets the synchronization continue:
```java
    params.errorManager((error, action) -> {
        System.err.println(action + " failed: " + error);
        return false; // true cancels the synchronization
    });
    params.retry().maxPasses(3);
```
- `Synchronization.waitFor()` returns when the last pass is over, and `getErrors()` returns the errors of the last pass.
- Statistics keep what the previous passes did. The tasks that failed are not counted again by the new pass.
- The events of a new pass use the same actions as the previous ones: an action that failed in a pass can be planned again (`PLANNED` after `FAILED`) in the next one.

## Implement your own provider

A provider is an implementation of the [FileProvider](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/FileProvider.java) interface. It allows you to access files on a remote storage (ssh, s3, etc...).

To implement your own provider, you should:
- Implement the [FileProvider](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/FileProvider.java) interface.
- Implement the [Folder](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/Folder.java) interface.
- Implement the [File](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/File.java) interface.
- Optionally, ship a [FileProviderFactory](https://github.com/sync4j/sync4j/blob/main/src/main/java/com/fathzer/sync4j/FileProviderFactory.java), declared in `META-INF/services/com.fathzer.sync4j.FileProviderFactory`. It creates your provider from a configuration object (`FileProviderFactory<T>`, `T` is a class, a `record` for instance, whose properties are the settings of your provider, like `record PCloudConfig(Zone zone, String token)`), so that programs driven by a configuration file, like [sync4j-config](https://github.com/sync4j/sync4j-config), can use it as soon as your library is on their classpath, without depending on it. The providers of this library (`local` and `memory`) ship their factory.
- Respect the contract described in the `FileProvider` javadoc: `Folder.mkdir` is idempotent, `Folder.copy` overwrites, errors are reported with the right exception type (`RetryableIOException`, `FileNotFoundException`, `FileAlreadyExistsException`...), and the provider does not retry by itself. Declare `isStronglyConsistent()` if your storage guarantees that an operation is visible as soon as it returns.

An example of a non trivial provider is the [pCloudProvider](https://github.com/fathzer/sync4j-pcloud).

