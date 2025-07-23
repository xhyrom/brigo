import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    idea
    id("xyz.wagyourtail.unimined")
    id("com.github.johnrengelman.shadow")
}

val modId: String = property("mod_id") as String
val minecraftVersion: String = property("minecraft_version") as String
val mcpVersion: String = property("mcp_version") as String
val loaderVersion: String = property("fabric_loader_version") as String

val shadowBundle: Configuration by configurations.creating

unimined.minecraft {
    version(minecraftVersion)

    legacyFabric {
        loader(loaderVersion)
        accessWidener(project(":common").file("src/main/resources/${modId}.aw"))
    }

    mappings {
        searge()
        mcp("stable", mcpVersion)
    }

    defaultRemapJar = true
}

repositories {
   unimined.legacyFabricMaven()
}

dependencies {
    implementation(project(":common", configuration = "noRemap"))
    shadowBundle(project(":common", configuration = "noRemap"))
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(tasks.shadowJar)
    asJar {
        inputFile.set(tasks.shadowJar.get().archiveFile)
        archiveFileName = "${base.archivesName.get()}.jar"
    }
}

tasks.build {
    dependsOn(tasks.named("remapJar"))
}