middleware

plugins {
    `java-library`
}

description = "Middleware for using MongoDB in Sisyphus Project"

dependencies {
    api("org.mongodb:mongodb-driver-reactivestreams")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
}
