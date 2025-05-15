plugins {
    id("java")
    id("com.google.protobuf") version "0.9.4"
    application
}

group = "fhv.ops"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.72.0")
    implementation("io.grpc:grpc-protobuf:1.72.0")
    implementation("io.grpc:grpc-stub:1.72.0")
    implementation("com.google.protobuf:protobuf-java:4.29.3")
    implementation("com.google.protobuf:protobuf-java-util:4.29.3")
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.8.0")
    implementation("com.typesafe.akka:akka-serialization-jackson_2.13:2.8.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.3"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.72.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

application {
    mainClass.set("fhv.ops.server.OrderProcessorServer")
}

tasks.test {
    useJUnitPlatform()
}
