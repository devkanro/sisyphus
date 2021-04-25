middleware

plugins {
    `java-library`
}

description = "Middleware for using RocketMQ in Sisyphus Project"

dependencies {
    implementation(project(":lib:sisyphus-common"))

    api("org.apache.rocketmq:rocketmq-client")
    implementation("org.apache.rocketmq:rocketmq-acl")
}
