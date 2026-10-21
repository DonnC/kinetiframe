// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class)
internal fun <T> SerialDescriptor.requiredField(value: T?, index: Int): T =
    value ?: throw MissingFieldException(getElementName(index), serialName)
