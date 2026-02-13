plugins {
    id("java")
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.incendo:cloud-paper:2.0.0-beta.2")
    compileOnly("me.clip:placeholderapi:2.11.5")
}
