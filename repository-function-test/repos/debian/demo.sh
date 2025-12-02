#!/bin/bash -x
file=$(/bin/ls *.deb)

# upload
echo "Test ${repo} upload"
grab rt upload debian ${file} ep/demo/ \
	--distribution=fw \
	--component=private \
	--architecture=amd64

# download
echo "Test ${repo} download"
grab rt download debian ep/demo/${file} /tmp/

# delete
echo "Test ${repo} delete"
grab rt delete debian/ep/demo/${file}

# install
#docker run --rm -it -w /root \
#	-v ${PWD}/docker.sh:/root/docker.sh \
#	arti.private-domain/ubuntu:19.04 /root/docker.sh