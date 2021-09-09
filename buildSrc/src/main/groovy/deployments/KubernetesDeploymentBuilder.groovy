package deployments

import com.google.common.collect.Iterables
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.util.Yaml
import java.util.function.Consumer

enum DeploymentTarget {
    DEV, AZURE

    V1StorageClassBuilder getFileShareStorage(String name) {
        if (this == AZURE) {
            return new V1StorageClassBuilder()
                    .withKind("StorageClass")
                    .withApiVersion("storage.k8s.io/v1")
                    .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                    .withProvisioner("kubernetes.io/azure-file")
                    .withMountOptions(["dir_mode=0777",
                                       "file_mode=0777",
                                       "uid=0",
                                       "gid=0",
                                       "mfsymlinks",
                                       "cache=strict",
                                       "actimeo=30"])
                    .withParameters([skuName: "Standard_LRS"])
        } else {
            return new V1StorageClassBuilder()
                    .withKind("StorageClass")
                    .withApiVersion("storage.k8s.io/v1")
                    .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                    .withProvisioner("docker.io/hostpath")
                    .withMountOptions(["dir_mode=0777",
                                       "file_mode=0777",
                                       "uid=0",
                                       "gid=0",
                                       "mfsymlinks",
                                       "cache=strict",
                                       "actimeo=30"])
        }
    }
}

class DockerRegistrySecret implements Iterable<Object> {

    private final V1Secret dockerSecret
    private final List<V1Secret> list

    DockerRegistrySecret(namespace, name, registry, username, password) {


        String auth = "$username:$password"
        String encodedAuthString = Base64.getEncoder().encodeToString(auth.getBytes()).trim()
        String email = "docker@me.com"

        String toEncode = "{\"auths\":{\"$registry\":{\"username\":\"$username\",\"password\":\"$password\",\"email\":\"$email\",\"auth\":\"$encodedAuthString\"}}}".trim()

        dockerSecret = new V1SecretBuilder()
                .withApiVersion("v1")
                .withKind("Secret")
                .withNewMetadata()
                .withCreationTimestamp(null)
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withData(".dockerconfigjson": Base64.getEncoder().encodeToString(toEncode.getBytes()).trim())
                .withType("kubernetes.io/dockerconfigjson")
                .build()

        list = Collections.singletonList(dockerSecret)
    }

    @Override
    Iterator<Object> iterator() {
        return list.iterator()
    }

    @Override
    void forEach(Consumer<? super Object> action) {
        list.forEach(action)
    }

    @Override
    Spliterator<Object> spliterator() {
        return list.spliterator()
    }
}

class StorageClass implements Iterable<Object> {

    private final V1StorageClass storageClass
    private final Iterable<V1StorageClass> iterable = Collections.singletonList(storageClass)

    StorageClass(V1StorageClass storageClass) {
        this.storageClass = storageClass
    }

    V1StorageClass getStorageClass() {
        return storageClass
    }

    @Override
    Iterator<Object> iterator() {
        return iterable.iterator()
    }

    @Override
    void forEach(Consumer<? super Object> action) {
        iterable.forEach(action)
    }

    @Override
    Spliterator<Object> spliterator() {
        return iterable.spliterator()
    }
}

class Namespace implements Iterable<Object> {

    private final V1Namespace namespace
    private final Iterable<V1Namespace> iterable = Collections.singletonList(namespace)

    Namespace(V1Namespace namespace) {
        this.namespace = namespace
    }

    V1Namespace getNamespace() {
        return namespace
    }

    @Override
    Iterator<Object> iterator() {
        return iterable.iterator()
    }

    @Override
    void forEach(Consumer<? super Object> action) {
        iterable.forEach(action)
    }

    @Override
    Spliterator<Object> spliterator() {
        return iterable.spliterator()
    }
}

class KubernetesDeployment {

    private DeploymentTarget deploymentTarget

    KubernetesDeployment() {

        // default to local deployment
        deploymentTarget = DeploymentTarget.DEV

        def envDeployTarget = System.getenv("R3_SANDBOX_DEPLOYMENT_TARGET")
        if (envDeployTarget!=null && envDeployTarget=="AZURE") {
            deploymentTarget = DeploymentTarget.AZURE
        }

        System.out.println(" Deployment target is: " + deploymentTarget.toString())
    }

    def buildFileStorageClass = { String identifier ->
        return new StorageClass(
                deploymentTarget.getFileShareStorage("standard-file-share").build()
        )
    }

    def buildNamespace = { String devNamespace ->
        return new Namespace(new V1NamespaceBuilder()
                .withNewApiVersion("v1")
                .withNewKind("Namespace")
                .withNewMetadata()
                .withName(devNamespace)
                .withLabels("name": devNamespace)
                .endMetadata()
                .build())
    }

    def buildNodeStatefulSet = {
        String regcred,
        String devNamespace,
        String targetNetworkService,
        String identifier,
        String x500,
        String imageName,
        String imageVersion
            ->
            return NodeStatefulSet.buildNodeStatefulSet(regcred, devNamespace, targetNetworkService, identifier, x500, imageName, imageVersion)
    }

    def buildNodeDeployment = {
        String regcred,
        String devNamespace,
        String targetNetworkService,
        String identifier,
        String x500,
        String imageName,
        String imageVersion ->
            return NodeDeployment.buildNodeDeployment(
                    regcred,
                    devNamespace,
                    targetNetworkService,
                    identifier,
                    x500,
                    imageName,
                    imageVersion
            )
    }

    def buildNMSDeployment = { String devNamespace, String networkServiceName, String imageName ->
        return NetworkServicesDeployment.buildNMSDeployment(devNamespace, networkServiceName, imageName)
    }

    def buildWebAppDeployment = {
        String regcred,
        String namespace,
        String identifier,
        String imageName,
        String imageVersion,
        Map<String, String> envStr = null,
        Integer webAppPort = 8080 ->
            List<V1EnvVar> env = envStr ? mapToEnvList(envStr) : null
            return WebAppDeployment.buildWebappDeployment(regcred, namespace, identifier, imageName, imageVersion, env, webAppPort)
    }

    def buildFrontEndDeployment = {
        String regcred,
        String devNamespace,
        String identifier,
        String imageName,
        String imageVersion
            ->
            return FrontEndDeployment.buildFrontEndDeployment(regcred, identifier, imageName, imageVersion, devNamespace)
    }

    def buildIngressDeployment = { String namespace,
                                   String identifier,
                                   List<IngressRule> rules
        ->
        List<NetworkingV1beta1IngressRule> ingressRules = new ArrayList()
        rules.forEach({ rule ->
            NetworkingV1beta1IngressRule r = new NetworkingV1beta1IngressRuleBuilder()
                    .withHost(rule.host)
                    .withNewHttp()
                    .withPaths(
                            new NetworkingV1beta1HTTPIngressPathBuilder()
                                    .withNewBackend()
                                    .withNewServiceName(rule.targetService.metadata.name)
                                    .withNewServicePort(rule.port)
                                    .endBackend()
                                    .build()
                    )
                    .endHttp()
                    .build()
            ingressRules.add(r)
        })

        return IngressDeployment.buildIngressDeployment(namespace, identifier, ingressRules)
    }

    def buildReverseProxyDeployment = {
        String regcred,
        String namespace,
        String identifier,
        String nginxImageName,
        WebAppDeployment api,
        FrontEndDeployment frontend,
        Map<String, String> envStr = null
            ->
            List<V1EnvVar> env = envStr ? mapToEnvList(envStr) : null
            return NginxReverseProxyDeployment.buildReverseProxyDeployment(
                    regcred,
                    identifier,
                    nginxImageName,
                    api,
                    frontend,
                    namespace,
                    env
            )
    }

    def generateYaml = { Iterable<Object> toYamlify ->
        return Yaml.dumpAll(Iterables.concat(toYamlify.asList().toArray(new Iterable[0]) as Iterable<? extends Iterable<?>>).iterator())
    }

    // Helper function to create environment list
    static List<V1EnvVar> mapToEnvList(Map<String, String> envVars) {
        List<V1EnvVar> env = new ArrayList<>()
        envVars.entrySet().forEach {
            env.add(new V1EnvVarBuilder().withName(it.key).withValue(it.value).build())
        }
        // add jdwp debug port env
        env.add(new V1EnvVarBuilder().withName("JVM_ARGS").withValue("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -XX:MaxHeapFreeRatio=40").build())
        return env
    }
}