keycloak_version = 26.1.2
date = $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag = $(keycloak_version)-$(date)

build-artifacts:
	./mvnw clean compile package

# Builds the docker image
build: build-artifacts
	docker build --platform linux/amd64 --tag tidepool/keycloak-extensions:$(image_tag) .
