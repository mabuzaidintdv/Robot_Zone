package com.intdv.robotzone

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.core.view.isVisible
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.ApproachHumanBuilder
import com.aldebaran.qi.sdk.builder.EngageHumanBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.geometry.Transform
import com.aldebaran.qi.sdk.`object`.geometry.TransformTime
import com.aldebaran.qi.sdk.`object`.geometry.Vector3
import com.aldebaran.qi.sdk.`object`.human.*
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanAwareness
import com.intdv.robotzone.adapters.HumansAdapter
import com.intdv.robotzone.databinding.ActivityMainBinding
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.sqrt

class MainActivity : RobotActivity(), RobotLifecycleCallbacks, HumansAdapter.IHumanListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var humansAdapter: HumansAdapter

    private var _qiContext: QiContext? = null
    private var _humanAwareness: HumanAwareness? = null
    private var _engagement: Future<Void>? = null

    private var selectedHuman: Human? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContentView(R.layout.activity_main)

        QiSDK.register(this, this)

        humansAdapter = HumansAdapter(this)
        binding.rvHumans.adapter = humansAdapter

        binding.btFind.setOnClickListener {
            findHumansAround()
        }

        binding.btRecommendApproach.setOnClickListener {
            recommendHumanToApproach()
        }

        binding.btRecommendEngage.setOnClickListener {
            recommendHumanToEngage()
        }

        binding.btApproach.setOnClickListener {
            selectedHuman?.let {
                approachSelectedHuman(it)
            }
        }

        binding.btEngage.setOnClickListener {
            selectedHuman?.let {
                engageSelectedHuman(it)
            }
        }
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        Timber.tag(TAG).d("onRobotFocusGained()")
        _qiContext = qiContext
        _humanAwareness = _qiContext?.humanAwareness
    }

    override fun onRobotFocusLost() {
        Timber.tag(TAG).d("onRobotFocusLost()")
        _qiContext = null
    }

    override fun onRobotFocusRefused(reason: String?) {
        Timber.tag(TAG).d("onRobotFocusRefused()")
    }

    private fun findHumansAround() {
        Timber.tag(TAG).d("FIND HUMANS")

        try {
            Thread {
                val humansAround = _humanAwareness?.humansAround

                runOnUiThread {
                    Timber.tag(TAG).i("${humansAround?.size} human(s) around.")

                    humansAround?.let {
                        humansAdapter.setHumans(it)
                    }
                }
            }.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun recommendHumanToApproach() {
        Timber.tag(TAG).d("RECOMMEND HUMANS TO APPROACH")

        try {
            Thread {
                val recommendedHumanToApproach =
                    _humanAwareness?.recommendedHumanToApproach

                runOnUiThread {
                    Timber.tag(TAG).i("Approach: A Human found")

                    recommendedHumanToApproach?.let {
                        humansAdapter.setHumans(listOf(it))
                    }
                }
            }.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun recommendHumanToEngage() {
        Timber.tag(TAG).d("RECOMMEND HUMANS TO ENGAGE")

        try {
            Thread {
                val recommendedHumanToEngage =
                    _humanAwareness?.recommendedHumanToEngage

                runOnUiThread {
                    Timber.tag(TAG).i("Engage: A Human found")

                    recommendedHumanToEngage?.let {
                        humansAdapter.setHumans(listOf(it))
                    }
                }
            }.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onHumanClicked(human: Human) {
        Timber.tag(TAG).i("Human Clicked")
        binding.llHumansData.isVisible = true
        selectedHuman = human
        retrieveCharacteristics(human)
    }

    private fun retrieveCharacteristics(human: Human) {
        // Get the characteristics.

        Thread {
            val age: Int = human.estimatedAge.years
            val gender: Gender = human.estimatedGender
            val pleasureState: PleasureState = human.emotion.pleasure
            val excitementState: ExcitementState = human.emotion.excitement
            val engagementIntentionState: EngagementIntentionState = human.engagementIntention
            val smileState: SmileState = human.facialExpressions.smile
            val attentionState: AttentionState = human.attention

            retrieveDistance(human)
            retrievePhoto(human)

            runOnUiThread {
                // Display the characteristics.
                Timber.tag(TAG).i("Age: $age year(s)")
                Timber.tag(TAG).i("Gender: $gender")
                Timber.tag(TAG).i("Pleasure state: $pleasureState")
                Timber.tag(TAG).i("Excitement state: $excitementState")
                Timber.tag(TAG).i("Engagement state: $engagementIntentionState")
                Timber.tag(TAG).i("Smile state: $smileState")
                Timber.tag(TAG).i("Attention state: $attentionState")

                binding.tvAge.text = getString(R.string.human_age, age)
                binding.tvGender.text = getString(R.string.human_gender, gender)
                binding.tvPleasure.text = getString(R.string.human_pleasure_state, pleasureState)
                binding.tvExcitement.text = getString(R.string.human_excitement_state, excitementState)
                binding.tvEngagement.text = getString(R.string.human_engagement_state, engagementIntentionState)
                binding.tvSmile.text = getString(R.string.human_smile_state, smileState)
                binding.tvAttention.text = getString(R.string.human_attention_state, attentionState)
            }
        }.start()
    }

    private fun retrieveDistance(human: Human) {
        _qiContext?.let {
            val actuation: Actuation = it.actuation
            val robotFrame: Frame = actuation.robotFrame()
            val humanFrame: Frame = human.headFrame

            val distance = computeDistance(humanFrame, robotFrame)
            Timber.tag(TAG).i("Distance: $distance meter(s).")
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

    private fun retrievePhoto(human: Human) {
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
            runOnUiThread {
                binding.ivHumanFace.setImageBitmap(facePicture)
            }
        } else {
            Timber.tag(TAG).i("Picture not available")
            runOnUiThread {
                binding.ivHumanFace.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun approachSelectedHuman(human: Human) {
        _qiContext?.let {
            ApproachHumanBuilder.with(it)
                .withHuman(human)
                .buildAsync()
                .andThenConsume { approachHuman ->
                    approachHuman.addOnHumanIsTemporarilyUnreachableListener {
                        SayBuilder.with(it)
                            .withText("I have troubles to reach you, come closer!")
                            .buildAsync().andThenConsume { say ->
                                Timber.d("say building finished")
                                val futureSay = say.async().run()
                                futureSay.andThenApply {
                                    Timber.d("say finished")
                                }
                            }
                        approachHuman.async().run()
                    }
                }
        }
    }

    private fun engageSelectedHuman(human: Human) {
        _qiContext?.let {
            EngageHumanBuilder.with(it)
                .withHuman(human)
                .buildAsync()
                .andThenConsume { engageHuman ->

                    engageHuman.addOnHumanIsEngagedListener {
                        SayBuilder.with(it)
                            .withText("Hello!... How are you today?")
                            .buildAsync()
                            .andThenConsume { say ->
                                say.async().run()
                            }
                    }

                    engageHuman.addOnHumanIsDisengagingListener {
                        SayBuilder.with(it)
                            .withText("Good bye?")
                            .buildAsync()
                            .andThenConsume { say ->
                                say.async().run()
                            }
                        _engagement?.requestCancellation()
                    }

                    engageHuman.async().run()
                }
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("onDetach()")
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    companion object {
        private const val TAG: String = "MainActivity"
    }
}