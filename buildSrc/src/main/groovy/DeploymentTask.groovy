import deployments.KubernetesDeployment
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DeploymentTask extends DefaultTask {

    protected KubernetesDeployment kdeployment = new KubernetesDeployment()
    private File kubernetesBuildDir = new File(project.buildDir, "kubernetes")
    protected Iterable<Object> deployments
    protected String fileName

    DeploymentTask() {
        group = "deployment k8s"
    }

    @TaskAction
    def writeYaml() {
        kubernetesBuildDir.mkdirs()
        def yamlFile = new File(kubernetesBuildDir, "${fileName}.yaml")
        yamlFile.delete() // remove old
        def yaml = kdeployment.generateYaml(deployments)
        yamlFile.write(yaml)
    }
}