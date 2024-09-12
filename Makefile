keycloak_version = 25.0.4
date = $(shell date -u +"%Y-%m-%dT%H-%M-%S")
image_tag = $(keycloak_version)-$(date)

download-keycloak-extensions:
	mkdir -p temp && \
	wget -O ./temp/keycloak-home-idp-discovery.jar https://github.com/sventorben/keycloak-home-idp-discovery/releases/download/v25.0.0/keycloak-home-idp-discovery.jar

install-keycloak-extensions: download-keycloak-extensions
	./mvnw org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                             -Dfile=./temp/keycloak-home-idp-discovery.jar -DgroupId=de.sventorben.keycloak \
                             -DartifactId=keycloak-home-idp-discovery -Dversion=25.0.0 \
                             -Dpackaging=jar -DlocalRepositoryPath=maven-repo

build-artifacts:
	./mvnw clean compile package

# Builds the docker image
build: build-artifacts
	docker build --platform linux/amd64 --tag tidepool/keycloak-extensions:$(image_tag) .
