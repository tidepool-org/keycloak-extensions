# Extensions for Keycloak

This project contains multiple custom extensions that we use in our Keycloak deployment.

### Packaging artifacts

Artifacts are packaged using maven. If an extensions depends on a library that's not provided by keycloak it should be deployed as an ear. 
To build all artifacts use the following command:
```
./mvnw clean compile package
```

### Releasing to github

Create a maven settings file in `~/.m2/settings.xml` with the following contents and fill in your username and api token:
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>${user.home}/.m2/repository</localRepository>

  <servers>
    <server>
        <id>github</id>
        <username>GITHUB_USERNAME</username>
        <privateKey>GITHUB_API_TOKEN</privateKey>
    </server>
  </servers>
</settings>
```

To create a github release and upload the generated artifacts on github use the following command:
```
./mvnw -pl . de.jutzig:github-release-plugin:1.1.1:release 
```
