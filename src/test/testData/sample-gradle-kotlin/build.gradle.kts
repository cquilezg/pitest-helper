plugins {
    id("java")
    id("info.solidsoft.pitest") version ("1.15.0")
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
    loadSetProperty("pitest.targetClasses")?.let { targetClasses = it }
    loadSetProperty("pitest.targetTests")?.let { targetTests = it }
    loadStringProperty("pitest.junit5PluginVersion")?.let { junit5PluginVersion = it }
    loadBooleanProperty("pitest.verbose")?.let { verbose = it }
}

fun loadSetProperty(propertyName: String): SetProperty<String>? {
    val propertyValue = properties[propertyName]
    return if (propertyValue != null && (propertyValue as String).isNotBlank()) {
        objects.setProperty(String::class).value(propertyValue.split(","))
    } else
        null
}

fun loadStringProperty(propertyName: String): String? {
    val propertyValue = properties[propertyName]
    return if (propertyValue != null && (propertyValue as String).isNotBlank()) {
        propertyValue
    } else
        null
}

fun loadBooleanProperty(propertyName: String): Boolean? {
    val propertyValue = properties[propertyName]
    if (propertyValue is String) {
        return propertyValue.equals("true", true)
    }
    return null
}