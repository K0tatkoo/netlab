package com.n3d.netlab.core

/**
 * An exercise is walked one stage at a time rather than answered as one big
 * form.
 *
 * Each stage is exactly one move of the paper method, checked on its own. That
 * matters more than it sounds: a learner who fills a whole VLSM table in and
 * gets "9 of 20 fields wrong" has learned nothing, because every later answer
 * was built on the first mistake. Stopping at the stage that actually went
 * wrong is the difference between practice and guessing.
 */
enum class VlsmStage {
    /** Drag the subnets into largest-first order. */
    Order,

    /** How many addresses does each one need, and what prefix is that? */
    Size,

    /** Where does each block start and end? */
    Place,

    Done,
    ;

    val index: Int get() = ordinal
}

enum class AnalyzeStage {
    /** Prefix → dotted mask, and the block size that comes with it. */
    Mask,

    /** Round the address down to the start of its block. */
    Network,

    /** Network + block − 1. */
    Broadcast,

    /** The two edges and the count between them. */
    Hosts,

    Done,
    ;

    val index: Int get() = ordinal
}

/** Which fields a stage asks for — the single source of truth for grading it. */
val AnalyzeStage.fields: List<AnalyzeField>
    get() = when (this) {
        AnalyzeStage.Mask -> listOf(AnalyzeField.Mask, AnalyzeField.BlockSize)
        AnalyzeStage.Network -> listOf(AnalyzeField.Network)
        AnalyzeStage.Broadcast -> listOf(AnalyzeField.Broadcast)
        AnalyzeStage.Hosts -> listOf(AnalyzeField.FirstHost, AnalyzeField.LastHost, AnalyzeField.Hosts)
        AnalyzeStage.Done -> emptyList()
    }

val VlsmStage.fields: List<VlsmField>
    get() = when (this) {
        VlsmStage.Size -> listOf(VlsmField.Prefix)
        VlsmStage.Place -> listOf(VlsmField.Network, VlsmField.FirstHost, VlsmField.LastHost, VlsmField.Broadcast)
        else -> emptyList()
    }

/** Total stages a learner walks through, excluding [VlsmStage.Done]. */
const val VLSM_STAGE_COUNT = 3
const val ANALYZE_STAGE_COUNT = 4
