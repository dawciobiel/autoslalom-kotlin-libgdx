plugins {
    `java-library`
}

val gdxVersion: String by project

dependencies {
    // LibGDX core — api pozwala na widoczność tej zależności w desktop
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
}
