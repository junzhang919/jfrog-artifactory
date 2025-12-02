#!/bin/bash

eu_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-abc.elb.us-east-1.amazonaws.com.
"

us_west_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-abc.elb.us-east-1.amazonaws.com.
"

function check_dns_resolve(){

    dns_name_server="$1"
    resolve_expect="$2"
    dc_name=$3
    dig_status=0
    for name_server in $dns_name_server;do
        dig_res=$(dig arti-cache.private-domain @${name_server} +short |grep "$resolve_expect")
        if [ -n "$dig_res" ];then
            dig_status=1
            break
        fi
    done
#    echo "arti_cache_dns_resolve_state{dc=\"$dc_name\"} $dig_status"


    echo arti_cache_dns_resolve_state $dig_status |curl --user ep:ep --data-binary @- http://db-proxy-push.pqm.net/prometheus/push/metrics/job/arti_cache_dns_resolve_state/Project/EP/Service/Artifactory/dc/$dc_name

}
#options='# OPTIONS labels={app="artifactory"}'
#echo "$options"
#echo "# HELP arti_cache_dns_resolve_state arti cache dig state"
#echo "# TYPE arti_cache_dns_resolve_state gauge"
# check bjo
check_dns_resolve "$bjo" "$bjo_expect" "bjo"
# check eu
check_dns_resolve "$eu_ams" "$eu_expect" "eu_ams"
check_dns_resolve "$eu_pa3" "$eu_expect" "eu_pa3"
# check us-west
# check_dns_resolve "$us_west_svl" "$us_west_expect" "us_west_svl"
check_dns_resolve "$us_west_lax" "$us_west_expect" "us_west_lax"
# check us-east-prd
check_dns_resolve "$us_east_ch3" "$us_east_prd_expect" "us_east_ch3"
# check_dns_resolve "$us_east_ash" "$us_east_prd_expect" "us_east_ash"
check_dns_resolve "$us_east_ny5_prd" "$us_east_prd_expect" "us_east_ny5_prd"
# check us-east-stg
check_dns_resolve "$us_east_ny5_stg" "$us_east_stg_expect" "us_east_ny5_stg"
