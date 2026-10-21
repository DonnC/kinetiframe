// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts.observability

import com.donnclab.kinetiframe.core.contracts.ServiceFailure

enum class WorkflowOperation { DRAWING, RIGGING, PERFORMANCE, EXPORT, BACKUP }
enum class OperationOutcome { SUCCEEDED, CANCELLED, FAILED }
data class UsageEvent(val operation: WorkflowOperation, val outcome: OperationOutcome)
data class PerformanceSample(val operation: WorkflowOperation, val elapsedMicros: Long) {
    init {
        require(elapsedMicros >= 0)
    }
}
data class CrashIncident(val origin: WorkflowOperation, val failure: ServiceFailure)

/** Only safe categorical events. Product names, frames, project paths and SDK objects are excluded.
 * Adapters own bounded buffering/consent; telemetry must never run inline in pointer/render loops. */
fun interface AnalyticsSink {
    suspend fun record(event: UsageEvent)
}
fun interface DiagnosticsSink {
    suspend fun record(sample: PerformanceSample)
}
fun interface CrashReporter {
    suspend fun report(incident: CrashIncident)
}

object NoOpAnalytics : AnalyticsSink {
    override suspend fun record(event: UsageEvent) = Unit
}
object NoOpDiagnostics : DiagnosticsSink {
    override suspend fun record(sample: PerformanceSample) = Unit
}
object NoOpCrashReporter : CrashReporter {
    override suspend fun report(incident: CrashIncident) = Unit
}
