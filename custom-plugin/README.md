# jfrog-artifactory
    该架构是基于路径的权限验证，通过管理CN和PATH的对应关系，通过SCOPE详细控制每个路径的读写权限。
    Cache节点只需要通过Edge节点搭建，减少Cost，同时只根据下载请求，只缓存有效artifacts，减少存储花费。
