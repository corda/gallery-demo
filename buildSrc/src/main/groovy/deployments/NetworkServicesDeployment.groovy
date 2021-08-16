package deployments

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class NetworkServicesDeployment implements Iterable<Object> {
    private final V1Deployment nmsDeployment
    private final V1Service nmsService
    private final V1PersistentVolumeClaim persistencePVC
    private final List<Object> backingListForIteration = Arrays.asList(nmsDeployment, nmsService, persistencePVC)

    NetworkServicesDeployment(V1Deployment nmsDeployment, V1Service nmsService, V1PersistentVolumeClaim persistencePVC) {
        this.nmsDeployment = nmsDeployment
        this.nmsService = nmsService
        this.persistencePVC = persistencePVC
    }

    V1Deployment getNmsDeployment() {
        return nmsDeployment
    }

    V1Service getNmsService() {
        return nmsService
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

    static NetworkServicesDeployment buildNMSDeployment(String devNamespace, String networkServiceName, String imageName) {


        V1PersistentVolumeClaim persistencePVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("nms-persistence-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("1Gi")])
                .endResources()
                .endSpec()
                .build()

        def nmsDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName(networkServiceName)
                .withNamespace(devNamespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: networkServiceName])
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: networkServiceName])
                .endMetadata()
                .withNewSpec()
                .withVolumes(
                        new V1VolumeBuilder().withName("nms-persistence-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(persistencePVC.metadata.name)
                                ).build(),
                )
                .addNewContainer()
                .withName(networkServiceName)
                .withImage(imageName)
                .withImagePullPolicy("IfNotPresent")
                .withCommand("/start.sh")
                .withArgs("--minimumPlatformVersion=4")
                .withPorts(
                        new V1ContainerPortBuilder().withName("networkmapport").withContainerPort(8080).build()
                ).withNewResources()
                .withRequests("memory": new Quantity("512Mi"), "cpu": new Quantity("0.2"))
                .endResources()
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("nms-persistence-storage").withMountPath("/opt/node-storage").build(),
                )
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        def nmsService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName(networkServiceName)
                .withLabels([run: networkServiceName])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder().withPort(8080).withProtocol("TCP").withName("networkmapport").build()
                ).withSelector([run: networkServiceName])
                .endSpec()
                .build()

        return new NetworkServicesDeployment(nmsDeployment, nmsService, persistencePVC)
    }


}