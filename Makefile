keycloak_version = 26.7.3
date := $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag := $(keycloak_version)-$(date)

.PHONY: submodules
submodules:
	@git submodule status | awk '/^-/ {print $$2}' | while read path; do \
		[ -n "$$path" ] && git submodule update --init "$$path"; \
	done

build-artifacts: submodules
	./mvnw clean compile package


# Builds the docker image
build: build-artifacts
	docker build --platform linux/amd64 --tag tidepool/keycloak-extensions:$(image_tag) .

release: build
	docker push tidepool/keycloak-extensions:$(image_tag)
