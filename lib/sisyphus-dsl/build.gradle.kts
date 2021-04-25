lib

plugins {
    antlr
    `java-library`
    protobuf
}

description = "Utils and toolkit for building gRPC service easier"

dependencies {
    api(project(":lib:sisyphus-grpc"))

    implementation(project(":extension:sisyphusx-reflect"))
    implementation(project(":lib:sisyphus-common"))

    antlr("org.antlr:antlr4")

    testImplementation("org.junit.jupiter:junit-jupiter")
}
