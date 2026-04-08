plugins {
    id("fabric-loom") version "1.15-SNAPSHOT"
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    mappings(loom.officialMojangMappings())
    modCompileOnly("net.fabricmc:fabric-loader:0.18.5")

    modCompileOnly("net.fabricmc.fabric-api:fabric-api:0.42.0+1.16")
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

        archiveFileName.set("[fabric-1.16.5]AllMusic_Client-${project.version}.jar")
        destinationDirectory.set(file("${parent!!.projectDir}/target"))
    }

    build {
        dependsOn(remapJar)
    }
}
