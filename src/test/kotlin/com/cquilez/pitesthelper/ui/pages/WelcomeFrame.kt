// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.cquilez.pitesthelper.ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.Assertions.fail
import java.time.Duration

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.() -> Unit) {
    find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Welcome Frame")
@DefaultXpath("type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
    private val openProjectButton
        get() = actionLinks(
            byXpath(
                "Open",
                "//div[(@accessiblename='Open' and @accessiblename.key='action.Tabbed.WelcomeScreen.OpenProject.text' and @class='JBOptionButton' and @text='Open' and @text.key='action.Tabbed" +
                        ".WelcomeScreen.OpenProject.text') or @defaulticon='open.svg']"
            )
        )

    private val openButtons
        get() = buttons(
            byXpath("Open", "//div[@visible_text='Open']")
        )

    fun openProject() {
        if (openProjectButton.size == 1) {
            openProjectButton[0].click()
        } else if (openButtons.size == 1) {
            openButtons[0].click()
        } else {
            fail("The project could not be opened")
        }
    }
}