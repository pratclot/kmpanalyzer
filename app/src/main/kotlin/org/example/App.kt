import org.example.CustomModel
import org.gradle.tooling.GradleConnector
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: AnalyzerKt <path-to-project>")
        return
    }

    val projectDir = File(args[0])
    if (!projectDir.isDirectory) {
        println("Invalid project directory: $projectDir")
        return
    }

    analyzeProject(projectDir)
}

fun analyzeProject(projectDir: File) {
    val connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)

    connector.connect().use { connection ->
        connection.action()
            .projectsLoaded(
                {
                    it.getModel(CustomModel::class.java)
                },
                {
                    println(it.projectName)
                    it.projectsWithSourceSets
                        .forEach {
                            it.entries.forEach { (k, v) -> println("$k: $v") }
                        }
                },
            )
            .build()
            .withArguments(
                "tasks",
                "--init-script",
                "${System.getProperty("user.dir")}/initScriptForActualProject.gradle.kts",
            )
            .setStandardOutput(System.out)
            .setStandardError(System.err)
            .run()
    }
}