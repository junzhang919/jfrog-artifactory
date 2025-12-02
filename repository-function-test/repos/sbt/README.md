## Exposing Maven Indexes
- https://www.jfrog.com/confluence/display/RTF/Exposing+Maven+Indexes
- https://www.jfrog.com/confluence/display/JFROG/SBT+Repositories

#### config (~/.sbt/repositories and ~/.sbt/.credentials)

```
grab rt config sbt
```


#### make package and upload

```
cd demo

# init context need to append into sbt project ./build.sbt
grab rt config sbt-build 

sbt package

sbt publish

# open the url in your browser
# https://arti.private-domain/sbt/ep/demo/
```




