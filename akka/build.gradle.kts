plugins {
    id("java")
    application
}

dependencies {
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.8.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.typesafe.akka:akka-serialization-jackson_2.13:2.8.0")
}


tasks.test {
    useJUnitPlatform()
}