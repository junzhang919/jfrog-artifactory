#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
import bitmath
import json, os

# reqUrl      = "http://localhost:8081/artifactory/api/plugins/execute/storageinfo"
# reqUrl      = "http://localhost:8081/artifactory/api/storageinfo"


def data_format(origin_data):

    origin_data = origin_data.replace(",", "")
    if "%" in origin_data:
        origin_data = origin_data.rsplit(" ",1)[0]
    if "bytes" in origin_data:
        origin_data = origin_data.rsplit(" ",1)[0]
        return float(origin_data)
    if 'KB' in origin_data:
        origin_data = origin_data.replace('KB','kB')

    parse_data = bitmath.parse_string(origin_data)
    new_data = float(parse_data.to_Byte().format("{value:.2f}"))
    return new_data


def fileStore(data):
    print('# HELP arti_file_store Filesystem size in bytes')
    print('# TYPE arti_file_store gauge')
    print('arti_file_store{key="used",storage_type="%s",storage_directory="%s"} %f' %
          (data['storageType'], data['storageDirectory'], data_format(data['usedSpace'])))
    print('arti_file_store{key="free",storage_type="%s",storage_directory="%s"} %f' %
          (data['storageType'], data['storageDirectory'], data_format(data['freeSpace'])))
    print('arti_file_store{key="total",storage_type="%s",storage_directory="%s"} %f' %
          (data['storageType'], data['storageDirectory'], data_format(data['totalSpace'])))

def binaries(data):
    print('# HELP arti_binaries storage')
    print('# TYPE arti_binaries gauge')
    print('arti_binaries{key="binaries_count"} %f' % (float(data['binariesCount'].replace(',',''))))
    print('arti_binaries{key="binaries_size"} %f' % (data_format(data['binariesSize'])))
    print('arti_binaries{key="artifacts_size"} %f' % (data_format(data['artifactsSize'])))
    # print('binaries{key="optimization"} %f' % (data['optimization']))
    print('arti_binaries{key="items_count"} %f' % (float(data['itemsCount'].replace(',',''))))
    print('arti_binaries{key="artifacts_count"} %f' % (float(data['artifactsCount'].replace(',',''))))

def repositories(data):
    print('# HELP arti_repo storage')
    print('# TYPE arti_repo gauge')
    for repo in data:
        if not 'packageType' in repo:
            repo['packageType'] = "NA"
        print('arti_repo{key="folders_count",repo_key="%s",repo_type="%s",package_type="%s"} %f' %
              (repo['repoKey'], repo['repoType'], repo['packageType'], float(repo['foldersCount'])))
        print('arti_repo{key="files_count",repo_key="%s",repo_type="%s",package_type="%s"} %f' %
              (repo['repoKey'], repo['repoType'], repo['packageType'], float(repo['filesCount'])))
        print('arti_repo{key="used_space",repo_key="%s",repo_type="%s",package_type="%s"} %f' %
              (repo['repoKey'], repo['repoType'], repo['packageType'], data_format(repo['usedSpace'])))
        print('arti_repo{key="items_count",repo_key="%s",repo_type="%s",package_type="%s"} %f' %
              (repo['repoKey'], repo['repoType'], repo['packageType'], float(repo['itemsCount'])))

def main():
    """ main """

    if os.system("systemctl is-enabled artifactory.service >/dev/null 2>&1") != 0:
        return

    try:
        storageinfo = open('/opt/node_exporter/artifactory/storageinfo.json', 'r+')
        ctx = json.load(storageinfo)
        fileStore(ctx['fileStoreSummary'])
        binaries(ctx['binariesSummary'])
        repositories(ctx['repositoriesSummaryList'])
    except:
        # except Exception as e:
        # print(e)
        return


if __name__ == "__main__":
    main()
