import kotlin.Triple
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task abstraction for generation/push of docker images
 */
class ImageTask extends DefaultTask {
    protected List<Triple<File, String, String>> resourceImageAndVersion
    protected Map<String, Map<String, String>> imageToBuildArgsMap

    @TaskAction
    def buildImage() {
        resourceImageAndVersion.forEach({
            DockerClientProvider.buildDockerImage(
                    it.first as File,
                    it.second as String,
                    it.third as String,
                    true,
                    project,
                    imageToBuildArgsMap.getOrDefault(it.second, null)
            )
        })
    }
}