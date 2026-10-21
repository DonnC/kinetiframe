// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts.catalog

import com.donnclab.kinetiframe.core.contracts.ServiceFailure
import com.donnclab.kinetiframe.core.contracts.ServiceResult

enum class CatalogKind { ACTOR, PERFORMANCE, PACK, BACKGROUND }
data class CatalogItem(val id: String, val title: String, val kind: CatalogKind)
data class CatalogQuery(val cursor: String? = null, val limit: Int = DEFAULT_LIMIT) {
    init {
        require(limit in 1..MAXIMUM_LIMIT)
    }
    companion object {
        const val DEFAULT_LIMIT = 32
        const val MAXIMUM_LIMIT = 100
    }
}
data class CatalogPage(val items: List<CatalogItem>, val nextCursor: String?)

/** Discovery only. Paid delivery/auth is a separate backend-controlled workflow. */
fun interface AssetCatalog {
    suspend fun page(query: CatalogQuery): ServiceResult<CatalogPage>
}
object DisabledAssetCatalog : AssetCatalog {
    override suspend fun page(query: CatalogQuery) = ServiceResult.Failure(ServiceFailure.UNAVAILABLE)
}
