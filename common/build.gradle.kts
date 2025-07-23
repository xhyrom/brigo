plugins {
    idea
    java
    id("java-library")
    id("xyz.wagyourtail.unimined")
    id("com.github.johnrengelman.shadow")
}

val modId: String = property("mod_id") as String
val minecraftVersion: String = property("minecraft_version") as String
val mcpVersion: String = property("mcp_version") as String
val shadowBundle: Configuration by configurations.creating
val noRemap: Configuration by configurations.creating

unimined.minecraft {
    version(minecraftVersion)

    accessWidener {
        accessWidener(file("src/main/resources/${modId}.aw"))
    }

    mappings {
        searge()
        mcp("stable", mcpVersion)
    }

    defaultRemapJar = false
}

repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    api("org.tinylog:tinylog-api:2.7.0")
    api("org.tinylog:tinylog-impl:2.7.0")
    shadowBundle("org.tinylog:tinylog-api:2.7.0")
    shadowBundle("org.tinylog:tinylog-impl:2.7.0")

    implementation("com.mojang:brigadier:1.0.18")
    shadowBundle("com.mojang:brigadier:1.0.18")

    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "no-remap"
    archiveFileName = "${base.archivesName.get()}-${archiveClassifier.get()}.jar"
}

artifacts {
    add(noRemap.name, tasks.shadowJar)
}
