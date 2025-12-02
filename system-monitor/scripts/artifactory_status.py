#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
import requests
import os

reqUrl = "http://localhost:8082/router/api/v1/system/health"
reqTimeout = 2

if os.system("systemctl is-enabled artifactory.service >/dev/null 2>&1") != 0:
    os._exit(1)
header = {
    'Content-Type': 'application/json'
}
try:
    ctx = requests.get(reqUrl, headers=header, timeout=15)
    ctx = ctx.json()
    router = ctx['router']
    system_state = "1"
    router_state = router['state']
    if router_state != "HEALTHY":
        system_state = "0"
    else:
        services = ctx['services']
        for service in services:
            service_state = service['state']
            if service_state != "HEALTHY":
                system_state = '0'
                break
    print('# HELP arti_up up')
    print('# TYPE arti_up gauge')
    print('arti_up %s' % system_state)
except:
    print('# HELP arti_up up')
    print('# TYPE arti_up gauge')
    print('arti_up 0')