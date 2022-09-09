keycloak_version = 16.1.0
date = $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag = $(keycloak_version)-$(date)

build-artifacts:
	./mvnw clean compile package

# Builds the docker image
build: build-artifacts
	docker build -t tidepool/keycloak-extensions:$(image_tag) .


