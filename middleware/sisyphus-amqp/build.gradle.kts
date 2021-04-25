middleware

plugins {
    `java-library`
}

description = "Middleware for using AMQP in Sisyphus Project"

dependencies {
    implementation(project(":lib:sisyphus-dto"))
    implementation(project(":lib:sisyphus-jackson"))

    api("org.springframework.amqp:spring-rabbit")
}
