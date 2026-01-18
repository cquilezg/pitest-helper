plugins {
    id("java")
    id("info.solidsoft.pitest") version("1.15.0")
    id("io.github.cquilezg.properties-manager") version("1.0")
}

group = "com.cquilez"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib")) // Add lib as an app dependency
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
    propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String::class.java)
    junit5PluginVersion = "1.2.0"
    useClasspathFile = true
    verbose = true
}