package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.common.ui.ContextIconAction
import com.donnclab.kinetiframe.common.ui.ContextIconButton
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SwitchPerformanceButton(
    placement: ActorPlacementReference,
    projectId: String,
    performanceRepository: PerformanceRepository,
    onSwitchPerformance: (ActorPlacementReference, PerformanceDocument) -> Unit,
    onCreatePerformance: ((String, String) -> Unit)?,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var performances by remember(placement.actorId) { mutableStateOf<List<PerformanceDocument>>(emptyList()) }
    var error by remember(placement.actorId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(menuOpen, placement.actorId) {
        if (!menuOpen) return@LaunchedEffect
        when (val result = withContext(Dispatchers.IO) { performanceRepository.listForActor(placement.actorId) }) {
            is ActorStoreResult.Value -> {
                performances = result.value
                error = null
            }

            is ActorStoreResult.Failure -> error = result.message
        }
    }
    Box {
        ContextIconButton(ContextIconAction(Lucide.Clapperboard, "Switch performance", { menuOpen = true }))
        DropdownMenu(menuOpen, { menuOpen = false }) {
            if (error != null) {
                DropdownMenuItem(text = { Text("Couldn't load performances") }, enabled = false, onClick = {})
            } else {
                PerformanceChooserItems(
                    performances = performances,
                    excludePerformanceId = placement.performanceId,
                    onCreatePerformance = onCreatePerformance?.let { create ->
                        {
                            create(placement.actorId, projectId)
                            menuOpen = false
                        }
                    },
                    onChoose = { performance ->
                        scope.launch {
                            when (
                                val result =
                                    withContext(Dispatchers.IO) {
                                        performanceRepository.copyIntoProject(performance, projectId)
                                    }
                            ) {
                                is ActorStoreResult.Value -> {
                                    onSwitchPerformance(placement, result.value)
                                    menuOpen =
                                        false
                                }

                                is ActorStoreResult.Failure -> error = result.message
                            }
                        }
                    },
                )
            }
        }
    }
}
