## rpm

#### push file by grab
```
grab rt u ./demo-0.0.1-1.x86_64.rpm rpm/ep/iaas/
```

#### download file
```
# download file from cache server
grab rt dl -c rpm/ep/iaas/demo-0.0.1-1.x86_64.rpm

# download file from primary server with extended source pattern
grab rt dl -e rpm/ep/iaas/\*.rpm
```

#### delete
```
grab rt delete rpm/ep/iaas/demo-0.0.1-1.x86_64.rpm
grab rt delete -e rpm/ep/iaas/\*.rpm
```


## install pkg
```
sudo grab rt config rpm-download
yum install -y demo --disablerepo="*" --enablerepo="fw"
rpm -q --info demo
```

