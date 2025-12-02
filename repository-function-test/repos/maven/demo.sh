#!/bin/bash -x

# config (~/.m2/settings.xml)
echo "Test ${repo} config"
grab rt config maven

# edit pom.xml
cd demo
sed -i "s/1.0-SNAPSHOT/1.0.$BUILD_NUMBER-SNAPSHOT/g" pom.xml
grab rt config maven-pom

# build package
mvn package

# deploy
echo "Test ${repo} upload"
mvn deploy

#echo "Test ${repo} delete"
#grab rt delete "libs-release/ep/demo/demo/1.0"
