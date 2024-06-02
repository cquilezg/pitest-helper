// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.cquilez.pitesthelper.ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.projectToolWindow(function: ProjectToolWindow.() -> Unit) {
    find(ProjectToolWindow::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Project Tool Window")
@DefaultXpath("InternalDecoratorImpl type", "//div[@class='InternalDecoratorImpl' and @accessiblename='Project Tool Window']")
class ProjectToolWindow(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    val collapseAllButton
        get() = button(
            byXpath(
                "ActionButton type",
                "//div[@class='ToolWindowHeader']//div[@class='JPanel']//div[@class='TabPanel']//div[@class='ContentComboLabel' and @text='Project']/../../../div[@class='JPanel']//div[@class='ActionToolbarImpl']//div[@class='ActionButton' and @myicon='collapseall.svg']"
            )
        )
}