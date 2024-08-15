# pitest-helper

![Build](https://github.com/carmeloquilez/pitest-helper/actions/workflows/build.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/23649-pitest-helper)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/23649-pitest-helper)

## PITest Helper

<!-- Plugin description -->
Plugin to run [PITest](https://pitest.org/) in your Java project using Maven or Gradle.
The plugin builds and runs PITest mutation coverage commands for you.

Requisites:  
- A Maven/Gradle project.
- For Maven: [pitest-maven](https://pitest.org/quickstart/maven/) plugin configured on your project.
- For Gradle: [gradle-pitest-plugin](https://github.com/szpak/gradle-pitest-plugin) plugin configured on your project.

Usage:  
- Right-click on your classes and packages you want to run mutation coverage and click _Run Mutation Coverage..._

Note: depending on your build system, you need to do extra steps to make PITest Helper work.

Are you experiencing problems with the plugin? Do you have any suggestion? You can create an issue at the [Plugin Site](https://github.com/carmeloquilez/pitest-helper/issues)

PITest logo used in this plugin was created by Ling Yeung.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "PITest Helper"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/carmeloquilez/pitest-helper/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Project setup

### Maven project

For projects using Maven, you only need to set up [pitest-maven](https://pitest.org/quickstart/maven/) plugin. You can follow the official PIT instructions for 
Maven at the following link:

[PIT Configuration for Maven](https://pitest.org/quickstart/maven/)

If your project already has the PITest plugin configured, ignore this step.

### Gradle project

For projects using Gradle, you need to follow the next steps.

#### 1. Add the gradle-pitest-plugin to your project

To add the [gradle-pitest-plugin](https://github.com/szpak/gradle-pitest-plugin) plugin, edit your `build.gradle` (or `build.gradle.kts` for Kotlin DSL) file by adding the following code:

#### `build.gradle`

```groovy
plugins {
    id 'info.solidsoft.pitest' version '1.15.0'  // Choose your version
    id 'io.github.cquilezg.properties-manager' version '1.0'
}
```

#### `build.gradle.kts`

```kotlin
plugins {
    id("info.solidsoft.pitest") version ("1.15.0")  // Choose your version
    id("io.github.cquilezg.properties-manager' version '1.0")
}
```
For more details on configuring the gradle-pitest-plugin for PITest in Gradle, visit the [official plugin documentation](https://github.com/szpak/gradle-pitest-plugin).

#### 2. Configure your pitest task to load the properties successfully

#### `build.gradle`

```groovy
pitest {
  propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String)
  propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String)
  propertyManager.bindStringProperty(project, junit5PluginVersion, "pitest.junit5PluginVersion")
  propertyManager.bindBooleanProperty(project, verbose, "pitest.verbose")
}
```

#### `build.gradle.kts`

```kotlin
pitest {
  propertyManager.bindMultiValueProperty(project, targetClasses, "pitest.targetClasses", String::class.java)
  propertyManager.bindMultiValueProperty(project, targetTests, "pitest.targetTests", String::class.java)
  propertyManager.bindStringProperty(project, junit5PluginVersion, "pitest.junit5PluginVersion")
  propertyManager.bindBooleanProperty(project, verbose, "pitest.verbose")
}
```

This table shows the relation between gradle-pitest-plugin properties and PITest Helper properties:

| gradle-pitest-plugin property | PITest Helper property     |
|-------------------------------|----------------------------|
| targetClasses                 | pitest.targetClasses       |
| targetTests                   | pitest.targetTests         |
| junit5PluginVersion           | pitest.junit5PluginVersion |
| verbose                       | pitest.verbose             |



---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
