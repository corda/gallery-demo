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
                                                  List<String> webAppArgs,
                                                  List<V1EnvVar> env = null, // override env for spring config
                                                  Integer webAppPort = 8080 // server port for spring config and service
    ) {

        V1PersistentVolumeClaim postgresDataPVC = new V1PersistentVolumeClaimBuilder()
                .withKind("PersistentVolumeClaim")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("${apiId}-postgres-data-pvc")
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests([storage: new Quantity("1Gi")])
                .endResources()
                .endSpec()
                .build()

        def postgresDeployment = new V1DeploymentBuilder()
                .withKind("Deployment")
                .withApiVersion("apps/v1")
                .withNewMetadata()
                .withName("${apiId}-webappdb")
                .withNamespace(devNamespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels([run: "${apiId}-webappdb".toString()])
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels([run: "${apiId}-webappdb".toString()])
                .endMetadata()
                .withNewSpec()
                .withVolumes(
                        new V1VolumeBuilder().withName("${apiId}-postgres-data-storage")
                                .withPersistentVolumeClaim(
                                        new V1PersistentVolumeClaimVolumeSource().claimName(postgresDataPVC.metadata.name)
                                ).build()
                )
                .addNewInitContainer()
                .withName("${apiId}-postgres-chown-data")
                .withImage("busybox:latest")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("chown")
                .withArgs("-Rv", "999:999", "/var/lib/postgresql/data")
                .withVolumeMounts(
                        new V1VolumeMountBuilder().withName("${apiId}-postgres-data-storage").withMountPath("/var/lib/postgresql/data").build()
                )
                .endInitContainer()
                .addNewContainer()
                .withName("${apiId}-webappdb")
                .withImage("postgres:9.6")
                .withImagePullPolicy("IfNotPresent")
                .withEnv(new V1EnvVarBuilder().withName("POSTGRES_PASSWORD").withValue("postgres").build())
                .withPorts(
                        new V1ContainerPortBuilder().withName("postgresport").withContainerPort(5432).build()
                )
                .withVolumeMounts(
                        new V1VolumeMountBuilder()
                                .withName("${apiId}-postgres-data-storage")
                                .withSubPath("postgres")
                                .withMountPath("/var/lib/postgresql/data").build()
                )
                .withNewResources()
                .withRequests("memory": new Quantity("512Mi"), "cpu": new Quantity("0.5"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()

        def postgresService = new V1ServiceBuilder()
                .withKind("Service")
                .withApiVersion("v1")
                .withNewMetadata()
                .withNamespace(devNamespace)
                .withName("${apiId}-webappdb")
                .withLabels([run: "${apiId}-webappdb".toString()])
                .endMetadata()
                .withNewSpec()
                .withPorts(
                        new V1ServicePortBuilder()
                                .withPort(5432).withProtocol("TCP").withName("postgresport").build()
                ).withSelector([run: "${apiId}-webappdb".toString()])
                .endSpec()
                .build()


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
                .withImage("${imageName}")
                .withImagePullPolicy("Always")
                .withPorts(
                        new V1ContainerPortBuilder().withName("webappport").withContainerPort(webAppPort).build(),
                        new V1ContainerPortBuilder().withName("debugport").withContainerPort(5000).build()
                )
                .withEnv(env)
                .withArgs(webAppArgs)
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
                                .withPort(5000)
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