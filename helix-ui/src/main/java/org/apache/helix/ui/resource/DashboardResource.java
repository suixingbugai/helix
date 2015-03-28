package org.apache.helix.ui.resource;

import com.google.common.collect.ImmutableList;
import org.apache.helix.manager.zk.ZKUtil;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;
import org.apache.helix.ui.api.*;
import org.apache.helix.ui.util.ClientCache;
import org.apache.helix.ui.util.DataCache;
import org.apache.helix.ui.view.ClusterView;
import org.apache.helix.ui.view.LandingView;
import org.apache.helix.ui.view.ResourceView;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Path("/dashboard")
@Produces(MediaType.TEXT_HTML)
public class DashboardResource {
    private static final List<String> REBALANCE_MODES = ImmutableList.of(
            IdealState.RebalanceMode.SEMI_AUTO.toString(),
            IdealState.RebalanceMode.FULL_AUTO.toString(),
            IdealState.RebalanceMode.CUSTOMIZED.toString(),
            IdealState.RebalanceMode.USER_DEFINED.toString(),
            IdealState.RebalanceMode.TASK.toString());

    private final boolean adminMode;
    private final ClientCache clientCache;
    private final DataCache dataCache;

    public DashboardResource(ClientCache clientCache,
                             DataCache dataCache,
                             boolean adminMode) {
        this.clientCache = clientCache;
        this.dataCache = dataCache;
        this.adminMode = adminMode;
    }

    @GET
    public LandingView getLandingView() {
        return new LandingView();
    }

    @GET
    @Path("/{zkAddress}")
    public ClusterView getClusterView(@PathParam("zkAddress") String zkAddress) throws Exception {
        clientCache.get(zkAddress); // n.b. will validate
        return getClusterView(zkAddress, null);
    }

    @GET
    @Path("/{zkAddress}/{cluster}")
    public ClusterView getClusterView(
            @PathParam("zkAddress") String zkAddress,
            @PathParam("cluster") String cluster) throws Exception {
        ClusterConnection clusterConnection = clientCache.get(zkAddress);

        // All clusters
        List<String> clusters = dataCache.getClusterCache().get(zkAddress);

        // The active cluster
        String activeCluster = cluster == null ? clusters.get(0) : cluster;
        ClusterSpec clusterSpec = new ClusterSpec(zkAddress, activeCluster);

        // Check it
        if (!ZKUtil.isClusterSetup(activeCluster, clusterConnection.getZkClient())) {
            return new ClusterView(adminMode, zkAddress, clusters, false, activeCluster, null, null, null, null, null);
        }

        // Resources in the active cluster
        List<String> activeClusterResources = dataCache.getResourceCache().get(clusterSpec);

        // Instances in active cluster
        List<InstanceSpec> instanceSpecs = dataCache.getInstanceCache().get(clusterSpec);

        // State models in active cluster
        List<String> stateModels
                = clusterConnection.getClusterSetup().getClusterManagementTool().getStateModelDefs(activeCluster);

        // Config table
        List<ConfigTableRow> configTable = dataCache.getConfigCache().get(clusterSpec);

        return new ClusterView(
                adminMode,
                zkAddress,
                clusters,
                true,
                activeCluster,
                activeClusterResources,
                instanceSpecs,
                configTable,
                stateModels,
                REBALANCE_MODES);
    }

    @GET
    @Path("/{zkAddress}/{cluster}/{resource}")
    public ResourceView getResourceView(
            @PathParam("zkAddress") String zkAddress,
            @PathParam("cluster") String cluster,
            @PathParam("resource") String resource) throws Exception {
        ClusterConnection clusterConnection = clientCache.get(zkAddress);

        // All clusters
        List<String> clusters = dataCache.getClusterCache().get(zkAddress);

        // The active cluster
        String activeCluster = cluster == null ? clusters.get(0) : cluster;
        ClusterSpec clusterSpec = new ClusterSpec(zkAddress, activeCluster);

        // Check it
        if (!ZKUtil.isClusterSetup(activeCluster, clusterConnection.getZkClient())) {
            return new ResourceView(
                    adminMode, zkAddress, clusters, false, activeCluster, null, null, null, null, null, null, null);
        }

        // Resources in the active cluster
        List<String> activeClusterResources = dataCache.getResourceCache().get(clusterSpec);
        if (!activeClusterResources.contains(resource)) {
            throw new NotFoundException("No resource " + resource + " in " + activeCluster);
        }

        // Instances in active cluster
        List<InstanceSpec> instanceSpecs = dataCache.getInstanceCache().get(clusterSpec);
        Map<String, InstanceSpec> instanceSpecMap = new HashMap<String, InstanceSpec>(instanceSpecs.size());
        for (InstanceSpec instanceSpec : instanceSpecs) {
            instanceSpecMap.put(instanceSpec.getInstanceName(), instanceSpec);
        }

        // Resource state
        IdealState idealState
                = clusterConnection.getClusterSetup().getClusterManagementTool().getResourceIdealState(cluster, resource);
        ExternalView externalView
                = clusterConnection.getClusterSetup().getClusterManagementTool().getResourceExternalView(cluster, resource);
        ResourceStateSpec resourceStateSpec
                = new ResourceStateSpec(resource, idealState, externalView, instanceSpecMap);
        List<ResourceStateTableRow> resourceStateTable
                = resourceStateSpec.getResourceStateTable();

        // Resource config
        List<ConfigTableRow> configTable = dataCache.getResourceConfigCache().get(new ResourceSpec(zkAddress, activeCluster, resource));

        // Resource instances
        Set<String> resourceInstances = new HashSet<String>();
        for (ResourceStateTableRow row : resourceStateTable) {
            resourceInstances.add(row.getInstanceName());
        }

        return new ResourceView(
                adminMode,
                zkAddress,
                clusters,
                true,
                activeCluster,
                activeClusterResources,
                resource,
                resourceStateTable,
                resourceInstances,
                configTable,
                IdealStateSpec.fromIdealState(idealState),
                instanceSpecs);
    }
}