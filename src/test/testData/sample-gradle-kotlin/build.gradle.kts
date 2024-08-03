import java.nio.charset.Charset

plugins {
    id("java")
    id("info.solidsoft.pitest") version ("1.15.0")
    id("com.cquilezg.properties-manager") version ("1.0")
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

//pitest {
//    propertyManager.bindProperty(project, targetClasses, "pitest.targetClasses")
//    propertyLoad.bindProperty(project, targetTests, "pitest.targetTests")
//    propertyLoad.bindStringProperty(project, junit5PluginVersion, "pitest.junit5PluginVersion")
//    propertyLoad.bindBooleanProperty(project, verbose, "pitest.verbose")
//    threads = 4
//}

tasks.register("helloWorld") {
    println(propertyManager.loadStringProperty(project, "myStringProperty"))  // Loads String property
    println(propertyManager.loadBooleanProperty(project, "myBooleanProperty"))  // Loads Boolean property
}

tasks.register("customProperty") {
    val charsetProperty = project.objects.property(Charset::class.java)
    propertyManager.bindCustomProperty(project, charsetProperty, "custom.charset", Charset::class.java) { charsetConverter(it) }
    println("My charset: $charsetProperty")
}

tasks.register("bindProperties") {
    val stringProperty = project.objects.property(String::class.java)
    propertyManager.bindStringProperty(project, stringProperty, "myStringProperty")
    println("My String property: ${stringProperty.orNull}")

    val intProperty = project.objects.property(Int::class.java)
    propertyManager.bindIntProperty(project, intProperty, "myIntProperty")
    println("My Int property: ${intProperty.orNull}")
}

tasks.register("bindMultiValueProperties") {
    val setCharProperty = project.objects.setProperty(Char::class.java)
    println("Set Char: $setCharProperty")
    propertyManager.bindMultiValueProperty(project, setCharProperty, "setChar", Char::class.java)
    println("Set Char: ${setCharProperty.orNull}")
}

pitest {
    propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
    propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
    propertyManager.bindMultiValueProperty(project, features, "pitest.features", String::class.java)
    propertyManager.bindMultiValueProperty(project, outputFormats, "pitest.outputFormats", String::class.java)
    propertyManager.bindIntProperty(project, threads, "pitest.threads")
    propertyManager.bindStringProperty(project, junit5PluginVersion, "pitest.junit5PluginVersion")
    propertyManager.bindBooleanProperty(project, verbose, "pitest.verbose")
    propertyManager.bindBigDecimalProperty(project, timeoutFactor, "pitest.timeoutFactor")

    val charsetProperty = project.objects.property(Charset::class.java)
    println("Input charset: $charsetProperty")
    propertyManager.bindCustomProperty(project, charsetProperty, "pitest.inputCharset", Charset::class.java) { charsetConverter(it) }
    println("Input charset: $charsetProperty")

    val setDecimalProperty = project.objects.setProperty(BigDecimal::class.java)
    println("Big decimal: $setDecimalProperty")
    propertyManager.bindMultiValueProperty(project, setDecimalProperty, "bigDecimal", BigDecimal::class.java)
    println("Big decimal: ${setDecimalProperty.orNull}")

    val charProperty = project.objects.property(Char::class.java)
    println("Char: $charProperty")
    propertyManager.bindCharProperty(project, charProperty, "char")
    println("Char: ${charProperty.orNull}")

    val setCharProperty = project.objects.setProperty(Character::class.java)
    println("Set Char: $setCharProperty")
    propertyManager.bindMultiValueProperty(project, setCharProperty, "setChar", Character::class.java)
    println("Set Char: ${setCharProperty.orNull}")
}

fun charsetConverter(charsetName: String): Charset {
    return Charset.forName(charsetName)
}