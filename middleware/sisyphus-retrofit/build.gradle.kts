middleware

plugins {
    `java-library`
}

description = "Middleware for using Retrofit in Sisyphus Project"

dependencies {
    api("com.squareup.retrofit2:retrofit")
    api("com.squareup.okhttp3:okhttp")
    api("org.reflections:reflections")
    api("io.github.resilience4j:resilience4j-retrofit")
    api(project(":lib:sisyphus-common"))
    api(project(":lib:sisyphus-dto"))
    api(project(":lib:sisyphus-jackson"))
}
