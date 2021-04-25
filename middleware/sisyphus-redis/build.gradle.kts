middleware

plugins {
    `java-library`
}

description = "Middleware for using Redis cache in Sisyphus Project"

dependencies {
    api(project(":lib:sisyphus-common"))
    api("org.springframework.boot:spring-boot-starter-data-redis")
}
