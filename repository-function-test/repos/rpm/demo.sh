#!/bin/bash -eax

file=demo-0.0.1-1.x86_64.rpm

# delete
echo "Test ${repo} delete"
grab rt del rpm/ep/demo/${file}

# upload
echo "Test ${repo} upload"
grab rt u rpm ${file} ep/demo/${file}

# download
echo "Test ${repo} download"
grab rt dl rpm/ep/demo/${file} /tmp/

# config yum repo & install
#docker run --rm -it -w /root \
#	-e GRAB_USER=${GRAB_USER} \
#	-e GRAB_API_KEY=${GRAB_API_KEY} \
#	-v $(which grab):/bin/grab \
#	-v ${PWD}/docker.sh:/root/docker.sh \
#	arti.private-domain/centos:7.6.1810 /root/docker.sh



