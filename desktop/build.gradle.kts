plugins {
    application
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
    mainClass = "io.github.dawciobiel.DesktopLauncherKt"
}

// Wskazanie folderu assets jako katalogu roboczego
tasks.run.configure {
    workingDir = rootProject.file("assets")
}
