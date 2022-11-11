keycloak_version = 20.0.1
date = $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag = $(keycloak_version)-$(date)

build-artifacts:
	./mvnw clean compile package

# Builds the docker image
build: build-artifacts
	docker build -t tidepool/keycloak-extensions:$(image_tag) .


