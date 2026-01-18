plugins {
    id("net.neoforged.moddev") version "2.0.95"
}

val mod_id: String by project
val neoform_version: String by project
val parchment_minecraft_version: String by project
val parchment_version: String by project

neoForge {
    neoFormVersion = neoform_version

    parchment {
        minecraftVersion.set(parchment_minecraft_version)
        mappingsVersion.set(parchment_version)
    }
}

dependencies {
    compileOnly("com.google.guava:guava:32.1.2-jre")
}
