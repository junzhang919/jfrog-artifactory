#!/bin/bash

bjo_expect="gdsarti02.fwmrm.net."
bjo="172.26.14.11 172.26.14.12"


eu_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-ab252bd450cd80aa.elb.us-east-1.amazonaws.com.
"
eu_ams="10.131.32.13 10.131.32.12 10.131.32.11"
eu_pa3="10.1.252.13 10.1.252.12 10.1.252.11"

# svl dc shutdown
#us_west_expect="arti-cache.private-domain.akadns.net.
#svlart01.fwmrm.net.
#10.1.96.70"

us_west_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-ab252bd450cd80aa.elb.us-east-1.amazonaws.com.
"
#us_west_svl="10.131.96.13 10.131.96.12 10.131.96.11"
us_west_lax="10.131.92.13 10.131.92.12 10.131.92.11"


us_east_ch3="10.1.188.13 10.1.188.12"
# ash has been shut down by ops at: 2022/04/01
#us_east_ash="10.131.2.13 10.131.2.11 10.131.2.12"
us_east_ny5_prd="10.131.4.13 10.131.4.12 10.131.4.11 10.131.20.13 10.131.20.11 10.131.20.12 10.131.10.13
10.131.10.12 10.131.10.11"

us_east_prd_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-ab252bd450cd80aa.elb.us-east-1.amazonaws.com.
"

us_east_ny5_stg="10.132.1.13 10.132.1.12 10.132.1.11"
us_east_stg_expect="arti-cache.private-domain.akadns.net.
arti-ha-tcp-ab252bd450cd80aa.elb.us-east-1.amazonaws.com.
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
