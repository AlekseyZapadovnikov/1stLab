#!/usr/local/bin/bash
set -euo pipefail

# Run once for a new domain created with --portbase 12000.
export AS_JAVA=/usr/local/openjdk17
export JAVA_TOOL_OPTIONS='-XX:MaxHeapSize=1G -XX:MaxMetaspaceSize=128m'
ASADMIN="$HOME/IS/glassfish7/bin/asadmin"

asadmin() {
    /usr/local/bin/bash "$ASADMIN" --port 12048 --interactive=false "$@"
}

awk -F: '$4 == "s465945" {printf "AS_ADMIN_ALIASPASSWORD=%s\n", $5; exit}' "$HOME/.pgpass" |
    asadmin --passwordfile /dev/stdin create-password-alias lab-db-password

asadmin delete-jvm-options -- -Xmx512m
asadmin create-jvm-options '-XX\:MaxHeapSize=1G:-XX\:MaxMetaspaceSize=128m'
asadmin set configs.config.server-config.java-config.java-home=/usr/local/openjdk17
asadmin set configs.config.server-config.network-config.network-listeners.network-listener.admin-listener.address=127.0.0.1
asadmin set configs.config.server-config.admin-service.jmx-connector.system.address=127.0.0.1
asadmin set configs.config.server-config.iiop-service.iiop-listener.orb-listener-1.address=127.0.0.1
asadmin set configs.config.server-config.iiop-service.iiop-listener.SSL.address=127.0.0.1
asadmin set configs.config.server-config.iiop-service.iiop-listener.SSL_MUTUALAUTH.address=127.0.0.1
asadmin create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
    --restype javax.sql.DataSource --steadypoolsize 2 --maxpoolsize 16 \
    --property 'serverName=pg:portNumber=5432:databaseName=studs:user=s465945:currentSchema=s465945' LabPool
asadmin set 'resources.jdbc-connection-pool.LabPool.property.password=${ALIAS=lab-db-password}'
asadmin create-jdbc-resource --connectionpoolid LabPool jdbc/LabDS
asadmin ping-connection-pool LabPool
