keycloak_version = 16.1.0
date = $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag = $(keycloak_version)-$(date)

# Builds the docker image
build: 
	docker build -t tidepool/keycloak-extensions:$(image_tag) .


