# Indy Generic Proxy Service
Indy Generic Proxy Service is a single full-functional service providing a
generic HTTP proxy interface to Indy cached content, for non-Maven, non-NPM
builds.

## Prerequisite for building
1. jdk11
2. mvn 3.6.2+

## Configure

see [src/main/resources/application.yaml](src/main/resources/application.yaml) for details


## Try it

There are a few steps to set it up.

1. Build (make sure you use jdk11 and mvn 3.6.2+)
```
$ git clone git@github.com:Commonjava/indy-generic-proxy-service.git
$ cd indy-generic-proxy-service
$ mvn clean compile
```
2. Before running this service, it's required to have a running Indy instance
```
...
services:
  - host: localhost
...
repo-service-api/mp-rest/uri: http://localhost:8080
...
```
3. Start in debug mode
```
$ mvn quarkus:dev
```
