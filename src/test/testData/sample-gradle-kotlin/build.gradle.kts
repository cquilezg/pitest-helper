import java.nio.charset.Charset

plugins {
    id("java")
    id("info.solidsoft.pitest") version ("1.15.0")
    id("io.github.cquilezg.properties-manager") version ("1.0")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
    propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String::class.java)