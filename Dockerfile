FROM maven:3.8.6-jdk-11 as build

COPY . /build
WORKDIR /build

RUN unset MAVEN_CONFIG && \
    ./mvnw versions:set -DnewVersion=LATEST && \
    ./mvnw install && \
    ./mvnw clean compile package && \
    wget -O keycloak-rest-provider.jar https://github.com/daniel-frak/keycloak-user-migration/releases/download/1.0.0/keycloak-rest-provider-1.0.0.jar && \
    wget -O keycloak-metrics-spi.jar https://github.com/aerogear/keycloak-metrics-spi/releases/download/3.0.0/keycloak-metrics-spi-3.0.0.jar && \
    wget -O keycloak-home-idp-discovery.jar https://github.com/tidepool-org/keycloak-home-idp-discovery/releases/download/v21.4.0-alpha.1/keycloak-home-idp-discovery.jar

FROM alpine:latest as release

COPY --from=build /build/admin/target/*.jar /release/extensions/
COPY --from=build /build/*.jar /release/extensions/
COPY ./tidepool-theme /release/tidepool-theme
