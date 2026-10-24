// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.storage

/** The IDs that name stored folders and files: 1 to 80 letters, digits, `_` or `-`, starting with a letter or digit. */
object StorageIds {
    private val pattern = Regex("[a-zA-Z0-9][a-zA-Z0-9_-]{0,79}")

    fun requireValid(value: String, label: String) = require(value.matches(pattern)) { "Invalid $label" }
}
