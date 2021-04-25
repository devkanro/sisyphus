lib

plugins {
    `java-library`
}

description = "Jackson utils for Sisyphus"

dependencies {
    api(project(":lib:sisyphus-common"))
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-properties")
}
