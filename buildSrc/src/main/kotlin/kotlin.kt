import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val Project.kotlin: Project
    get() {
        apply {
            plugin("kotlin")
            plugin("kotlin-spring")
            plugin("org.jlleitschuh.gradle.ktlint")
        }

        dependencies {
            "compileOnly"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }

        return this
    }