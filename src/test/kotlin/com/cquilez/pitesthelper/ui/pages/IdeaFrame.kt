// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.cquilez.pitesthelper.ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.fixtures.JMenuBarFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration

fun RemoteRobot.idea(function: IdeaFrame.() -> Unit) {
    find<IdeaFrame>(timeout = Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Idea frame")
@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val projectToolWindow
        get() = button(
            byXpath(
                "StripeButton type (vertical left bar)",
                "//div[@class='StripeButton' and @text='Project']"
            )
        )

    val projectVerticalBar
        get() = jLabels(
            byXpath(
                "ContentComboLabel type",
                "//div[@class='ToolWindowHeader']//div[@class='JPanel']//div[@class='TabPanel']//div[@class='ContentComboLabel' and @text='Project']"
            )
        )

    val menuBar: JMenuBarFixture
        get() = step("Menu...") {
            return@step remoteRobot.find(JMenuBarFixture::class.java, JMenuBarFixture.byType())
        }

    fun isMutationCoverageOpen() =
        findAll(
            MutationCoverageDialog::class.java,
            byXpath("//div[@class='MyDialog' and @title='Mutation Coverage']")
        ).isNotEmpty()

    @JvmOverloads
    fun dumbAware(timeout: Duration = Duration.ofMinutes(5), function: () -> Unit) {
        step("Wait for smart mode") {
            waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
                runCatching { isDumbMode().not() }.getOrDefault(false)
            }
            function()
            step("..wait for smart mode again") {
                waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
                    isDumbMode().not()
                }
            }
        }
    }

    fun isDumbMode(): Boolean {
        return callJs(
            """
            const frameHelper = com.intellij.openapi.wm.impl.ProjectFrameHelper.getFrameHelper(component)
            if (frameHelper) {
                const project = frameHelper.getProject()
                project ? com.intellij.openapi.project.DumbService.isDumb(project) : true
            } else { 
                true 
            }
        """, true
        )
    }

    fun modulesJs(): Int {
        return callJs("""importPackage(com.intellij.openapi.wm.impl);importPackage(com.intellij.openapi.module);const frameHelper = ProjectFrameHelper.getFrameHelper(component);if (frameHelper) {const project = frameHelper.getProject();ModuleManager.getInstance(project).modules.length;}""") as Int
    }
}