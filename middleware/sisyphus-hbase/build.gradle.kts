middleware

plugins {
    `java-library`
}

description = "Middleware for using HBase in Sisyphus Project"

dependencies {
    api(project(":lib:sisyphus-dto"))
    api(project(":lib:sisyphus-jackson"))
    compileOnly("org.apache.hbase:hbase-client")
    implementation(project(":lib:sisyphus-common"))
}
