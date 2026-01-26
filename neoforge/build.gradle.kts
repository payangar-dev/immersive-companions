plugins {
    id("net.neoforged.moddev") version "2.0.95"
}

base {
    archivesName.set("${project.property("mod_id")}-neoforge")
}

configurations {
    create("vscode")
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
val yacl_version: String by project

repositories {
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "YACL"
        url = uri("https://maven.isxander.dev/releases")
    }
}

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
    compileOnly("maven.modrinth:epic-fight:21.14.4-mc1.21.1-neoforge")

    // Config GUI
    implementation("dev.isxander:yet-another-config-lib:${yacl_version}-neoforge")

    // Waystones compatibility (compile-only soft dependency)
    compileOnly("maven.modrinth:waystones:21.1.4+neoforge-1.21.1")
    compileOnly("maven.modrinth:balm:21.0.21+neoforge-1.21.1")

    // Dynamic Lights compatibility (compile-only soft dependency)
    compileOnly("maven.modrinth:sodium-dynamic-lights:neoforge-1.21.1-1.0.10")

    "vscode"("maven.modrinth:epic-fight:21.14.4-mc1.21.1-neoforge")
    "vscode"("maven.modrinth:waystones:21.1.4+neoforge-1.21.1")
    "vscode"("maven.modrinth:balm:21.0.21+neoforge-1.21.1")
}

java {
    sourceSets["main"].compileClasspath += configurations["vscode"]
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
