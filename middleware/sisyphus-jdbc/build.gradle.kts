middleware

plugins {
    `java-library`
}

description = "Middleware for using JDBC in Sisyphus Project"

dependencies {
    api("org.jooq:jooq")
    implementation("com.zaxxer:HikariCP")

    compileOnly(project(":lib:sisyphus-dsl"))

    testImplementation("com.h2database:h2")
    testImplementation(project(":lib:sisyphus-dsl"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
