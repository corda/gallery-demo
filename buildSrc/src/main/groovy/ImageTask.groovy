import kotlin.Pair
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task abstraction for generation/push of docker images
 */
class ImageTask extends DefaultTask {
//    protected List<String> sourcePaths
//    protected File resourceDir
//    protected String imageName
    protected List<Pair<File, String>> resourceAndImage

    @TaskAction
    def buildImage() {
        resourceAndImage.forEach({
            DockerClientProvider.buildDockerImage(
                    it.first as File,
                    it.second as String,
                    project.version as String,
                    true,
                    project
            )
        })
    }
}