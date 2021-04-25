lib

plugins {
    `java-library`
    protobuf
}

description = "Sisyphus customized gRPC runtime for Kotlin coroutine(full support)"

dependencies {
    api(project(":lib:sisyphus-grpc"))
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-kotlin-stub") {
        exclude("io.grpc", "grpc-protobuf")
    }
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation(project(":lib:sisyphus-jackson"))

    proto(project(":lib:sisyphus-grpc"))
}

protobuf {
    plugins {
        separatedCoroutine()
    }
}
