plugins {
    id("java-library")
}

repositories {
    // PaperMC en premier : Maven Central limite les téléchargements (429)
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
    // EterLib : compilé depuis GitHub
    maven("https://jitpack.io")
    // Repli : EterLib publié sur cette machine (`gradlew publishToMavenLocal` dans EterLib), pour tester avant de pousser
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    // Socle commun : base, Redis, langues, joueurs du réseau (plugin EterLib installé sur le serveur)
    compileOnly("com.github.Eternom:EterLib:1.4.0")
    // Grades dans le chat (facultatif)
    compileOnly("net.luckperms:api:5.5")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        // Déclarée comme entrée : sinon le cache de Gradle réutilise un plugin.yml avec l'ancienne version
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
