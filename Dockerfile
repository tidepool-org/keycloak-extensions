FROM maven:3.8.4-jdk-11 as build

COPY . /build
WORKDIR /build

RUN unset MAVEN_CONFIG && \
    ./mvnw versions:set -DnewVersion=LATEST && \
    ./mvnw install && \
    ./mvnw clean compile package && \
    wget -O keycloak-rest-provider.jar https://github.com/toddkazakov/keycloak-user-migration/releases/download/v1.1/keycloak-rest-provider.jar && \
    wget -O keycloak-metrics-spi.jar https://github.com/toddkazakov/keycloak-metrics-spi/releases/download/2.5.4-20.0.1/keycloak-metrics-spi-2.5.4.jar && \
    wget -O keycloak-home-idp-discovery.jar https://github.com/toddkazakov/keycloak-home-idp-discovery/releases/download/0.2.0/keycloak-home-idp-discovery.jar    

FROM alpine:3.15 as release

COPY --from=build /build/admin/target/*.jar /release/extensions/
COPY --from=build /build/*.jar /release/extensions/
COPY ./tidepool-theme /release/tidepool-theme
