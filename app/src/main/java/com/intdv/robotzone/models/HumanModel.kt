package com.intdv.robotzone.models

import android.graphics.Bitmap
import android.os.Parcelable
import com.aldebaran.qi.sdk.`object`.human.*
import kotlinx.parcelize.Parcelize

/**
 * Created by Mohamad Abuzaid on 2/26/2023.
 */
@Parcelize
data class HumanModel(
    val age: Int,
    val gender: Gender,
    val pleasureState: PleasureState,
    val excitementState: ExcitementState,
    val engagementIntentionState: EngagementIntentionState,
    val smileState: SmileState,
    val attentionState: AttentionState,
    var distance: Double = 0.0,
    var photo: Bitmap? = null,
) : Parcelable