package deployments

import io.kubernetes.client.custom.IntOrString
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.*

import java.util.function.Consumer

class WebAppDeployment implements Iterable<Object> {

    private final V1Deployment webAppDeployment
    private final V1Service webAppService
    private final V1Service webappDebugService
    private final List<Object> backingListForIteration

    WebAppDeployment(V1Deployment webAppDeployment, V1Service webAppService, V1Service webAppDebugService) {
        this.webAppDeployment = webAppDeployment
        this.webAppService = webAppService
        this.webappDebugService = webAppDebugService
        backingListForIteration = Arrays.asList(webAppDeployment, webAppService, webAppDebugService)
    }

    V1Deployment getPostgresDeployment() {
        return postgresDeployment
    }

    V1Service getPostgresService() {
        return postgresService
    }

    V1Deployment getWebAppDeployment() {
        return webAppDeployment
    }

    V1Service getWebAppService() {
        return webAppService
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

    static WebAppDeployment buildWebappDeployment(String regcred,
                                                  String devNamespace,
                                                  String apiId,
                                                  imageName,
                                                  imageVersion,
                                                  List<V1EnvVar> env = null, // override env for spring config
                                                  Integer webAppPort = 8080 // server port for spring config and service
    ) {

        def webappDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName("${apiId}-webappapi")
                .withNamespace(devNamespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: "${apiId}-webappapi".toString()])
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
                .withLabels([run: "${apiId}-webappapi".toString()])
                .endMetadata()
                .withNewSpec()
                .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(regcred).build())
                .addNewContainer()
                .withName("${apiId}-webappapi")
                .withImage("${imageName}:${imageVersion}")
                .withImagePullPolicy("Always")
                .withPorts(
                        new V1ContainerPortBuilder().withName("webappport").withContainerPort(webAppPort).build(),
                        new V1ContainerPortBuilder().withName("debugport").withContainerPort(5005).build()
                )
                .withEnv(env)
                .withNewResources()
                .withRequests("memory": new Quantity("1024Mi"), "cpu": new Quantity("0.1"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        def webappHttpService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("${apiId}-webappapi-http")
                .withLabels([run: "${apiId}-webappapi-http".toString()])
                .endMetadata()
                .withNewSpec()
                .withType("ClusterIP")
                .withPorts(
                        new V1ServicePortBuilder()
                                .withPort(80)
                                .withTargetPort(new IntOrString(webAppPort))
                                .withProtocol("TCP")
                                .withName("webappport")
                                .build()
                ).withSelector([run: "${apiId}-webappapi".toString()])
                .endSpec()
                .build()

        def webappDebugService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("${apiId}-webappapi-debug")
                .withLabels([run: "${apiId}-webappapi-debug".toString()])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder()
                                .withPort(5005)
                                .withProtocol("TCP")
                                .withName("debugport")
                                .build()
                ).withSelector([run: "${apiId}-webappapi".toString()])
                .endSpec()
                .build()


        //return new WebAppDeployment(postgresDataPVC, postgresDeployment, postgresService, webappDeployment, webappHttpService, webappDebugService)
        //we do not need webapp DB just yet.
        return new WebAppDeployment(webappDeployment, webappHttpService, webappDebugService)
    }
}