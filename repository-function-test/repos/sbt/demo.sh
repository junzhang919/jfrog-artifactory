#!/bin/bash -x

export SBT_OPTS="-Xms200M -Xmx500M -Xss2M -XX:MaxMetaspaceSize=600M"

grab rt config sbt

# edit build.sbt
cd demo
sed -i "s/0.0.1/0.0.$BUILD_NUMBER/g" build.sbt
# deploy
sbt publish