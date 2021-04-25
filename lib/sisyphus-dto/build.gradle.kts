lib

plugins {
    `java-library`
}

description = "Easy to create struct in Sisyphus"

dependencies {
    compileOnly(project(":lib:sisyphus-jackson"))
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")

    implementation(project(":lib:sisyphus-common"))
    implementation(project(":extension:sisyphusx-reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter")
}
