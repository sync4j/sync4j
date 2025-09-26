# sync4j

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/sync4j)](https://search.maven.org/artifact/com.fathzer/sync4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/sync4j.svg)](https://www.javadoc.io/doc/com.fathzer/sync4j)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=fathzer_sync4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer_sync4j)

sync4j is a Java library for synchronizing files and folders.

It is designed as a replacement of famous [rclone](https://rclone.org/) tool in development.  
*rclone* is a very impressive command line program to manage files on cloud storage ... but has some drawbacks when used in applications.
Typically, the root cause of this alternative project is *rclone* stops sending progress data on stdout when it is used, for instance in a Docker container with no tty, making it unusable in applications that need to track progress of long synchronizations.

## Features

- Synchronize files and folders
- Support for local files and folders included in this project (see [LocalProvider](https://github.com/fathzer/sync4j/blob/main/src/main/java/com/fathzer/sync4j/file/LocalProvider.java))
- Support for remote files and folders:
    Currently there's only one provider supported:
    - [pCloud](https://www.pcloud.com/) -> [pCloudProvider](https://github.com/fathzer/sync4j-pcloud)
    
