plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "dev.lorenzz"
version = "1.0.3"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.j4c0b3y.net/public/")
    maven("https://jitpack.io")
    maven {
        name = "reposiliteRepositoryReleases"
        url = uri("https://repo.lorenzzzz.xyz/releases")
    }
    maven {
        name = "reposiliteRepositoryPrivate"
        url = uri("https://repo.lorenzzzz.xyz/private")
        credentials {
            username = project.findProperty("lorenzRepoUser") as String? ?: System.getenv("REPO_USER")
            password = project.findProperty("lorenzRepoPass") as String? ?: System.getenv("REPO_PASS")
        }
    }
    maven("https://libraries.minecraft.net")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven {
        name = "lunarclient"
        url = uri("https://repo.lunarclient.dev")
    }
}

dependencies {
    // Server API
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Custom CommandAPI
    implementation("dev.lorenzz:commandapi-common:2.0.2")
    implementation("dev.lorenzz:commandapi-bukkit:2.0.2")

    // Hooks (compileOnly)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.7")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.lunarclient:apollo-api:1.2.1")
    implementation("dev.lorenzz.libs:discord-api-spigot:1.0.0-SNAPSHOT")
    implementation("dev.lorenzz.libs:discord-api-common:1.0.0-SNAPSHOT")



    // Shaded libraries
    implementation("net.j4c0b3y:MenuAPI-core:1.5.5")
    implementation("net.j4c0b3y:MenuAPI-extras:1.5.5")
    implementation("net.kyori:adventure-api:4.15.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.3")
    implementation("com.github.cryptomorin:XSeries:13.1.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.sqlite", "dev.lorenzz.hiru.libs.sqlite")
        relocate("net.j4c0b3y", "dev.lorenzz.hiru.libs.menuapi")
        relocate("com.cryptomorin.xseries", "dev.lorenzz.hiru.libs.xseries")
        relocate("dev.lorenzz.libs.discord", "dev.lorenzz.hiru.libs.discord")
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
