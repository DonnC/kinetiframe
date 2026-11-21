package com.donnclab.kinetiframe.studio.drawing

internal data class ParticleTip(
    val radiusScale: Float,
    val spacingScale: Float,
    val scatterScale: Float,
    val alongStrokeJitter: Float,
    val opacity: Float,
    val particlesPerStamp: Int,
    val distribution: ParticleDistribution = ParticleDistribution.UNIFORM,
    val maximumSpacing: Float = Float.MAX_VALUE,
    val maximumParticleRadius: Float = Float.MAX_VALUE,
    val clusterSpan: Int = 1,
)

internal enum class ParticleDistribution { UNIFORM, CLUSTERED }

internal data class StrandProfile(
    val strands: Int,
    val scatter: Float,
    val lengthJitter: Float,
    val strokeScale: Float,
    val opacity: Float,
)

internal data class PhaseParticleProfile(
    val particles: Int,
    val radius: Float,
    val spacing: Float,
    val scatter: Float,
    val alongJitter: Float,
    val opacity: Float,
    val maximumSpacing: Float,
    val skipProbability: Float = 0f,
)

internal data class SoftLayer(val radiusScale: Float, val opacity: Float)

internal data class SoftTip(
    val radiusScale: Float,
    val opacity: Float,
    val spacingScale: Float,
    val layers: List<SoftLayer>,
)

internal val PencilTip = ParticleTip(0.18f, 0.16f, 0.20f, 0.06f, 0.35f, 2, maximumSpacing = 1.6f)

internal val CrayonTip = ParticleTip(
    0.13f,
    0.30f,
    0.76f,
    0.42f,
    0.58f,
    7,
    ParticleDistribution.CLUSTERED,
    maximumSpacing = 1.35f,
    maximumParticleRadius = 2.2f,
    clusterSpan = 4,
)

internal val HairProfile = StrandProfile(3, 0.24f, 0.35f, 0.16f, 0.62f)

internal val FurProfile = StrandProfile(5, 0.58f, 0.70f, 0.13f, 0.42f)

internal val SketchProfile = PhaseParticleProfile(3, 0.16f, 0.28f, 0.24f, 0.10f, 0.34f, 1.4f)

internal val SprayProfile = PhaseParticleProfile(8, 0.10f, 0.44f, 1.25f, 0.82f, 0.32f, 2.4f, 0.46f)

internal val StippleProfile = PhaseParticleProfile(2, 0.24f, 0.70f, 0.42f, 0.18f, 0.58f, 3.2f)

internal val GrainProfile = PhaseParticleProfile(3, 0.12f, 0.32f, 0.28f, 0.16f, 0.18f, 1.8f, 0.18f)

internal val SoftBrushTip =
    SoftTip(0.90f, 0.82f, 0.18f, listOf(SoftLayer(1.26f, 0.08f), SoftLayer(1.06f, 0.16f), SoftLayer(0.78f, 0.38f)))

internal val AirbrushTip =
    SoftTip(0.95f, 0.45f, 0.24f, listOf(SoftLayer(1.65f, 0.04f), SoftLayer(1.28f, 0.08f), SoftLayer(0.90f, 0.14f)))
