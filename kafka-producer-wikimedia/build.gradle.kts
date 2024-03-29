plugins {
	java
	id("org.springframework.boot") version "3.2.4"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "com.pavelkostal"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.apache.kafka:kafka-clients:3.7.0")
//	implementation("org.slf4j:slf4j-api:1.7.36")
//	implementation("org.slf4j:slf4j-simple:1.7.36")
	compileOnly("org.projectlombok:lombok:1.18.32")
	annotationProcessor("org.projectlombok:lombok:1.18.20")
	implementation("com.squareup.okhttp3:okhttp:4.9.3")
	implementation("com.launchdarkly:okhttp-eventsource:2.5.0")



	implementation("org.springframework.boot:spring-boot-starter")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
