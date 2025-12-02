#!/bin/sh
init_env=$1
if [ $# != 1 ] ; then
  echo "USAGE: $0 Parameter: Env Value beta|prd"
  echo "e.g.: $0 beta"
  exit 1;
elif [ "$init_env" != "prd" ] && [ "$init_env" != "beta" ];then
  echo "Env Value Should Be: beta or prd"
  echo "e.g.: $0 beta"
  exit 1
fi
cfg_backup_dir="/data1/efs/arti-${init_env}-env-backup"
[ -d "${cfg_backup_dir}" ] || mkdir -p "$cfg_backup_dir"
df -h |grep "/data1/efs" || mount -t nfs4 -o rw,nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 fs-837cc9cb.efs.us-east-1.amazonaws.com:/ /data1/efs
# backup nginx
[ -d "${cfg_backup_dir}/nginx/conf/conf.d/" ] || mkdir -p "${cfg_backup_dir}"/nginx/conf/conf.d/
cp /opt/nginx/conf/nginx.conf "${cfg_backup_dir}"/nginx/conf/nginx.conf
cp /opt/nginx/conf/conf.d/*.conf "${cfg_backup_dir}"/nginx/conf/conf.d/


# backup armory-cache
[ -d "${cfg_backup_dir}"/armory_cache/etc ] || mkdir -p "${cfg_backup_dir}"/armory_cache/etc
cp /opt/armory_cache/etc/* "${cfg_backup_dir}"/armory_cache/etc/

# backup monitor script
[ -d "${cfg_backup_dir}/node-exporter-scripts" ] || mkdir -p "${cfg_backup_dir}/node-exporter-scripts"
cp /opt/node_exporter/scripts/* "${cfg_backup_dir}"/node-exporter-scripts/


# backup rds connect driver
[ -f "/opt/jfrog/artifactory/var/bootstrap/artifactory/tomcat/lib/mysql-connector-java-8.0.15.jar" ] && cp /opt/jfrog/artifactory/var/bootstrap/artifactory/tomcat/lib/mysql-connector-java-8.0.15.jar "${cfg_backup_dir}"

# backup java cacerts
[ -f /opt/jfrog/artifactory/app/third-party/java/lib/security/cacerts ] && cp /opt/jfrog/artifactory/app/third-party/java/lib/security/cacerts "${cfg_backup_dir}/cacerts"

# backup master.key
cp /opt/jfrog/artifactory/var/etc/security/master.key "${cfg_backup_dir}/master.key"

# backup license and binarystore.xml files

cp /opt/jfrog/artifactory/var/etc/artifactory/artifactory.cluster.license ${cfg_backup_dir}/
cp /opt/jfrog/artifactory/var/etc/artifactory/binarystore.xml ${cfg_backup_dir}/

# backup crontab jobs for root
[ -f /var/spool/cron/root ] && cp /var/spool/cron/root "${cfg_backup_dir}/cron-root"
