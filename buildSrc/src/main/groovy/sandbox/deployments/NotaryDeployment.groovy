package sandbox.deployments

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.V1ContainerPortBuilder
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentBuilder
import io.kubernetes.client.openapi.models.V1EnvVarBuilder
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServiceBuilder
import io.kubernetes.client.openapi.models.V1ServicePortBuilder

import java.util.function.Consumer

class NotaryDeployment implements Iterable<Object> {

    private final V1Deployment deployment
    private final V1Service service
    private final List<Object> backingListForIteration = Arrays.asList(deployment, service)

    NotaryDeployment(V1Deployment deployment, V1Service service) {
        this.deployment = deployment
        this.service = service
    }

    V1Deployment getDeployment() {
        return deployment
    }

    V1Service getService() {
        return service
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

    static NotaryDeployment buildNotaryDeployment(String namespace,
                                                  String notaryId,
                                                  String notaryImage,
                                                  V1Service nmsService) {

        def deployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName(notaryId)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: notaryId])
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: notaryId])
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(notaryId)
                .withImage(notaryImage)
                .withImagePullPolicy("IfNotPresent")
                .withEnv(
                        new V1EnvVarBuilder().withName("PUBLIC_ADDRESS").withValue(notaryId).build(),
                        new V1EnvVarBuilder().withName("NETWORK_SERVICES_URL").withValue("http://${nmsService.metadata.name}:8080").build()
                )
                .withPorts(
                        new V1ContainerPortBuilder().withName("notaryport").withContainerPort(10200).build(),
                ).withNewResources()
                .withRequests("memory": new Quantity("1024Mi"), "cpu": new Quantity("0.5"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        def service = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(notaryId)
                .withLabels([run: notaryId])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder().withPort(10200).withProtocol("TCP").withName("notaryport").build()
                ).withSelector([run: notaryId])
                .endSpec()
                .build()


        return new NotaryDeployment(deployment, service)

    }

}
