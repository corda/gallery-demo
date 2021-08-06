package sandbox.deployments

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

class NodeResources {

    public static Integer RPC_PORT = 10201
    public static Integer P2P_PORT = 10200

    static List<Object> createNodeComponents(String devNamespace,
                                             String dnsSafeIdentifier,
                                             String x500,
                                             String imageName) {

        V1PersistentVolumeClaim certPVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("$dnsSafeIdentifier-node-certificate-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("1Gi")])
                .endResources()
                .endSpec()
                .build()

        V1PersistentVolumeClaim persistencePVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("$dnsSafeIdentifier-node-persistence-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("50Gi")])
                .endResources()
                .endSpec()
                .build()

        V1PersistentVolumeClaim nodeConfigPVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("$dnsSafeIdentifier-node-config-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("1Gi")])
                .endResources()
                .endSpec()
                .build()

        V1PersistentVolumeClaim nodeLogsPVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("$dnsSafeIdentifier-node-log-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("10Gi")])
                .endResources()
                .endSpec()
                .build()

        V1Service nodeService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("$dnsSafeIdentifier-node")
                .withLabels([run: "$dnsSafeIdentifier-node".toString()])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder().withPort(P2P_PORT).withProtocol("TCP").withName("p2pport").build(),
                        new V1ServicePortBuilder().withPort(RPC_PORT).withProtocol("TCP").withName("rpcport").build(),
                        new V1ServicePortBuilder().withPort(5000).withProtocol("TCP").withName("debugport").build(),
                ).withSelector([run: "$dnsSafeIdentifier-node".toString()])
                .endSpec()
                .build()

        return [certPVC, persistencePVC, nodeConfigPVC, nodeLogsPVC, nodeService]
    }
}