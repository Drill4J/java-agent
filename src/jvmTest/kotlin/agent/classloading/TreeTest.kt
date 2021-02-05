/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.classloading

import kotlin.test.*

class LeavesTest {
    @Test
    fun `all nodes are equal`() {
        assertEquals(Node("1"), Node("2"))
    }

    @Test
    fun `parents simple case`() = with(Node("root")) {
        child("node") {
            val leaf = child("leaf")
            val parents = leaf.parents(Node::parent)
            assertEquals(listOf("root", "node"), parents.map(Node::data))
        }
        assertEquals(emptyList(), parents(Node::parent))
    }

    @Test
    fun `mutRefSet can store duplicated data`() {
        val mutRefSet = mutRefSet<Node>()
        val node = Node("1")
        mutRefSet.add(node)
        mutRefSet.add(node)
        mutRefSet.add(Node("1"))
        assertEquals(2, mutRefSet.count())
    }

    @Test
    fun `mutRefSet preserves order and removes duplications`() {
        val mutRefSet = mutRefSet<Node>()
        val node2 = Node("2")
        val nodes = listOf(Node("3"), node2, node2, Node("1"))
        mutRefSet.addAll(nodes)
        assertEquals(listOf("3", "2", "1"), mutRefSet.map(Node::data))
    }

    @Test
    fun `leaves are found correctly`() = with(Node("root")) {
        child("leaf1")
        child("node1") {
            child("leaf2")
            child("leaf3")
        }
        child("node2") {
            child("node21") {
                child("leaf4")
            }
            child("node22") {
                child("node221") {
                    child("leaf5")
                }
            }
        }
        val expected = setOf("leaf1", "leaf2", "leaf3", "leaf4", "leaf5")
        allNodes.shuffle()
        val leaves = allNodes.leaves(Node::parent)
        val leafData = leaves.toList().map(Node::data).toSet()
        assertEquals(expected, leafData)
        val leavesWithExtra = leaves.toListWith(Node("other"))
        assertEquals(6, leavesWithExtra.count())
    }
}

private data class Node(
    val data: String,
    val parent: Node? = null
) {
    val allNodes: MutableList<Node> = parent?.allNodes ?: mutableListOf()

    override fun equals(other: Any?) = true

    override fun hashCode() = 0

    override fun toString() = data
}

private fun Node.child(data: String, block: Node.() -> Unit = {}) = copy(data = data, parent = this)
    .apply { allNodes.add(this) }
    .apply(block)
