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
    # The shade plugin leaves a pre-shade "original-admin-*.jar" alongside the shaded jar. Shipping both
    # puts our resources on the classpath twice; Keycloak/Liquibase aborts startup on a duplicate
    # changelog (META-INF/tidepool-user-activity-changelog.xml). Keep only the shaded jar.
    rm -f admin/target/original-*.jar && \
    wget -O keycloak-rest-provider.jar https://github.com/daniel-frak/keycloak-user-migration/releases/download/6.2.2/keycloak-rest-provider-6.2.2.jar && \
    wget -O keycloak-metrics-spi.jar https://github.com/aerogear/keycloak-metrics-spi/releases/download/7.0.0/keycloak-metrics-spi-7.0.0.jar

FROM alpine:latest AS release

COPY --from=build /build/admin/target/*.jar /release/extensions/
COPY --from=build /build/keycloak-spi-trusted-device/spi/target/keycloak-spi-trusted-device-LATEST.jar /release/extensions/
COPY --from=build /build/keycloak-home-idp-discovery/target/keycloak-home-idp-discovery.jar /release/extensions/
COPY --from=build /build/*.jar /release/extensions/
COPY ./tidepool-theme /release/tidepool-theme
