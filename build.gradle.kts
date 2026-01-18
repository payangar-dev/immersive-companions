plugins {
    java
}

val mod_id: String by project
val mod_name: String by project
val mod_version: String by project
val mod_group_id: String by project

allprojects {
    apply(plugin = "java")

    group = mod_group_id
    version = mod_version

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.parchmentmc.org")
        maven("https://maven.isxander.dev/releases") // YACL
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
