package deployments

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class NetworkServicesDeployment implements Iterable<Object> {
    private final V1Deployment nmsDeployment
    private final V1Service nmsService
    private final List<Object> backingListForIteration = Arrays.asList(nmsDeployment, nmsService)

    NetworkServicesDeployment(V1Deployment nmsDeployment, V1Service nmsService) {
        this.nmsDeployment = nmsDeployment
        this.nmsService = nmsService
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

    static NetworkServicesDeployment buildNMSDeployment(String devNamespace, String networkServiceName) {
        def nmsDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName(networkServiceName+"-networkservices")
                .withNamespace(devNamespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: networkServiceName+"-networkservices"])
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: networkServiceName+"-networkservices"])
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(networkServiceName+"-networkservices")
                .withImage("roastario/notary-and-network-map:4.0")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("/start.sh")
                .withArgs("--minimumPlatformVersion=4")
                .withEnv(new V1EnvVarBuilder().withName("PUBLIC_ADDRESS").withValue(networkServiceName+"-networkservices").build())
                .withPorts(
                        new V1ContainerPortBuilder().withName("notaryport").withContainerPort(10200).build(),
                        new V1ContainerPortBuilder().withName("networkmapport").withContainerPort(8080).build()
                ).withNewResources()
                .withRequests("memory": new Quantity("512Mi"), "cpu": new Quantity("0.5"))
                .endResources()
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
                .withName(networkServiceName+"-networkservices")
                .withLabels([run: networkServiceName+"-networkservices"])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder().withPort(10200).withProtocol("TCP").withName("notaryport").build(),
                        new V1ServicePortBuilder().withPort(8080).withProtocol("TCP").withName("networkmapport").build()
                ).withSelector([run: networkServiceName+"-networkservices"])
                .endSpec()
                .build()

        return new NetworkServicesDeployment(nmsDeployment, nmsService)
    }


}