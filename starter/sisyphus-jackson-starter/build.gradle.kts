starter

plugins {
    `java-library`
}

description = "Starter for configuring HttpMessageEncoder with Jackson in Sisyphus Framework"

dependencies {
    api(project(":lib:sisyphus-jackson"))
    api("org.springframework.boot:spring-boot-starter-json")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    compileOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
}
