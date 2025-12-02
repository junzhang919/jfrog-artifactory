#!/usr/bin/env python
import json
import requests
import os

def append_to_file(filename, content):
    with open(filename, 'a') as file:
        if isinstance(content, dict):
            content = json.dumps(content)
        file.write(content + '\n')


if __name__ == "__main__":
    api_key = os.getenv("API_KEY")
    headers = {
        'Content-Type': 'application/json'
    }
    k8s_cluster_query = F"https://api.private-domainapi.com/api/v1/k8sinfra/clusters/all?apikey={private-domainapi_key}"
    k8s_response = requests.request("GET", k8s_cluster_query, headers=headers)
    k8s_res_obj = k8s_response.json()['output']
    aws_account_regions = {}
    artifacts_list = []
    artifacts_path_list = []
    arti_usage = "no"
    for cluster_details in k8s_res_obj:
        udc = cluster_details['udc']
        dc = cluster_details['dc']
        clusterName = cluster_details['clusterName']
        namespace = cluster_details['namespace']
        k8s_pods_query = F"https://private-domainapi.com/api/v2/k8sworkload/namespaces/{namespace}/udcs/{udc}/dcs/{dc}/clusters/{clusterName}/ns/all/images/remote?apikey={api_key}"
        k8s_pods_response = requests.request("GET", k8s_pods_query, headers=headers)
        status_code = k8s_pods_response.status_code
        if status_code != 200:
            print(F"{udc} {dc} {clusterName} Failed {k8s_pods_response.text}")
            continue
        k8s_pods_obj = k8s_pods_response.json()['output']
        for pod_details in k8s_pods_obj:
            image_name = pod_details['name'].replace("arti-cache", "arti")
            if "arti" in image_name:
                arti_usage = "yes"
                image_path = image_name.split(":")[0]
                if image_name not in artifacts_list:
                    artifacts_list.append(image_name)
                    append_to_file("artifactory_images.txt", image_name)
                if image_path not in artifacts_path_list:
                    artifacts_path_list.append(image_path)
                    append_to_file("artifactory_images_paths.txt", image_path)

        if arti_usage == "yes":
            if dc not in aws_account_regions:
                dc_account_list = [udc]
                aws_account_regions[dc] = dc_account_list
            else:
                if udc not in aws_account_regions[dc]:
                    aws_account_regions[dc].append(udc)

    append_to_file("artifactory_aws_region_usage.txt", aws_account_regions)
