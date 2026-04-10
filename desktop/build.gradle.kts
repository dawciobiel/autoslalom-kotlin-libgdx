plugins {
    application
    id("com.gradleup.shadow")
}

val gdxVersion: String by project

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

val launcherClassName = "io.github.dawciobiel.DesktopLauncherKt"

application {
    mainClass.set(launcherClassName)
}

// Kluczowe obejście dla Gradle 9.4.0 + Shadow Plugin
// Wtyczka Shadow w fazie konfiguracji szuka właściwości 'mainClassName' w rozszerzeniu 'application'.
// W Gradle 9 ta właściwość nie istnieje, więc 'wstrzykujemy' ją ręcznie jako właściwość dodatkową (extra).
(application as ExtensionAware).extra.set("mainClassName", launcherClassName)

tasks.shadowJar {
    archiveFileName.set("autoslalom.jar")
    from(rootProject.file("assets"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes["Main-Class"] = launcherClassName
    }
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
}
