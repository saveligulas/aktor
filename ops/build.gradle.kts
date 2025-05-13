plugins {
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

group = "jl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:4.29.3")
    implementation("com.google.protobuf:protobuf-java-util:4.29.3")
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.8.0")
    implementation("com.typesafe.akka:akka-serialization-jackson_2.13:2.8.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.3"
    }
}

tasks.test {
    useJUnitPlatform()
}
