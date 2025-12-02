#!/bin/sh
init_env=$1
arti_version=$2
cfg_backup_dir="/data1/efs/arti-${init_env}-env-backup"
if [ $# != 2 ] ; then
  echo "USAGE: $0 Parameter: Env Value beta|prd, Artifactory Version: 7.71.8"
  echo "e.g.: $0 beta 7.71.8"
  exit 1;
elif [ "$init_env" != "prd" ] && [ "$init_env" != "beta" ];then
  echo "Env Value Should Be: beta or prd"
  echo "e.g.: $0 beta 7.71.8"
  exit 1
fi
dnf install nfs-utils -y
[ -d /data1/efs ] || mkdir -p /data1/efs
df -h |grep "/data1/efs" || mount -t nfs4 -o rw,nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 fs-837cc9cb.efs.us-east-1.amazonaws.com:/ /data1/efs

if [ ! -d "$cfg_backup_dir" ];then
  echo "Can't found backup config folder: $cfg_backup_dir"
  exit 1
fi

#apply all updates
yum update
# install base tool
yum install -y telnet git python-pip gd gperftools-libs libxslt pcre htop
# install mysql cli
wget https://dev.mysql.com/get/mysql80-community-release-el9-1.noarch.rpm
rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
dnf install mysql80-community-release-el9-1.noarch.rpm -y
dnf install mysql-community-client -y

#PQM Monitor
#curl -s https://arti.private-domain/generic/pqm/exporters/node_exporter_install.sh | sudo bash

# install nginx: AL2 nginx-1.15.12-10.Linux.x86_64.rpm AL2023: nginx-1.15.12-11.Linux.x86_64.rpm
#AL2
#rpm -ivh https://arti.private-domain/artifactory/generic/ep/iaas/nginx-1.15.12-10.Linux.x86_64.rpm
#AL2023
#yum install -y pcre
rpm -ivh https://arti.private-domain/artifactory/generic/ep/iaas/nginx-1.15.12-11.Linux.x86_64.rpm

[ -d /opt/nginx/conf/conf.d/ ] || mkdir -p /opt/nginx/conf/conf.d/
cp "${cfg_backup_dir}"/nginx/conf/nginx.conf /opt/nginx/conf/nginx.conf
cp "${cfg_backup_dir}"/nginx/conf/conf.d/*.conf /opt/nginx/conf/conf.d/
systemctl start nginx.service
systemctl enable nginx.service
systemctl status nginx -l

# install armory or armory-cache
#rpm -ivh https://arti.private-domain/generic/ep/iaas/armory-0.2.0-3.Linux.x86_64.rpm
rpm -ivh https://arti.private-domain/generic/ep/iaas/armory_cache-0.2.0-9.Linux.x86_64.rpm
cp "${cfg_backup_dir}"/armory_cache/etc/* /opt/armory_cache/etc/
systemctl start armory_cache
systemctl enable armory_cache.service
systemctl status armory_cache -l

# service monitor env
update-alternatives --install /usr/bin/python python /usr/bin/python3.9 1
pip install 'urllib3<1.27,>=1.25.4' bitmath mysql-connector-python
#pip install urllib3==1.10.2 bitmath
[ -d /opt/node_exporter/artifactory/ ] || mkdir -p /opt/node_exporter/artifactory
[ -d /opt/node_exporter/scripts ] || mkdir -p /opt/node_exporter/scripts
cp "${cfg_backup_dir}"/node-exporter-scripts/* /opt/node_exporter/scripts/

#jfrog yum source
wget https://releases.jfrog.io/artifactory/artifactory-pro-rpms/artifactory-pro-rpms.repo -O /etc/yum.repos.d/jfrog-artifactory-pro-rpms.repo
# install artifactory
yum install -y jfrog-artifactory-pro-"${arti_version}" --disablerepo="*" --enablerepo="Artifactory-pro"

# restore rds connect driver
[ -d /opt/jfrog/artifactory/var/bootstrap/artifactory/tomcat/lib/ ] || mkdir -p /opt/jfrog/artifactory/var/bootstrap/artifactory/tomcat/lib/

[ -f "${cfg_backup_dir}/mysql-connector-java-8.0.15.jar" ] && cp "${cfg_backup_dir}/mysql-connector-java-8.0.15.jar" /opt/jfrog/artifactory/var/bootstrap/artifactory/tomcat/lib/

# restore java cacerts
[ -d /opt/jfrog/artifactory/app/third-party/java/lib/security/ ] && mkdir -p /opt/jfrog/artifactory/app/third-party/java/lib/security/
cp "${cfg_backup_dir}/cacerts" /opt/jfrog/artifactory/app/third-party/java/lib/security/cacerts

# restore master.key
[ -d /opt/jfrog/artifactory/var/etc/security/ ] || mkdir -p /opt/jfrog/artifactory/var/etc/security/
[ -f "${cfg_backup_dir}/master.key" ] && cp "${cfg_backup_dir}/master.key" /opt/jfrog/artifactory/var/etc/security/

# restore license and binarystore.xml files
[ -d /opt/jfrog/artifactory/var/etc/artifactory/plugins/ ] || mkdir -p /opt/jfrog/artifactory/var/etc/artifactory/plugins/
[ -f "${cfg_backup_dir}/artifactory.cluster.license" ] && cp ${cfg_backup_dir}/artifactory.cluster.license /opt/jfrog/artifactory/var/etc/artifactory/
[ -f "${cfg_backup_dir}/binarystore.xml" ] && cp "${cfg_backup_dir}/binarystore.xml" /opt/jfrog/artifactory/var/etc/artifactory/
echo "make sure the local cache dir is ready"
local_cache_dir=$(grep "cacheProviderDir" /opt/jfrog/artifactory/var/etc/artifactory/binarystore.xml |awk -F'[<>]' '/<cacheProviderDir>/ {print $3}')
if [ "${local_cache_dir}" == "cache" ];then
        mkdir -p /opt/jfrog/artifactory/var/data/artifactory/cache
else
        echo "create local cache dir ${local_cache_dir}"
        mkdir -p ${local_cache_dir}
        chown -R artifactory:artifactory ${local_cache_dir}
fi
sleep 60

# modify system.yaml to replace node ip and password info
[ -f "${cfg_backup_dir}/system.yaml" ] && cp "${cfg_backup_dir}/system.yaml" /opt/jfrog/artifactory/var/etc/
echo "Please modify the node IP and password in: /opt/jfrog/artifactory/var/etc/system.yaml"
sleep 90

# change file ownership
chown -R artifactory:artifactory /opt/jfrog/artifactory/
chown -R artifactory:artifactory /opt/jfrog/artifactory/var/

# start artifactory
systemctl start artifactory.service


# when all post check passed, enable auto service start
systemctl enable armory_cache nginx artifactory
# restore crontab jobs for root
rpm -qa |grep "cronie" || yum install -y cronie
[ -f "${cfg_backup_dir}/cron-root" ] && cp "${cfg_backup_dir}/cron-root" /var/spool/cron/root
