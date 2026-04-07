plugins {
    `java-library`
}

val gdxVersion: String by project

dependencies {
    // LibGDX core — api pozwala na widoczność tej zależności w desktop
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}
