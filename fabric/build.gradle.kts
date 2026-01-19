plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

base {
    archivesName.set("${project.property("mod_id")}-fabric")
}

val mod_id: String by project
val mod_name: String by project
val mod_version: String by project
val mod_author: String by project
val mod_description: String by project
val minecraft_version: String by project
val fabric_loader_version: String by project
val fabric_api_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project
val yacl_version: String by project
val modmenu_version: String by project

repositories {
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/releases")
    }
    maven {
        name = "YACL"
        url = uri("https://maven.isxander.dev/releases")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$parchment_minecraft_version:$parchment_version@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")

    // Config GUI
    modImplementation("dev.isxander:yet-another-config-lib:${yacl_version}-fabric")
    modImplementation("com.terraformersmc:modmenu:${modmenu_version}")

    implementation(project(":common"))
}

loom {
    runs {
        named("client") {
            client()
            ideConfigGenerated(true)
        }
        named("server") {
            server()
            ideConfigGenerated(true)
        }
    }
}

tasks.jar {
    from(project(":common").sourceSets.main.get().output)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_version" to mod_version,
        "mod_author" to mod_author,
        "mod_description" to mod_description,
        "minecraft_version" to minecraft_version,
        "fabric_loader_version" to fabric_loader_version
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
