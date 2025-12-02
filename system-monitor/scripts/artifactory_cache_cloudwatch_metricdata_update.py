#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
import requests
import os
import time

reqUrl = "http://localhost:8082/router/api/v1/system/health"
reqTimeout = 3

header = {
    'Content-Type': 'application/json'
}

def run_command_with_retry(command, retries=3, delay=2):
    for attempt in range(retries):
        result = os.system(command)
        if result == 0:
            return
        else:
            time.sleep(delay)
            delay *= 2

if __name__ == "__main__":

    try:
        ctx = requests.get(reqUrl, headers=header, timeout=reqTimeout)
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
                    system_state = "0"
                    break
    except:
        system_state = "0"

    # replace the Region_Value of Arti-Cache-Region_Value-HealthCheck-Status to the related values, such as West-2/West-1/East-2/East-2
    command = F"aws cloudwatch put-metric-data --metric-name Arti-Cache-Region_Value-HealthCheck-Status --namespace 'HealthCheck' --value {system_state} --unit Count >/dev/null 2>&1"
    run_command_with_retry(command)
