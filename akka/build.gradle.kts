plugins {
    id("java")
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.akka.io/maven")
    }
}

val akkaVersion = "2.10.5"
val akkaHttpVersion = "10.7.1"

dependencies {
    implementation("com.typesafe.akka:akka-actor-typed_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-serialization-jackson_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-slf4j_2.13:$akkaVersion") // explicitly add to satisfy version check
    implementation("com.typesafe.akka:akka-http_2.13:$akkaHttpVersion")
    implementation("com.lightbend.akka:akka-stream-alpakka-mqtt_2.13:6.0.2")
    implementation("org.scala-lang.modules:scala-java8-compat_2.13:1.0.2")
    implementation("io.grpc:grpc-netty-shaded:1.72.0")
    implementation("io.grpc:grpc-protobuf:1.72.0")
    implementation("io.grpc:grpc-stub:1.72.0")
    implementation(project(":ops"))
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("fhv.aktor.akka.Main")
}

tasks.test {
    useJUnitPlatform()
}
