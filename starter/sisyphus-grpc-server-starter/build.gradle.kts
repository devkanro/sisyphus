starter

plugins {
    `java-library`
    protobuf
}

dependencies {
    api(project(":lib:sisyphus-protobuf"))
    api(project(":middleware:sisyphus-grpc-client"))
    implementation(Dependencies.Grpc.stub)
    runtimeOnly(Dependencies.Grpc.netty)
}

protobuf {
    packageMapping(
            "grpc.reflection.v1" to "com.bybutter.sisyphus.starter.grpc.support.reflection.v1",
            "grpc.reflection.v1alpha" to "com.bybutter.sisyphus.starter.grpc.support.reflection.v1alpha"
    )
}