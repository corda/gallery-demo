package deployments

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class FrontEndDeployment implements Iterable<Object> {
    private final V1Deployment frontEndDeployment
    private final V1Service frontEndService
    private final List<Object> backingListForIteration

    FrontEndDeployment(V1Deployment frontEndDeployment, V1Service frontEndService) {
        this.frontEndDeployment = frontEndDeployment
        this.frontEndService = frontEndService
        backingListForIteration = Arrays.asList(frontEndDeployment, frontEndService)
    }

    V1Deployment getFrontEndDeployment() {
        return frontEndDeployment
    }

    V1Service getFrontEndService() {
        return frontEndService
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

    static FrontEndDeployment buildFrontEndDeployment(String regcred,
                                                      String identifier,
                                                      String imageName,
                                                      String namespace
    ) {

        def frontendDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName(identifier + "-frontend")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: identifier + "-frontend"])
                .endSelector()
                .withReplicas(1)
                .withNewStrategy()
                .withType("RollingUpdate")
                .withNewRollingUpdate()
                .withMaxSurge(new IntOrString(0))
                .withMaxUnavailable(new IntOrString(1))
                .endRollingUpdate()
                .endStrategy()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: identifier + "-frontend"])
                .endMetadata()
                .withNewSpec()
                .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(regcred).build())
                .addNewContainer()
                .withName(identifier + "-frontend")
                .withImage("${imageName}")
                .withImagePullPolicy("Always")
                .withPorts(
                        new V1ContainerPortBuilder().withName("frontendhttp").withContainerPort(6005).build(),
                )
                .withEnv(
                        new V1EnvVarBuilder().withName("PARTICIPANT_ROLE").withValue(identifier).build()
                ).withLivenessProbe(
                        new V1ProbeBuilder()
                                .withInitialDelaySeconds(120)
                                .withPeriodSeconds(10)
                                .withHttpGet(new V1HTTPGetActionBuilder()
                                        .withPort(new IntOrString(6005))
                                        .withPath("/")
                                        .build()
                                ).build())
                .withNewResources()
                .withRequests("memory": new Quantity("100Mi"), "cpu": new Quantity("0.1"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        def frontendHttpService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(identifier + "-frontend-http")
                .withLabels([run: identifier + "-frontend-http"])
                .endMetadata()
                .withNewSpec()
                .withType("ClusterIP")
                .withPorts(
                        new V1ServicePortBuilder().withPort(80).withTargetPort(new IntOrString(6005)).withProtocol("TCP").withName("http").build()
                ).withSelector([run: identifier + "-frontend"])
                .endSpec()
                .build()

        return new FrontEndDeployment(frontendDeployment, frontendHttpService)
    }
}