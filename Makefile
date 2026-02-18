keycloak_version = 26.1.3
date := $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag := $(keycloak_version)-$(git rev-parse --abbrev-ref HEAD)-$(git rev-parse --short HEAD)-$(date)

build-artifacts:
	./mvnw clean compile package

# Builds the docker image
build: build-artifacts
	docker build --platform linux/amd64 --tag tidepool/keycloak-extensions:$(image_tag) .

release: build
	docker push tidepool/keycloak-extensions:$(image_tag)
