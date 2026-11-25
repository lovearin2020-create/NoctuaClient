import net.fabricmc.loom.task.RemapJarTask

plugins {
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.lumen"
version = "2.7.4"
val modid = "lumen"

java { toolchain.languageVersion.set(JavaLanguageVersion.of(8)) }

loom {
    launchConfigs {
        "client" {
            property("mixin.debug.verbose", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--mixin", "mixins.lumen.json")
        }
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
    }
    mixin { defaultRefmapName.set("mixins.$modid.refmap.json") }
}

sourceSets.main { output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory }) }

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
}

tasks.withType(JavaCompile::class) { options.encoding = "UTF-8" }

tasks.withType(Jar::class) {
    archiveBaseName.set("NoctuaClient")
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["ModSide"] = "CLIENT"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
    }
}

val remapJar by tasks.named<RemapJarTask>("remapJar") {
    archiveClassifier.set("1.8.9")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl)
}

tasks.assemble.get().dependsOn(tasks.remapJar)

// Dev-only launcher (Loom 0.10's runClient task is rejected by Gradle 8 task validation).
tasks.register<JavaExec>("devRun") {
    dependsOn("classes", "downloadAssets")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.fabricmc.devlaunchinjector.Main")
    jvmArgs(
        "-Dfabric.dli.config=${file(".gradle/loom-cache/launch.cfg")}",
        "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.minecraft.launchwrapper.Launch"
    )
    workingDir = file("run")
    args("--width", "1920", "--height", "1080")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
    doFirst { file("run").mkdirs() }
}
