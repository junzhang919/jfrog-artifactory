# generic

## push file by grab
```
grab rt u generic ./README.md ep/demo/
```

## download file
```
# download file from cache server
grab rt dl generic -c ep/demo/README.md

# download file from primary server with extended source pattern
grab rt dl generic -e ep/demo/\*.md
```

## delete
```
grab rt delete generic/ep/demo/README.md

# use jfrog
grab rt delete -e generic/ep/demo/\*.md
```
