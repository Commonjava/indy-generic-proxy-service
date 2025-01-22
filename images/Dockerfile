FROM registry.access.redhat.com/ubi9/openjdk-11:1.21-1 AS builder

ARG GIT_REVISION=main

USER root

RUN microdnf install -y git-core && microdnf clean all

WORKDIR /workspace

RUN git init && \
    git clone -b ${GIT_REVISION} --depth 1 https://github.com/Commonjava/indy-generic-proxy-service.git

RUN cd indy-generic-proxy-service && \
    mvn package -Dquarkus.package.type=uber-jar

FROM registry.access.redhat.com/ubi9/openjdk-11-runtime:1.21-1

USER root

RUN mkdir -p /deployment/log /deployment/config && \
  chmod -R 777 /deployment/log /deployment/config

COPY --from=builder /workspace/indy-generic-proxy-service/target/*-runner.jar /deployment/indy-generic-proxy-service-runner.jar
RUN chmod +r /deployment/indy-generic-proxy-service-runner.jar

COPY images/start-service.sh /deployment/start-service.sh
RUN chmod +x /deployment/*

WORKDIR /

ENTRYPOINT ["bash", "-c"]
CMD ["/deployment/start-service.sh"]