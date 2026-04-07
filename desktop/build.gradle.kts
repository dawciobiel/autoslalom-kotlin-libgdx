plugins {
    application
    id("com.gradleup.shadow")
}

val gdxVersion: String by project

dependencies {
    // Zależy od core
    implementation(project(":core"))

    // Backend desktopowy LWJGL3
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")

    // Natywne biblioteki — Windows + Linux + Mac
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("io.github.dawciobiel.DesktopLauncherKt")
}

tasks.shadowJar {
    archiveFileName.set("autoslalom.jar")
    from(rootProject.file("assets"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Wskazanie folderu assets jako katalogu roboczego
tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")

    // Ustawienie środowiska dla ALSA / minimal log dla OpenAL
    environment("ALSA_CARD", "default")
    environment("ALSOFT_DRIVERS", "alsa,pulse")
    environment("ALSOFT_LOGLEVEL", "1")
}