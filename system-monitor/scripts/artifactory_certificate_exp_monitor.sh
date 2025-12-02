#!/bin/bash
>/opt/node_exporter/artifactory_cert_metric.temp
exec >> /opt/node_exporter/artifactory_cert_metric.temp 2>&1
TARGET_LIST="arti-cache.private-domain"
echo "# HELP arti_cert_exp_state arti cert exp state"
echo "# TYPE arti_cert_exp_state gauge"
for TARGET in $TARGET_LIST; do
    expirationdate=$(date -d "$(: | openssl s_client -connect $TARGET:443 -servername $TARGET 2>/dev/null \
                                  | openssl x509 -text \
                                  | grep 'Not After' \
                                  |awk '{print $4,$5,$7}')" '+%s')
    now_date=$(date +%s)
    valid_days=$((($expirationdate - $now_date)/86400))
    echo "arti_cert_exp_state{target=\"$TARGET\"} $valid_days"
done

cp -rf /opt/node_exporter/artifactory_cert_metric.temp /opt/node_exporter/artifactory_cert_metric.prom
