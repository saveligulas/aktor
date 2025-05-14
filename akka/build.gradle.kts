plugins {
    id("java")
    application
}

dependencies {
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.8.4")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.typesafe.akka:akka-serialization-jackson_2.13:2.8.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.0.1")
    implementation("com.typesafe.akka:akka-stream_2.13:2.8.4")
    implementation("com.lightbend.akka:akka-stream-alpakka-mqtt_2.13:6.0.2")
}



tasks.test {
    useJUnitPlatform()
}