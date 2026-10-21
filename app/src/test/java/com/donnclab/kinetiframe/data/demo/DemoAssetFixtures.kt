package com.donnclab.kinetiframe.data.demo

import java.io.File

object DemoAssetFixtures {
    fun file(assets: File, path: String): File = File(assets, path).also {
        require(it.isFile) { "Missing current bundled asset: $path" }
    }
}
