package com.intdv.robotzone.threads

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.builder.ApproachHumanBuilder
import com.aldebaran.qi.sdk.builder.EngageHumanBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.`object`.geometry.Transform
import com.aldebaran.qi.sdk.`object`.geometry.TransformTime
import com.aldebaran.qi.sdk.`object`.geometry.Vector3
import com.aldebaran.qi.sdk.`object`.human.*
import com.aldebaran.qi.sdk.`object`.humanawareness.EngageHuman
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanAwareness
import com.intdv.robotzone.models.HumanModel
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.sqrt

private const val TAG: String = "AwarenessThread"

class AwarenessThread(private val _qiContext: QiContext?, private val handler: Handler?) : Thread() {

    private var run = true
    private var action = FIND_HUMANS
    private var selectedHuman: Int = 0
    private val _humansMap = HashMap<Int, Human>()

    private val _humanAwareness: HumanAwareness? = _qiContext?.humanAwareness
    private var _engagement: Future<Void>? = null
    private var _approach: Future<Void>? = null

    override fun run() {

        while (run) {
            when (action) {
                FIND_HUMANS -> {
                    Timber.tag(TAG).d("FIND HUMANS")
                    try {
                        val humansAroundFuture: Future<List<Human>>? = _humanAwareness?.async()?.humansAround
                        humansAroundFuture?.andThenConsume { humansAround: List<Human> ->
                            Timber.tag(TAG).i("${humansAround.size} human(s) around.")
                            _humansMap.clear()
                            humansAround.forEachIndexed { i, v ->
                                _humansMap[i] = v
                            }
                            val arraylist = ArrayList<Int>(_humansMap.keys)
                            val bundle = Bundle().apply {
                                putIntegerArrayList("humans_list", arraylist)
                            }
                            sendMessage(HUMANS_LIST, bundle)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
                }
                RECOMMEND_APPROACH -> {
                    Timber.tag(TAG).d("RECOMMEND HUMANS TO APPROACH")
                    try {
                        val humansAroundFuture: Future<Human>? = _humanAwareness?.async()?.recommendedHumanToApproach
                        humansAroundFuture?.andThenConsume { human: Human ->
                            Timber.tag(TAG).i("Approach: A Human found")
                            _humansMap.clear()
                            _humansMap[0] = human
                            val bundle = Bundle().apply {
                                putIntegerArrayList("humans_list", arrayListOf(0))
                            }
                            sendMessage(HUMANS_LIST, bundle)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
                }
                RECOMMEND_ENGAGE -> {
                    Timber.tag(TAG).d("RECOMMEND HUMANS TO ENGAGE")
                    try {
                        val humansAroundFuture: Future<Human>? = _humanAwareness?.async()?.recommendedHumanToEngage
                        humansAroundFuture?.andThenConsume { human: Human ->
                            Timber.tag(TAG).i("Engage: A Human found")
                            _humansMap.clear()
                            _humansMap[0] = human
                            val bundle = Bundle().apply {
                                putIntegerArrayList("humans_list", arrayListOf(0))
                            }
                            sendMessage(HUMANS_LIST, bundle)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
                }
            }
        }
    }

    fun retrieveCharacteristics() {
        // Get the characteristics.
        val human = _humansMap[selectedHuman]!!
        val age: Int = human.estimatedAge.years
        val gender: Gender = human.estimatedGender
        val pleasureState: PleasureState = human.emotion.pleasure
        val excitementState: ExcitementState = human.emotion.excitement
        val engagementIntentionState: EngagementIntentionState = human.engagementIntention
        val smileState: SmileState = human.facialExpressions.smile
        val attentionState: AttentionState = human.attention

        val humanModel = HumanModel(
            age, gender, pleasureState, excitementState, engagementIntentionState, smileState, attentionState
        )
        val bundle = Bundle().apply {
            putParcelable("human_model", humanModel)
        }
        sendMessage(HUMAN_DATA, bundle)

        retrieveDistance(human, humanModel)
        retrievePhoto(human, humanModel)

        // Display the characteristics.
        Timber.tag(TAG).i("Age: $age year(s)")
        Timber.tag(TAG).i("Gender: $gender")
        Timber.tag(TAG).i("Pleasure state: $pleasureState")
        Timber.tag(TAG).i("Excitement state: $excitementState")
        Timber.tag(TAG).i("Engagement state: $engagementIntentionState")
        Timber.tag(TAG).i("Smile state: $smileState")
        Timber.tag(TAG).i("Attention state: $attentionState")
    }

    private fun retrieveDistance(human: Human, humanModel: HumanModel) {
        _qiContext?.let {
            val actuation: Actuation = it.actuation
            val robotFrame: Frame = actuation.robotFrame()
            val humanFrame: Frame = human.headFrame

            val distance = computeDistance(humanFrame, robotFrame)
            Timber.tag(TAG).i("Distance: $distance meter(s).")
            humanModel.distance = distance

            val bundle = Bundle().apply {
                putParcelable("human_model", humanModel)
            }
            sendMessage(HUMAN_DATA, bundle)
        }
    }

    private fun computeDistance(humanFrame: Frame, robotFrame: Frame): Double {
        // Get the TransformTime between the human frame and the robot frame.
        val transformTime: TransformTime = humanFrame.computeTransform(robotFrame)
        // Get the transform.
        val transform: Transform = transformTime.transform
        val translation: Vector3 = transform.translation
        // Get the x and y components of the translation.
        val x = translation.x
        val y = translation.y
        // Compute the distance and return it.
        return sqrt(x * x + y * y)
    }

    private fun retrievePhoto(human: Human, humanModel: HumanModel) {
        // Get face picture.
        val facePictureBuffer: ByteBuffer = human.facePicture.image.data
        facePictureBuffer.rewind()
        val pictureBufferSize: Int = facePictureBuffer.remaining()
        val facePictureArray = ByteArray(pictureBufferSize)
        facePictureBuffer.get(facePictureArray)

        // Test if the robot has an empty picture
        if (pictureBufferSize != 0) {
            Timber.tag(TAG).i("Picture available")
            val facePicture = BitmapFactory.decodeByteArray(facePictureArray, 0, pictureBufferSize)
            humanModel.photo = facePicture

            val bundle = Bundle().apply {
                putParcelable("human_model", humanModel)
            }
            sendMessage(HUMAN_DATA, bundle)

        } else {
            Timber.tag(TAG).i("Picture not available")
        }
    }

    fun approachHuman() {
        _qiContext?.let {
            val approachHuman = ApproachHumanBuilder.with(it)
                .withHuman(_humansMap[selectedHuman])
                .build()

            approachHuman.addOnHumanIsTemporarilyUnreachableListener {
                val say = SayBuilder.with(it)
                    .withText("I have troubles to reach you, come closer!")
                    .build()
                say.run()
            }

            _approach = approachHuman.async().run()
        }
    }

    fun engageHuman() {
        _qiContext?.let {
            val engageHuman: EngageHuman = EngageHumanBuilder.with(it)
                .withHuman(_humansMap[selectedHuman])
                .build()

            val say: Say = SayBuilder.with(it)
                .withText("Hello!... How are you today?")
                .build()

            engageHuman.addOnHumanIsEngagedListener { say.run() }

            engageHuman.addOnHumanIsDisengagingListener {
                say.run()
                _engagement?.requestCancellation()
            }

            _engagement = engageHuman.async().run()
        }
    }

    private fun sendMessage(what: Int, bundle: Bundle) {
        val message = handler?.obtainMessage(what)
        message?.data = bundle
        message?.sendToTarget()
    }

    /* Call this from the main activity to send data to the remote device */
    fun updateThreadAction(action: Int) {
        Timber.tag(TAG).d("Action: $action")
        this.action = action
    }

    fun setThreadSelectedHuman(humanKey: Int) {
        Timber.tag(TAG).d("Set Human")
        this.selectedHuman = humanKey
    }

    fun killThread() {
        this.run = false
    }

    companion object {
        const val HUMANS_LIST = 110
        const val HUMAN_DATA = 111

        const val FIND_HUMANS = 1
        const val RECOMMEND_APPROACH = 2
        const val RECOMMEND_ENGAGE = 3
    }
}