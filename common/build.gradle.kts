plugins {
    id("java-library")
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("redis.clients:jedis:5.1.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.incendo:cloud-core:2.0.0-beta.2")
}
