# pitest-helper

![Build](https://github.com/carmeloquilez/pitest-helper/actions/workflows/build.yml/badge.svg)
![Version](https://img.shields.io/jetbrains/plugin/v/23649-pitest-helper.svg)
![Downloads](https://img.shields.io/jetbrains/plugin/d/23649-pitest-helper.svg)

## PITest Helper

<!-- Plugin description -->
Plugin to run [PITest](https://pitest.org/) in your Java/Kotlin project using Maven or Gradle.
The plugin builds and runs PITest mutation coverage commands for you.

Requisites:  
- A Maven/Gradle project.
- For Maven: [pitest-maven](https://pitest.org/quickstart/maven/) configured on your project.
- For Gradle: [gradle-pitest-plugin](https://github.com/szpak/gradle-pitest-plugin) configured on your project.

> **_NOTE:_** if you use Gradle you need to do extra steps to make PITest Helper work: [Setup Gradle project](https://github.com/cquilezg/pitest-helper?tab=readme-ov-file#gradle-project).

Usage:  
- Right-click on your classes and packages you want to run mutation coverage and click _Run Mutation Coverage..._


Are you experiencing problems with the plugin? Do you have any suggestion? You can create an issue at the [Plugin Site](https://github.com/cquilezg/pitest-helper/issues)

<!-- Plugin description end -->

# Index
- [How PITest Helper works?](#how-pitest-helper-works)
- [Set up your project](#set-up-your-project)
- [Compatibility](#compatibility)

## How PITest Helper works?

PITest Helper relies on [pitest-maven](https://pitest.org/quickstart/maven/) (for Maven) and [gradle-pitest-plugin](https://github.com/szpak/gradle-pitest-plugin) (for Gradle) plugins to work. You need to configure the corresponding plugin in your project beforehand.

## Set up your project

### Maven project

For projects using Maven, you only need to set up [pitest-maven](https://pitest.org/quickstart/maven/) plugin. You can follow the official PIT instructions for 
Maven at the following link:

[PIT Configuration for Maven](https://pitest.org/quickstart/maven/)

If your project already has the PITest plugin configured, ignore this step.

### Gradle project

For projects using Gradle, you need to follow the next steps.

#### 1. Add the gradle-pitest-plugin to your project

Add the [gradle-pitest-plugin](https://github.com/szpak/gradle-pitest-plugin) plugin, edit your `build.gradle` (or `build.gradle.kts` for Kotlin DSL) file by adding the following code:

#### `build.gradle`

```groovy
plugins {
    id 'info.solidsoft.pitest' version '1.15.0'  // Choose your version
    // Other plugins...
}
```

#### `build.gradle.kts`

```kotlin
plugins {
    id("info.solidsoft.pitest") version ("1.15.0")  // Choose your version
    // Other plugins...
}
```
For more details on configuring the gradle-pitest-plugin for PITest in Gradle, visit the [official plugin documentation](https://github.com/szpak/gradle-pitest-plugin).

#### 2. Configure your pitest task to load the command-line project properties

Unlike the Maven plugin, gradle-pitest-plugin does not support command-line arguments to configure the PITest execution.  
To support this you need to load properties manually and pass its values to the property providers the plugin uses.
I have made a simple plugin that make the things easy for you: [properties-manager](https://github.com/cquilezg/properties-manager).
Below you have the instructions to use it:

#### `build.gradle`
Add the plugin to your project and binds the properties to the property providers:

```groovy
plugins {
    id 'io.github.cquilezg.properties-manager' version '1.0'
    // Other plugins
}

pitest {
  propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String)
  propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String)
  // More PITest config...
}
```

#### `build.gradle.kts`

```kotlin
plugins {
    id("io.github.cquilezg.properties-manager") version ("1.0")
    // Other plugins
}

pitest {
  propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
  propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String::class.java)
  // More PITest config...
}
```

Now you can run commands with PITest Helper!

Currently, PITest Helper supports only these properties to customize your PITest command:

| gradle-pitest-plugin property | PITest Helper property     |
|-------------------------------|----------------------------|
| targetClasses                 | pitest.targetClasses       |
| targetTests                   | pitest.targetTests         |


## Compatibility
PITest Helper is compatible with Java and Kotlin projects using Maven or Gradle.
At this time only configuring target classes and tests is allowed, but more options to customize the PITest command will be added soon.

The plugin is compatible with IntelliJ IDEA Community and Ultimate, from versions 2022.3 to 2024.3.


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
