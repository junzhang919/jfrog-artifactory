#!/bin/bash

dig_aws_east_1_expect="arti-cache-beta.private-domain.akadns.net.
arti-cache-geo-beta.net.
arti-cache-east1.net."

dig_aws_east_2_expect="arti-cache-beta.private-domain.akadns.net.
arti-cache-geo-beta.net.
arti-cache-east2.net."

dig_aws_west_1_expect="arti-cache-beta.private-domain.akadns.net.
arti-cache-geo-beta.net.
arti-cache-west1.net."

dig_aws_west_2_expect="arti-cache-beta.private-domain.akadns.net.
arti-cache-geo-beta.net.
arti-cache-west2.net."

dig_on_prem_bjo_expect="arti-cache.private-domain.akadns.net.
arti-cache-gds.private-domain.akadns.net."

dig_on_prem_us_west_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-abc.elb.us-east-1.amazonaws.com.
"

dig_on_prem_eu_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-abc.elb.us-east-1.amazonaws.com.
"

dig_aws_sgp_1_expect="arti-cache.private-domain.akadns.net.
arti-cache-geo-beta.net.
arti-cache.apse1.net.
"

dig_aws_primary_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-abc.elb.us-east-1.amazonaws.com.
"

dig_res=$(dig arti-cache.private-domain +short |grep "$dig_expect")

if [ -n "$dig_res" ];then
    dig_status=1
else
    dig_status=0
fi

echo "# HELP arti_cache_dig_state arti cache dig state"
echo "# TYPE arti_cache_dig_state gauge"
echo "arti_cache_dig_state $dig_status"

wget -q https://arti-cache.private-domain/generic/ep/cicd/public/dummy.file -P /opt/node_exporter/artifactory/
if [ $? -eq 0 -a -f /opt/node_exporter/artifactory/dummy.file ];then
    cache_dwonload_status=1
    rm -rf /opt/node_exporter/artifactory/dummy.file
else
    cache_dwonload_status=0
fi
echo "# HELP arti_cache_download_state arti cache download state"
echo "# TYPE arti_cache_download_state gauge"
echo "arti_cache_download_state $cache_dwonload_status"
