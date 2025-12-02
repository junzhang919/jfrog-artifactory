# Helm Chart Repositories
- https://www.jfrog.com/confluence/display/RTF/Helm+Chart+Repositories

## push file by grab
```
grab rt u helm ./nginx-0.1.0.tgz ep/demo/
```

## download file
```
# download file from cache server
grab rt dl helm -c ep/demo/nginx-0.1.0.tgz

# download file from primary server with extended source pattern
grab rt dl helm -e ep/demo/\*.tgz
```

## delete
```
grab rt delete helm/ep/demo/nginx-0.1.0.tgz

# use jfrog
grab rt delete -e helm/ep/demo/\*.tgz
```
