import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

val Project.managedDependencies: Project
    get() {
        dependencies {
            project.configurations.forEach {
                it.name.invoke(platform(project(":sisyphus-dependencies")))
            }
        }
        return this
    }