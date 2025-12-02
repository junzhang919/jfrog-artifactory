## Go Registry

Artifactory go registry is baes on golang module

- https://www.jfrog.com/confluence/display/RTF/Go+Registry
- https://github.com/jfrog/project-examples/tree/master/golang-example


#### config
```
grab rt config jfrog
```

#### build
```
cd hello
jfrog rt go build go --build-name=demo --build-number=1 
```


#### push:
```
jfrog rt gp go v1.0.0 --build-name=demo --build-number=1
```

#### pushinfo:
```
jfrog rt bce demo 1
jfrog rt bp demo 1
```
