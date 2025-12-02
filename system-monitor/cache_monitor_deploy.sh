#!/bin/bash
if [[ $# -eq 0 ]] ; then
    echo 'No cache env argument supplied'
    echo 'Env List: [on_prem_bjo|aws_east_1|aws_east_2|aws_west_1|aws_west_2|on_prem_us_west|on_prem_eu|aws_sgp_1]'
    exit 1
fi
# install node_exported
curl -s https://arti-cache.private-domain/generic/pqm/exporters/node_exporter_install.sh | sh
[ -d /opt/node_exporter/artifactory ] || mkdir /opt/node_exporter/artifactory
[ -d /opt/node_exporter/scripts ] || mkdir /opt/node_exporter/scripts
cp -rf scripts/* /opt/node_exporter/scripts/
#mv /opt/node_exporter/scripts/cache_nginx.py /opt/node_exporter/scripts/nginx.py
mv /opt/node_exporter/scripts/artifactory_storage_collect_cache.py /opt/node_exporter/scripts/artifactory_storage_collect.py

deploy_env=$1
env_expect="dig_${deploy_env}_expect"
sed -i "s/dig_expect/$env_expect/g" /opt/node_exporter/scripts/artifactory_cache_dig_download_monitor.sh

if [[ $deploy_env == *"aws"* ]]; then
    echo "Modify artifactory_cache_cloudwatch_metricdata_update.py env"
    region=${deploy_env#*_}  # Remove everything up to and including the first underscore
    formatted_region=$(echo "$region" | sed -E 's/^([a-z])/\U\1/; s/_/-/')
    sed -i "s/Region_Value/$formatted_region/g" /opt/node_exporter/scripts/artifactory_cache_cloudwatch_metricdata_update.py
fi
