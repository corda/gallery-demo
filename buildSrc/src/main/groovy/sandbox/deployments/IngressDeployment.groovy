package sandbox.deployments

import io.kubernetes.client.openapi.models.NetworkingV1beta1Ingress
import io.kubernetes.client.openapi.models.NetworkingV1beta1IngressBuilder
import io.kubernetes.client.openapi.models.NetworkingV1beta1IngressRule
import io.kubernetes.client.openapi.models.V1Service

import java.util.function.Consumer

class IngressRule {
    String host
    Integer port
    V1Service targetService

    IngressRule(String host, Integer port, V1Service targetService) {
        this.host = host
        this.port = port
        this.targetService = targetService
    }
}

class IngressDeployment implements Iterable<Object> {

    private final List<NetworkingV1beta1Ingress> ingress

    IngressDeployment(NetworkingV1beta1Ingress ingress) {
        this.ingress = Arrays.asList(ingress)
    }

    static IngressDeployment buildIngressDeployment(String namespace,
                        String identifier,
                        List<NetworkingV1beta1IngressRule> ingressRules
    ) {
        new IngressDeployment(new NetworkingV1beta1IngressBuilder()
                .withKind("Ingress")
                .withApiVersion("networking.k8s.io/v1beta1")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(identifier + "-http-ingress")
                .withAnnotations(["kubernetes.io/ingress.class": "nginx"])
                .endMetadata()
                .withNewSpec()
                .withRules(ingressRules)
                .endSpec()
                .build())
    }

    @Override
    Iterator<Object> iterator() {
        return ingress.iterator()
    }

    @Override
    void forEach(Consumer<? super Object> action) {
        ingress.forEach(action)
    }

    @Override
    Spliterator<Object> spliterator() {
        return ingress.spliterator()
    }
}
