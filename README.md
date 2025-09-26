# sync4j

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/sync4j)](https://search.maven.org/artifact/com.fathzer/sync4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/sync4j.svg)](https://www.javadoc.io/doc/com.fathzer/sync4j)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=fathzer_sync4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer_sync4j)

sync4j is a Java library for synchronizing files and folders.

It is designed as a replacement of the famous [rclone](https://rclone.org/) tool as a reusable library.  
*rclone* is a very impressive command line program to manage files on various cloud storages ... but has some drawbacks when launched from an application.
Typically, the root cause of this alternative project is *rclone* command line tool stops sending progress data on stdout when it is used, for instance in a Docker container with no tty. It's http API (launched with `rclone rcd`) misses clear documentation and some important features like file exclusions remains unsupported (or at least undocumented). This makes it unusable in application development if you need to track progress of long synchronizations and exclude files, which seems to be a common use case.

## Features

- Synchronize files and folders
- Support for local files and folders included in this project (see [LocalProvider](https://github.com/fathzer/sync4j/blob/main/src/main/java/com/fathzer/sync4j/file/LocalProvider.java))
- Support for remote files and folders:
    Currently there's only one provider supported:
    - [pCloud](https://www.pcloud.com/) -> [pCloudProvider](https://github.com/fathzer/sync4j-pcloud)

## Requirements

- Java 21

## Installation

Import with Maven:

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>sync4j</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

    
