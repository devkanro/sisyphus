tools

plugins {
    `java-library`
}

description = "Proto compiler for Sisyphus customized Protobuf runtime"

dependencies {
    api(project(":lib:sisyphus-common"))
    api("com.squareup:kotlinpoet")
    api("com.google.protobuf:protobuf-java")

    implementation("com.github.os72:protoc-jar")
    implementation("com.google.api.grpc:proto-google-common-protos")
    implementation("com.google.api:api-common")

    implementation("io.reactivex.rxjava2:rxjava")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":lib:sisyphus-grpc-coroutine"))
    testImplementation(project(":lib:sisyphus-grpc-rxjava"))
}
