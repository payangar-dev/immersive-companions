plugins {
    id("net.neoforged.moddev") version "2.0.95"
}

val mod_id: String by project
val mod_name: String by project
val mod_version: String by project
val mod_author: String by project
val mod_description: String by project
val minecraft_version: String by project
val neoforge_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

neoForge {
    version = neoforge_version

    parchment {
        minecraftVersion.set(parchment_minecraft_version)
        mappingsVersion.set(parchment_version)
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
        create("data") {
            data()
            programArguments.addAll(
                "--mod", mod_id,
                "--all",
                "--output", file("src/generated/resources/").absolutePath
            )
        }
    }

    mods {
        register(mod_id) {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":common"))
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
        "neoforge_version" to neoforge_version
    )

    inputs.properties(props)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
