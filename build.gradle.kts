import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    kotlin("jvm") version "1.9.0"
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "lol.koblizek"
version = "0.2.2"

tasks.getByName("build").finalizedBy("shadowJar")
val dependency: Configuration by configurations.creating

configurations.implementation {
    extendsFrom(dependency)
}

tasks.getByName("shadowJar", ShadowJar::class) {
    this.archiveClassifier = ""
    configurations = listOf(dependency)
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "OSGI-INF/**", "*.profile", "module-info.class", "ant_tasks/**")
    mergeServiceFiles()

    listOf(
        "com.github.salomonbrys.kotson",
        "com.google.errorprone.annotations",
        "com.google.gson",
        "dev.denwav.hypo",
        "io.sigpipe.jbsdiff",
        "me.jamiemansfield",
        "net.fabricmc",
        "org.apache.commons",
        "org.apache.felix",
        "org.apache.http",
        "org.cadixdev",
        "org.eclipse",
        "org.jgrapht",
        "org.jheaps",
        "org.objectweb.asm",
        "org.osgi",
        "org.tukaani.xz",
        "org.slf4j",
        "codechicken.diffpatch",
        "codechicken.repack",
        "joptsimple",
        "net.minecraftforge",
        "net.neoforged",
        "org.jetbrains.java"
    ).forEach { pack ->
        relocate(pack, "composer.$pack")
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "neoforgedReleases"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    dependency("com.github.MCPHackers:DiffPatch:cde1224")
    dependency("org.apache.commons:commons-lang3:3.13.0")
    dependency("commons-io:commons-io:2.14.0")
    dependency("com.google.code.gson:gson:2.10.1")
    dependency("net.neoforged:AutoRenamingTool:1.0.7")
    dependency("org.vineflower:vineflower:1.9.3")
    dependency("net.fabricmc:tiny-remapper:0.8.7")
    dependency("net.fabricmc:mapping-io:0.4.2")
    implementation(gradleApi())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website = "https://github.com/ComposeMC/Composer"
    vcsUrl = "https://github.com/ComposeMC/Composer"

    plugins {
        create("composer") {
            id = "lol.koblizek.composer"
            displayName = "Composer"
            description = "Plugin which makes creating Minecraft server development much easier!"
            implementationClass = "lol.koblizek.composer.ComposerPlugin"
            tags = listOf("minecraft", "decompilation", "deobfuscation", "plugins")
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "lol.koblizek"
            artifactId = "composer"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}