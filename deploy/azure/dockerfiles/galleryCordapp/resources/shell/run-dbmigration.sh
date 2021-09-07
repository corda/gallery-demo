#!/usr/bin/env bash
mkdir -p /opt/corda/logs
cd /opt/corda || exit 2
java -jar bin/corda.jar run-migration-scripts --config-file /etc/corda/node.conf --core-schemas --app-schemas
