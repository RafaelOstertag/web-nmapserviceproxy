FROM maven:3-jdk-11-slim AS build

COPY src /build/src
COPY .git /build/.git
COPY pom.xml /build
COPY docker/guengel.ch.crt /tmp
RUN keytool -importcert -noprompt -cacerts -file /tmp/guengel.ch.crt -storepass changeit
WORKDIR /build
RUN mvn -B package

FROM openjdk:11-slim

COPY --from=build /build/target/*-jar-with-dependencies.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["/usr/local/openjdk-11/bin/java"]
CMD ["-jar", "/app/app.jar"]