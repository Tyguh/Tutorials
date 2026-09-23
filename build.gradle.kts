plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "net.viraxis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    mavenLocal()
    maven {
        name = "viraxis-repo"
        url = uri("https://repo.viraxis.net/repository/viraxis/")
        credentials {
            username = System.getenv("VIRAXIS_USER")
            password = System.getenv("VIRAXIS_PASS")
        }
    }
    maven { url = uri("https://repo.fancyinnovations.com/releases") }
    maven { url = uri("https://maven.fancyspaces.net/fancyinnovations/releases") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.massivecraft.massivecore:MassiveCore:MC-1.21.4-VIRAXIS")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("de.oliver:FancyNpcs:2.9.2")
    compileOnly(files("../MassiveCore/build/classes/java/main"))
    compileOnly(files("../Islands/build/classes/java/main"))
    compileOnly(files("../currencies/build/classes/java/main"))
    compileOnly(files("../holograms/build/classes/java/main"))
    compileOnly(files("../stacker/libs/FastAsyncWorldEdit.jar"))
    compileOnly(files("../stacker/libs/worldguard.jar"))
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release.set(21) }
tasks.shadowJar { archiveFileName.set("Tutorials.jar") }

tasks.register<JavaExec>("verifyTutorialConfig") {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().output + sourceSets.main.get().output + configurations.compileClasspath.get()
    mainClass.set("net.viraxis.tutorials.TutorialConfigCheck")
    args(file("src/main/resources/gamemodes/skyblock.yml").absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
