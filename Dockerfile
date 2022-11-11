FROM maven:3.8.4-jdk-11 as build

COPY . /build
WORKDIR /build

RUN unset MAVEN_CONFIG && \
    ./mvnw versions:set -DnewVersion=LATEST && \
    ./mvnw install && \
    ./mvnw clean compile package

FROM alpine:3.15 as release

COPY --from=build /build/admin/target/*.jar /release/extensions/
COPY ./tidepool-theme /release/tidepool-theme
