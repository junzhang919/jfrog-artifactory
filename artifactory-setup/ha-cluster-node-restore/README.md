## Note
This script is used to restore node for arti-beta and arti primary env. There will be no downtime during the operation.
Base AMI: GoldenImage-AWSBuilder-ServerBase-AL2023-x64-2024-10-29T10-26-36.259Z

## Steps
1. Back the current node config files to efs
   * exec the node_cfg_backup.sh with env parameter: beta/prod
2. Launch a new node: arti-ha04 with the same SG and Subnet configuration with the current node
3. Remove arti-ha01 from the HA cluster
4. Stop Artifactory service on arti-ha01 and disable service monitor 
5. Install Artifactory service in the new nodes and restore system configure files
   * exec the ha_cluster_node_restore.sh with env parameter: beta/prod and Artifactory Version: 7.71.8
6. Check the service status on new node 
7. Enable the service monitor on arti-ha04 
8. Add arti-ha04 to the HA cluster 
9. Test the upload and download functions from arti-ha04. 
10. Repeat steps 2-9 to replace arti-ha02/arti-ha03 nodes one by one. 
11. Stop the arti-ha01/arti-ha02/arti-ha03 nodes for cost saving 
12. Terminate the arti-ha01/arti-ha02/arti-ha03 nodes after 1 week of new cluster running.

## RollBack
If the new nodes can't work in the HA cluster, we just need to remove them from the HA cluster and then put the old nodes back into the cluster.

Remove the new node from the cluster's LoadBalancer.
* Stop the artifactory service on the new node.
* Start the artifactory service on the old node.
* Put the old node back into the LoadBalancer.
* Terminate the new node instance.





