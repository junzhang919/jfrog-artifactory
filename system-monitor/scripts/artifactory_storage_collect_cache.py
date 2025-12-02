#!/usr/bin/env python
# -*- coding: utf-8 -*-
#

import json
import requests
import base64

reqUrl = "http://localhost:8081/artifactory/api/storageinfo"
# need admin user
# username = ""
# password = ""
# b64auth = base64.b64encode("%s:%s" % (username,password))
primary_b64auth = "YWRtaW46MDVkMDNkMGNmMGY1ZGQwNDFkMzgzMTMxNzliMmYzOGE="
cache_b64auth = "YWRtaW46RnIzMzIwMjEh"

header = {
    'Content-Type': 'application/json',
    'Authorization': 'Basic {}'.format(cache_b64auth)
}

try:
    ctx = requests.get(reqUrl, headers=header, timeout=15)
    ctx = ctx.json()
    with open("/opt/node_exporter/artifactory/storageinfo.json", "w") as outfile:
        json.dump(ctx, outfile)
except Exception as e:
    print(e)
