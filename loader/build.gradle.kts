dependencies {
    compileOnly(project(":core"))

    compileOnly("com.willfp:eco:6.67.0")
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
}

group = "com.willfp"
version = rootProject.version

tasks {
    build {
        dependsOn(publishToMavenLocal)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "libreforge-loader"
            groupId = "com.willfp"
        }
    }
}
