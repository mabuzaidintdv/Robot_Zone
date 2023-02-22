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
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.`object`.geometry.Transform
import com.aldebaran.qi.sdk.`object`.geometry.TransformTime
import com.aldebaran.qi.sdk.`object`.geometry.Vector3
import com.aldebaran.qi.sdk.`object`.human.*
import com.aldebaran.qi.sdk.`object`.humanawareness.EngageHuman
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanAwareness
import com.intdv.robotzone.adapters.HumansAdapter
import com.intdv.robotzone.databinding.ActivityMainBinding
import com.intdv.robotzone.interfaces.RobotDelegate
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.math.sqrt

class MainActivity : RobotActivity(), RobotLifecycleCallbacks, RobotDelegate, HumansAdapter.IHumanListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val _viewModel: MainViewModel by inject()
    private lateinit var humansAdapter: HumansAdapter

    private var _qiContext: QiContext? = null
    private var _humanAwareness: HumanAwareness? = null
    private var _engagement: Future<Void>? = null
    private var _approach: Future<Void>? = null

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
            _viewModel.selectedHuman?.let {
                approachSelectedHuman(it)
            }
        }

        binding.btEngage.setOnClickListener {
            _viewModel.selectedHuman?.let {
                engageSelectedHuman(it)
            }
        }
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        Timber.tag(TAG).d("onRobotFocusGained()")
        _qiContext = qiContext
        _humanAwareness = _qiContext?.humanAwareness

        findHumansAround()
        recommendHumanToApproach()
    }

    override fun onRobotFocusLost() {
        Timber.tag(TAG).d("onRobotFocusLost()")
        _qiContext = null
    }

    override fun onRobotFocusRefused(reason: String?) {
        Timber.tag(TAG).d("onRobotFocusRefused()")
    }

    private fun findHumansAround() {
        val humansAroundFuture: Future<List<Human>>? = _humanAwareness?.async()?.humansAround
        humansAroundFuture?.andThenConsume { humansAround: List<Human> ->
            Timber.tag(TAG).i("${humansAround.size} human(s) around.")
            humansAdapter.setHumans(humansAround)
        }
    }

    private fun recommendHumanToApproach() {
        val humansAroundFuture: Future<Human>? = _humanAwareness?.async()?.recommendedHumanToApproach
        humansAroundFuture?.andThenConsume { human: Human ->
            //humansAdapter.setHumans(listOf(human))
            approachSelectedHuman(human)
        }
    }

    private fun recommendHumanToEngage() {
        val humansAroundFuture: Future<Human>? = _humanAwareness?.async()?.recommendedHumanToEngage
        humansAroundFuture?.andThenConsume { human: Human ->
            //humansAdapter.setHumans(listOf(human))
            engageSelectedHuman(human)
        }
    }

    override fun onHumanClicked(human: Human) {
        Timber.tag(TAG).i("Human Clicked")
        binding.llHumansData.isVisible = true
        _viewModel.selectedHuman = human
        retrieveCharacteristics(human)
    }

    private fun retrieveCharacteristics(human: Human) {
        // Get the characteristics.
        val age: Int = human.estimatedAge.years
        binding.tvAge.text = getString(R.string.human_age, age)

        val gender: Gender = human.estimatedGender
        binding.tvGender.text = getString(R.string.human_gender, gender)

        val pleasureState: PleasureState = human.emotion.pleasure
        binding.tvPleasure.text = getString(R.string.human_pleasure_state, pleasureState)

        val excitementState: ExcitementState = human.emotion.excitement
        binding.tvExcitement.text = getString(R.string.human_excitement_state, excitementState)

        val engagementIntentionState: EngagementIntentionState = human.engagementIntention
        binding.tvEngagement.text = getString(R.string.human_engagement_state, engagementIntentionState)

        val smileState: SmileState = human.facialExpressions.smile
        binding.tvSmile.text = getString(R.string.human_smile_state, smileState)

        val attentionState: AttentionState = human.attention
        binding.tvAttention.text = getString(R.string.human_attention_state, attentionState)

        retrieveDistance(human)
        retrievePhoto(human)

        // Display the characteristics.
        Timber.tag(TAG).i("Age: $age year(s)")
        Timber.tag(TAG).i("Gender: $gender")
        Timber.tag(TAG).i("Pleasure state: $pleasureState")
        Timber.tag(TAG).i("Excitement state: $excitementState")
        Timber.tag(TAG).i("Engagement state: $engagementIntentionState")
        Timber.tag(TAG).i("Smile state: $smileState")
        Timber.tag(TAG).i("Attention state: $attentionState")
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
            binding.ivHumanFace.setImageBitmap(facePicture)
        } else {
            Timber.tag(TAG).i("Picture not available")
            binding.ivHumanFace.setImageResource(R.drawable.ic_person)
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

    private fun approachSelectedHuman(human: Human) {
        _qiContext?.let {
            val approachHuman = ApproachHumanBuilder.with(it)
                .withHuman(human)
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

    private fun engageSelectedHuman(human: Human) {
        _qiContext?.let {
            val engageHuman: EngageHuman = EngageHumanBuilder.with(it)
                .withHuman(human)
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

    override fun onDestroy() {
        Timber.tag(TAG).d("onDetach()")
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    companion object {
        private const val TAG: String = "MainActivity"
    }
}