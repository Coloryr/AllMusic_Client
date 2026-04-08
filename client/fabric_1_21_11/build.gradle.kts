plugins {
    id("fabric-loom") version "1.15-SNAPSHOT"
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.18.6")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.3+1.21.11")
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version
            )
        }
    }

    shadowJar {
        relocate("com.google.gson", "com.coloryr.allmusic.libs.com.google.gson")
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)

        archiveFileName.set("[fabric-1.21.11]AllMusic_Client-${project.version}.jar")
        destinationDirectory.set(file("${parent!!.projectDir}/target"))
    }

    build {
        dependsOn(remapJar)
    }
}
