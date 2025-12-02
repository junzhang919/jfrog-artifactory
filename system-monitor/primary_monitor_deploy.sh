#!/bin/bash
# install node_exported
curl -s https://arti-cache.private-domain/generic/pqm/exporters/node_exporter_install.sh | sh
[ -d /opt/node_exporter/artifactory ] || mkdir /opt/node_exporter/artifactory
[ -d /opt/node_exporter/scripts ] || mkdir /opt/node_exporter/scripts
cp -rf scripts/* /opt/node_exporter/scripts/
#mv /opt/node_exporter/scripts/primary_nginx.py /opt/node_exporter/scripts/nginx.py
mv /opt/node_exporter/scripts/artifactory_storage_collect_primary.py /opt/node_exporter/scripts/artifactory_storage_collect.py
