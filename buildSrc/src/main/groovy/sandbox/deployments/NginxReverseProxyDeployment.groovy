package sandbox.deployments

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class NginxReverseProxyDeployment implements Iterable<Object> {

    private final V1Deployment nginxDeployment
    private final V1Service nginxService
    private final List<Object> backingListForIteration


    NginxReverseProxyDeployment(V1Deployment nginxDeployment, V1Service nginxService) {
        this.nginxDeployment = nginxDeployment
        this.nginxService = nginxService
        backingListForIteration = Arrays.asList(nginxDeployment, nginxService)
    }

    V1Deployment getNginxDeployment() {
        return nginxDeployment
    }

    V1Service getNginxService() {
        return nginxService
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


    static NginxReverseProxyDeployment buildReverseProxyDeployment(
            String regcred,
            String identifier,
            String nginxImageName,
            WebAppDeployment api,
            FrontEndDeployment frontend,
            String namespace,
            List<V1EnvVar> env = null
    ) {

        def reactHost = frontend.frontEndService.metadata.name
        def apiHost = api.webAppService.metadata.name
        def envVars = env ?: [ // custom via param or defaulting to React/Spring pair
                new V1EnvVarBuilder().withName("REACT_NODE_HOST").withValue(reactHost).build(),
                new V1EnvVarBuilder().withName("REACT_PORT").withValue(80.toString()).build(),
                new V1EnvVarBuilder().withName("SPRING_BOOT_HOST").withValue(apiHost).build(),
                new V1EnvVarBuilder().withName("SPRING_BOOT_PORT").withValue(80.toString()).build()
        ]

        def nginxDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName(identifier + "-nginxproxy")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: identifier + "-nginxproxy"])
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
                .withLabels([run: identifier + "-nginxproxy"])
                .endMetadata()
                .withNewSpec()
                .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(regcred).build())
                .addNewContainer()
                .withName(identifier + "-nginxproxy")
                .withImage("${nginxImageName}")
                .withImagePullPolicy("Always")
                .withPorts(
                        new V1ContainerPortBuilder().withName("nginxproxyhttp").withContainerPort(80).build(),
                )
                .withEnv(envVars)
                .withLivenessProbe(
                        new V1ProbeBuilder()
                                .withInitialDelaySeconds(120)
                                .withPeriodSeconds(10)
                                .withHttpGet(new V1HTTPGetActionBuilder()
                                        .withPort(new IntOrString(80))
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

        def nginxService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(identifier + "-nginx-http")
                .withLabels([run: identifier + "-nginx-http"])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder().withPort(80).withTargetPort(new IntOrString(80)).withProtocol("TCP").withName("http").build()
                ).withSelector([run: identifier + "-nginxproxy"])
                .endSpec()
                .build()

        return new NginxReverseProxyDeployment(nginxDeployment, nginxService)

    }
}