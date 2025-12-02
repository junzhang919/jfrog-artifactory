## Exposing Maven Indexes
- https://www.jfrog.com/confluence/display/RTF/Exposing+Maven+Indexes
- https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html

#### config (~/.m2/settings.xml)

```
grab rt config maven
```


#### make package and upload
```
cd demo
mvn package
mvn deploy

# open the url in your browser
# https://arti.private-domain/libs-release/ep/demo/
```



#### config demo/pom.xml (already done)
add a deployment element with the URL of a target local repository to which you want to deploy your artifaces.
add below into pom.xml
```xml
<project>
...
  <distributionManagement>
      <snapshotRepository>
          <id>snapshots</id>
          <name>artifacts-snapshots</name>
          <url>https://arti.private-domain/libs-snapshot</url>
      </snapshotRepository>
  </distributionManagement>
  <properties>
    <maven.compiler.source>1.7</maven.compiler.source>
    <maven.compiler.target>1.7</maven.compiler.target>
  </properties>
</project>
```



