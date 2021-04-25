middleware

plugins {
    `java-library`
}

description = "Middleware for manage configuration of Sisyphus Project"

dependencies {
    implementation(project(":lib:sisyphus-common"))
    implementation("org.apache.maven:maven-resolver-provider")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic")
    implementation("org.apache.maven.resolver:maven-resolver-transport-wagon")
    implementation("org.apache.maven.wagon:wagon-file")
    implementation("org.apache.maven.wagon:wagon-http")
}
