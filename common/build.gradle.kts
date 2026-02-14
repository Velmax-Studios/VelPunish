plugins {
    id("java-library")
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.h2database:h2:2.2.224") // H2 local database support
    implementation("redis.clients:jedis:5.1.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.incendo:cloud-core:2.0.0-beta.2")
}
