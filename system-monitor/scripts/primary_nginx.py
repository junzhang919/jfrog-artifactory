#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
import os
import json
import requests
import re

reqUrl = 'http://localhost/status/format/json'
comp_key = re.compile('[^A-Za-z1-9_\.\*\-]')
header = {
    'Content-Type': 'application/json'
}


def clean_key(text):
    return comp_key.sub('', text)


def connections_handle(data):
    print('# HELP nginx_connections Total connections and requests(same as stub_status_module in NGINX)')
    print('# TYPE nginx_connections gauge')
    for k, v in data.items():
        print('nginx_connections{key="%s"} %f' % (k, v))

def shared_zones_handle(data):
    print('# HELP nginx_shared_zones The shared memory information using in nginx-module-vts.')
    print('# TYPE nginx_shared_zones gauge')
    print('nginx_shared_zones{name="%s", key="max_size"} %f' % (data['name'], data['maxSize']))
    print('nginx_shared_zones{name="%s", key="used_size"} %f' % (data['name'], data['usedSize']))
    print('nginx_shared_zones{name="%s", key="used_node"} %f' % (data['name'], data['usedNode']))
	
def server_zones_handle(data):
    print('# HELP nginx_server_zones Traffic(in/out) and request and response counts and cache hit ratio per each server zone')
    print('# TYPE nginx_server_zones gauge')
    for k, v in data.items():
        server = clean_key(k)
        print('nginx_server_zones{server="%s", key="request_counter"} %f' % (server, v['requestCounter']))
        print('nginx_server_zones{server="%s", key="in_bytes"} %f' % (server, v['inBytes']))
        print('nginx_server_zones{server="%s", key="out_bytes"} %f' % (server, v['outBytes']))
        print('nginx_server_zones{server="%s", key="request_msec_counter"} %f' % (server, v['requestMsecCounter']))
        print('nginx_server_zones{server="%s", key="request_msec"} %f' % (server, v['requestMsec']))
        for k2, v2 in v['responses'].items():
            print('nginx_server_zones{server="%s", key="responses_%s"} %f' % (server, k2, v2))
        #print('server_zones{server="%s", key="request_msecs"} %f' % (server, v['requestMsecs']))
        #print('server_zones{server="%s", key="request_buckets"} %f' % (server, v['requestBuckets']))

def filter_zones_handle(data):
    print('# HELP nginx_filter_zones Traffic(in/out) and request and response counts and cache hit ratio per each server zone filtered through the vhost_traffic_status_filter_by_set_key directive')
    print('# TYPE nginx_filter_zones gauge')

def upstream_zones_handle(data):
    print('# HELP nginx_upstream_zones Traffic(in/out) and request and response counts per server in each upstream group')
    print('# TYPE nginx_upstream_zones gauge')
    for k, v in data.items():
    	for v2 in v:
            server = v2['server']
            print('nginx_upstream_zones{upstream="%s", server="%s", key="request_counter"} %f' % (k, server, v2['requestCounter']))
            print('nginx_upstream_zones{upstream="%s", server="%s", key="in_bytes"} %f' % (k, server, v2['inBytes']))
            print('nginx_upstream_zones{upstream="%s", server="%s", key="out_bytes"} %f' % (k, server, v2['outBytes']))
            print('nginx_upstream_zones{upstream="%s", server="%s", key="request_msec_counter"} %f' % (k, server, v2['requestMsecCounter']))
            print('nginx_upstream_zones{upstream="%s", server="%s", key="request_msec"} %f' % (k, server, v2['requestMsec']))
            for k3, v3 in v2['responses'].items():
                print('nginx_upstream_zones{upstream="%s", server="%s", key="responses_%s"} %f' % (k, server, k3, v3))
            #print('upstream_zones{server="%s", key="request_msecs"} %f' % (k, v['requestMsecs']))

def cache_zones_handle(data):
    print('# HELP nginx_cache_zones Traffic(in/out) and size(capacity/used) and hit ratio per each cache zone when using the proxy_cache directive.')
    print('# TYPE nginx_cache_zones gauge')

def main():
    """ main """

    if os.system("systemctl is-enabled nginx.service >/dev/null 2>&1") != 0:
        return

    ctx = requests.get(reqUrl, headers=header, timeout=15)
    try:
        ctx = ctx.json()
        #ctx = json.loads(open("sample.json").read())

        connections_handle(ctx['connections'])
        if 'sharedZones' in ctx:
            shared_zones_handle(ctx['sharedZones'])

        if 'serverZones' in ctx:
            server_zones_handle(ctx['serverZones'])

        #if 'filterZones' in ctx:
        #    filter_zones_handle(ctx['filterZones'])

        if 'upstreamZones' in ctx:
            upstream_zones_handle(ctx['upstreamZones'])

        #if 'cacheZones' in ctx:
        #    cache_zones_handle(ctx['cacheZones'])

        print('# HELP nginx_up up')
        print('# TYPE nginx_up gauge')
        print('nginx_up 1')
    except:
        print('# HELP nginx_up up')
        print('# TYPE nginx_up gauge')
        print('nginx_up 0')
 
if __name__ == "__main__":
    main()
