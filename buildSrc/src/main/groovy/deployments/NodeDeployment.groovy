package deployments


import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class NodeDeployment implements Iterable<Object> {

    public static Integer RPC_PORT = 10201
    public static Integer P2P_PORT = 10200
    public static String RPC_PASSWORD = "test"

    private final V1PersistentVolumeClaim certsPVC
    private final V1PersistentVolumeClaim configPVC
    private final V1PersistentVolumeClaim persistencePVC
    private final V1PersistentVolumeClaim logsPVC
    private final V1Deployment nodeDeployment
    private final V1Service nodeService
    private final List<Object> backingListForIteration = Arrays.asList(certsPVC, configPVC, persistencePVC, logsPVC, nodeDeployment, nodeService)

    NodeDeployment(V1PersistentVolumeClaim certsPVC,
                          V1PersistentVolumeClaim configPVC,
                          V1PersistentVolumeClaim persistencePVC,
                          V1PersistentVolumeClaim logsPVC,
                          V1Deployment nodeDeployment,
                          V1Service nodeService) {
        this.persistencePVC = persistencePVC
        this.certsPVC = certsPVC
        this.configPVC = configPVC
        this.nodeDeployment = nodeDeployment
        this.nodeService = nodeService
        this.logsPVC = logsPVC
    }

    V1PersistentVolumeClaim getCertsPVC() {
        return certsPVC
    }

    V1PersistentVolumeClaim getConfigPVC() {
        return configPVC
    }

    V1PersistentVolumeClaim getPersistencePVC() {
        return persistencePVC
    }

    V1Deployment getNodeDeployment() {
        return nodeDeployment
    }

    V1Service getNodeService() {
        return nodeService
    }

    @Override
    Iterator<Object> iterator() {
        return backingListForIteration.iterator()
    }

    @Override
    void forEach(Consumer<? super Object> action) {
        backingListForIteration.forEach(action)
    }

    @Override
    Spliterator<Object> spliterator() {
        return backingListForIteration.spliterator()
    }

    static NodeDeployment buildNodeDeployment(String regcred,
                                              String devNamespace,
                                              String targetNetworkService,
                                              String identifier,
                                              String x500,
                                              String imageName,
                                              String randomSuffix) {
        def dnsSafeIdentifier = identifier.toLowerCase()

        def nodeComponents = NodeResources.createNodeComponents(devNamespace, dnsSafeIdentifier, x500, imageName)

        V1PersistentVolumeClaim certPVC = nodeComponents.get(0) as V1PersistentVolumeClaim
        V1PersistentVolumeClaim persistencePVC = nodeComponents.get(1) as V1PersistentVolumeClaim
        V1PersistentVolumeClaim nodeConfigPVC = nodeComponents.get(2) as V1PersistentVolumeClaim
        V1PersistentVolumeClaim nodeLogsPVC = nodeComponents.get(3) as V1PersistentVolumeClaim
        V1Service nodeService = nodeComponents.get(4) as V1Service

        V1Deployment nodeDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName("$dnsSafeIdentifier-node")
                .withNamespace(devNamespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: "$dnsSafeIdentifier-node".toString()])
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: "$dnsSafeIdentifier-node".toString()])
                .endMetadata()
                .withNewSpec()
                .withNewSecurityContext()
                .withFsGroup(1000)
                .endSecurityContext()
                .withVolumes(
                        new V1VolumeBuilder().withName("$dnsSafeIdentifier-config-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(nodeConfigPVC.metadata.name)
                                ).build(),
                        new V1VolumeBuilder().withName("$dnsSafeIdentifier-certificates-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(certPVC.metadata.name)
                                ).build(),
                        new V1VolumeBuilder().withName("$dnsSafeIdentifier-persistence-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(persistencePVC.metadata.name)
                                ).build(),
                        new V1VolumeBuilder().withName("$dnsSafeIdentifier-logs-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(nodeLogsPVC.metadata.name)
                                ).build()
                )
                .addNewInitContainer()
                .withName("chown-certificates")
                .withImage("busybox:latest")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("chown")
                .withArgs("-Rv", "1000:1000", "/opt/corda/certificates", "/etc/corda", "/opt/corda/persistence", "/opt/corda/logs")
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-config-storage").withMountPath("/etc/corda").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-certificates-storage").withMountPath("/opt/corda/certificates").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-persistence-storage").withMountPath("/opt/corda/persistence").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-logs-storage").withMountPath("/opt/corda/logs").build(),
                )
                .endInitContainer()
                .addNewInitContainer()
                .withName("download-truststore")
                .withImage("curlimages/curl:latest")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("curl")
                .withArgs("-o", "/opt/corda/certificates/network-root-truststore.jks", "http://$targetNetworkService:8080/trustStore")
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-config-storage").withMountPath("/etc/corda").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-certificates-storage").withMountPath("/opt/corda/certificates").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-persistence-storage").withMountPath("/opt/corda/persistence").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-logs-storage").withMountPath("/opt/corda/logs").build()
                )
                .endInitContainer()
                .addNewInitContainer()
                .withName("initial-registration")
                .withImage("${imageName}:${randomSuffix}")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("config-generator")
                .withArgs("--generic", "--exit-on-generate")
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-config-storage").withMountPath("/etc/corda").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-certificates-storage").withMountPath("/opt/corda/certificates").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-persistence-storage").withMountPath("/opt/corda/persistence").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-logs-storage").withMountPath("/opt/corda/logs").build()
                )
                .withEnv(
                        new V1EnvVarBuilder().withName("MY_LEGAL_NAME").withValue(x500).build(),
                        new V1EnvVarBuilder().withName("MY_PUBLIC_ADDRESS").withValue("$dnsSafeIdentifier-node").build(),
                        new V1EnvVarBuilder().withName("NETWORKMAP_URL").withValue("http://$targetNetworkService:8080").build(),
                        new V1EnvVarBuilder().withName("DOORMAN_URL").withValue("http://$targetNetworkService:8080").build(),
                        new V1EnvVarBuilder().withName("NETWORK_TRUST_PASSWORD").withValue("trustpass").build(),
                        new V1EnvVarBuilder().withName("MY_EMAIL_ADDRESS").withValue("$dnsSafeIdentifier@rtree.com").build(),
                        new V1EnvVarBuilder().withName("RPC_PASSWORD").withValue(RPC_PASSWORD).build(),
                        new V1EnvVarBuilder().withName("MY_RPC_PORT").withValue("${RPC_PORT}").build(),
                        new V1EnvVarBuilder().withName("SSHPORT").withValue("2222").build(),
                        new V1EnvVarBuilder().withName("RPC_USER").withValue("rpcUser").build(),
                )
                .withNewResources()
                .endResources()
                .endInitContainer()
                .addNewInitContainer()
                .withName("run-migration")
                .withImage("${imageName}:${randomSuffix}")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("bash")
                .withArgs("run-dbmigration.sh")
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-config-storage").withMountPath("/etc/corda").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-certificates-storage").withMountPath("/opt/corda/certificates").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-persistence-storage").withMountPath("/opt/corda/persistence").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-logs-storage").withMountPath("/opt/corda/logs").build()
                )
                .withNewResources()
                .endResources()
                .endInitContainer()
                .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(regcred).build())
                .addNewContainer()
                .withName("$dnsSafeIdentifier-node")
                .withImage("${imageName}:${randomSuffix}")
                .withImagePullPolicy("IfNotPresent")
                .withPorts(
                        new V1ContainerPortBuilder().withName("p2pport").withContainerPort(P2P_PORT).build(),
                        new V1ContainerPortBuilder().withName("rpcport").withContainerPort(RPC_PORT).build(),
                        new V1ContainerPortBuilder().withName("debugport").withContainerPort(5000).build(),
                )
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-config-storage").withMountPath("/etc/corda").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-certificates-storage").withMountPath("/opt/corda/certificates").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-persistence-storage").withMountPath("/opt/corda/persistence").build(),
                        new V1VolumeMountBuilder().withName("$dnsSafeIdentifier-logs-storage").withMountPath("/opt/corda/logs").build()
                )
                .withEnv(
                        new V1EnvVarBuilder().withName("CORDA_ARGS").withValue("--log-to-console").build(),
                        new V1EnvVarBuilder().withName("JVM_ARGS").withValue("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5000 -XX:MaxHeapFreeRatio=40").build()
                )
                .withNewResources()
                .withRequests("memory": new Quantity("3072Mi"), "cpu": new Quantity("0.5"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        return new NodeDeployment(certPVC, nodeConfigPVC, persistencePVC, nodeLogsPVC, nodeDeployment, nodeService)
    }
}