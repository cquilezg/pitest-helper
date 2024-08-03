package com.cquilez.pitesthelper.ui.components

import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText

class Node(val remoteTexts: MutableList<RemoteText>, val parent: Node?, val children: MutableList<Node>) {
}