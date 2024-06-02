// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.cquilez.pitesthelper.ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.mutationCoverageDialog(function: MutationCoverageDialog.() -> Unit) {
    find(MutationCoverageDialog::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Mutation Coverage Dialog")
@DefaultXpath("MyDialog type", "//div[@class='MyDialog' and @title='Mutation Coverage']")
class MutationCoverageDialog(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    val commandTextArea
        get() = textArea(byXpath("Command", "//div[@class='JBTextArea']"))

    val cancelButton
        get() = button(
            byXpath("JButton type", "//div[@class='JButton' and @text='Cancel']")
        )
}