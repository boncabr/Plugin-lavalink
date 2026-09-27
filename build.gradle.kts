plugins {
    java
    alias(libs.plugins.lavalink)
}

group = "com.boncabr"
version = "0.1.0"

lavalinkPlugin {
    name = "lavasonic"
    apiVersion = libs.versions.lavalink.api
    serverVersion = libs.versions.lavalink.server
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 17
    }
}

dependencies {
    // Lavalink and Lavaplayer APIs are supplied by the Lavalink Gradle plugin.
}