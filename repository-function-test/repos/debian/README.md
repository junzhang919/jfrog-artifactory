# debian repositories
- https://www.jfrog.com/confluence/display/RTF/Debian+Repositories

## upload
```
grab rt upload debian demo-0.0.1-1.amd64.deb ep/demo/ \
	--distribution=fw --component=private \
	--architecture=amd64 --debug
```

## download
```
grab rt download debian ep/demo/demo-0.0.1-1.amd64.deb /tmp/
```

## install pacakge by apt-get

add artifactory to sources.list
```
cat > /etc/apt/apt.conf.d/artifacts <<EOF
Acquire::https::arti.private-domain::Verify-Peer "false";
EOF

cat >> /etc/apt/sources.list <<EOF
deb [trusted=yes] https://arti.private-domain/debian fw private
EOF

apt-get update
apt-get install -t fw demo
```


## use artifactory instead of http://archive.ubuntu.com/ubuntu
```
sed -i -e 's/http:\/\/archive\.ubuntu\.com\/ubuntu/[trusted=yes] https:\/\/arti.private-domain\/debian/g' /etc/apt/sources.list
sed -i -e 's/http:\/\/security\.ubuntu\.com\/ubuntu/[trusted=yes] http:\/\/arti.private-domain\/debian/g' /etc/apt/sources.list
apt-get update
```
