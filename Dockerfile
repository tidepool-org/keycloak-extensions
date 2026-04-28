FROM maven:3.8.5-openjdk-17 AS build

COPY . /build
WORKDIR /build

RUN microdnf update \
 && microdnf install --nodocs wget unzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

RUN unset MAVEN_CONFIG && \
    ./mvnw versions:set -DnewVersion=LATEST -Drevision=LATEST && \
    ./mvnw install && \
    ./mvnw clean compile package && \
    wget -O keycloak-rest-provider.jar https://github.com/daniel-frak/keycloak-user-migration/releases/download/6.2.1/keycloak-rest-provider-6.2.1.jar && \
    wget -O keycloak-metrics-spi.jar https://github.com/aerogear/keycloak-metrics-spi/releases/download/7.0.0/keycloak-metrics-spi-7.0.0.jar && \
    wget -O keycloak-home-idp-discovery.jar https://github.com/tidepool-org/keycloak-home-idp-discovery/releases/download/v26.6.1/keycloak-home-idp-discovery.jar

FROM alpine:latest AS release

COPY --from=build /build/admin/target/*.jar /release/extensions/
COPY --from=build /build/keycloak-spi-trusted-device/spi/target/keycloak-spi-trusted-device-LATEST.jar /release/extensions/
COPY --from=build /build/*.jar /release/extensions/
COPY ./tidepool-theme /release/tidepool-theme
