# Keycloak Prometheus Exporter

Wildfly doesn't export infinispan metrics in its metrics endpoint and the current version (20.x) doesn't allow loading prometheus exporter as a javaagent. 

This module allows the deployment of prometheus as module in the application server.
## Deployment

Copy the jar from the target directory to `$KEYCLOAK_HOME/standalone/deployments`
