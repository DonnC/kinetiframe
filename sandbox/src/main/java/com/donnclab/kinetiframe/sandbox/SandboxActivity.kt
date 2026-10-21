// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.donnclab.kinetiframe.sandbox.rigging.RiggingRoute

class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { RiggingRoute() } }
    }
}
