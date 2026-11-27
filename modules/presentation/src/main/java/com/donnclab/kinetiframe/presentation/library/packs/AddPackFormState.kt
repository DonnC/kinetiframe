package com.donnclab.kinetiframe.presentation.library.packs

/** Everything New Pack's form screen collects, bundled into one value so [AddPackScreen] and its
 *  host never need more than one parameter to read or update it. [dimension] is always one of the
 *  three square sizes [AddPackScreen] offers -- 512, 768 or 1024. */
data class AddPackFormState(val name: String = "", val dimension: Int = 512)

/** Callbacks [AddPackScreen] needs, bundled so the composable itself takes state + one callbacks
 *  value rather than four separate lambda parameters. */
data class AddPackCallbacks(
    val onNameChange: (String) -> Unit,
    val onDimensionChange: (Int) -> Unit,
    val onBack: () -> Unit,
    val onCreate: () -> Unit,
)
