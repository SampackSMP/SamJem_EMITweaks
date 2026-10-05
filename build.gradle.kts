plugins {
    java
    id("net.neoforged.moddev") version "2.0.78"
}

val mod_id: String by project
val mod_version: String by project
val mod_group_id: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project
val neoforge_version: String by project
val neoforge_version_range: String by project
val loader_version_range: String by project
val emi_version: String by project
val remi_version: String by project
val mixinextras_version: String by project

version = mod_version
group = mod_group_id

base {
    archivesName.set(mod_id)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    // EMI's NeoForge builds aren't published to a normal Maven layout anywhere - Modrinth's
    // own maven-compatible endpoint is the only reliable source, keyed by Modrinth's opaque
    // per-file version id rather than EMI's own version string (see emi_version comment in
    // gradle.properties).
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

neoForge {
    version = neoforge_version

    parchment {
        minecraftVersion = "1.21.1"
        mappingsVersion = "2024.11.17"
    }

    runs {
        create("client") {
            client()
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // Compile against EMI's classes (EmiTags, EmiShapedRecipe, ...) - never bundled, EMI is a
    // client runtime dependency declared in neoforge.mods.toml instead. Coordinate
    // is Modrinth's own opaque per-file version id (see gradle.properties), not EMI's version
    // string.
    compileOnly("maven.modrinth:emi:$emi_version")

    // REMI's stack groups, which the creative inventory's groups follow. Required on the client at
    // runtime (neoforge.mods.toml), never bundled.
    compileOnly("maven.modrinth:reliable-emi:$remi_version")

    // MixinExtras for @WrapOperation/@ModifyExpressionValue, used to wrap calls without overwriting
    // whole methods. NeoForge bundles a MixinExtras-aware Mixin service at runtime already
    // (same reason REMI's own build.gradle only needs these two configurations, no runtime jar).
    compileOnly("io.github.llamalad7:mixinextras-common:$mixinextras_version")
    annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinextras_version")
}

tasks.jar {
    // The group sprites (from Inventory Item Groups) and the IconDump code are MIT-licensed and
    // must ship with their notices.
    from("LICENSE-InventoryItemGroups")
    from("LICENSE-IconDump")
}

tasks.withType<ProcessResources> {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neoforge_version" to neoforge_version,
        "neoforge_version_range" to neoforge_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to project.findProperty("mod_name"),
        "mod_license" to project.findProperty("mod_license"),
        "mod_version" to mod_version,
        "mod_authors" to project.findProperty("mod_authors")
    )
    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}
