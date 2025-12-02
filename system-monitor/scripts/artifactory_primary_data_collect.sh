#!/bin/bash
>/opt/node_exporter/artifactory_metric.temp
exec >> /opt/node_exporter/artifactory_metric.temp 2>&1

/opt/node_exporter/scripts/artifactory_status.py
/opt/node_exporter/scripts/artifactory_storage.py
#/opt/node_exporter/scripts/nginx.py

cp -rf /opt/node_exporter/artifactory_metric.temp /opt/node_exporter/artifactory_metric.prom