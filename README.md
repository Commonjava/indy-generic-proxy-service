# Indy Generic Proxy Service
Indy Generic Proxy Service is a single full-functional service providing a
generic HTTP proxy interface to Indy cached content, for non-Maven, non-NPM
builds.

## Prerequisite for building
1. jdk11
2. mvn 3.6.2+

## Prerequisite for debugging in local
1. docker 20+
2. docker-compose 1.20+

## Configure

see [src/main/resources/application.yaml](src/main/resources/application.properties) for details


## Try it

There are a few steps to set it up.

1. Build (make sure you use jdk11 and mvn 3.6.2+)
```
$ git clone git@github.com:Commonjava/indy-repository-service.git
$ cd indy-repository-service
$ mvn clean compile
```
2. Start depending services:
```
$ docker-compose up
```
2. Start in debug mode
```
$ mvn quarkus:dev
```
