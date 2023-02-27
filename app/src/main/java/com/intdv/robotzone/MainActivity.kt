package com.intdv.robotzone

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.view.isVisible
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.intdv.robotzone.adapters.HumansAdapter
import com.intdv.robotzone.databinding.ActivityMainBinding
import com.intdv.robotzone.models.HumanModel
import com.intdv.robotzone.threads.AwarenessThread
import es.dmoral.toasty.Toasty
import timber.log.Timber

class MainActivity : RobotActivity(), RobotLifecycleCallbacks, HumansAdapter.IHumanListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var _qiContext: QiContext? = null

    private lateinit var humansAdapter: HumansAdapter

    private var mHandler: Handler? = null
    private var _awarenessThread: AwarenessThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        QiSDK.register(this, this)

        setupThreadHandler()

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
            approachSelectedHuman()
        }

        binding.btEngage.setOnClickListener {
            engageSelectedHuman()
        }
    }

    private fun setupThreadHandler() {
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == AwarenessThread.HUMANS_LIST) {
                    val humans: List<Int> = msg.data.getIntegerArrayList("humans_list")?.toList() ?: listOf()
                    humansAdapter.setHumans(humans)

                } else if (msg.what == AwarenessThread.HUMAN_DATA) {
                    val humanData: HumanModel? = msg.data.getParcelable("human_model")
                    loadHumanDataToView(humanData)
                }
            }
        }
    }

    private fun loadHumanDataToView(humanData: HumanModel?) {
        if (humanData != null) {
            with(binding) {
                tvAge.text = getString(R.string.human_age, humanData.age)
                tvGender.text = getString(R.string.human_gender, humanData.gender)
                tvPleasure.text = getString(R.string.human_pleasure_state, humanData.pleasureState)
                tvExcitement.text = getString(R.string.human_excitement_state, humanData.excitementState)
                tvEngagement.text = getString(R.string.human_engagement_state, humanData.engagementIntentionState)
                tvSmile.text = getString(R.string.human_smile_state, humanData.smileState)
                tvAttention.text = getString(R.string.human_attention_state, humanData.attentionState)

                humanData.photo?.let {
                    ivHumanFace.setImageBitmap(it)
                } ?: ivHumanFace.setImageResource(R.drawable.ic_person)
            }
        } else {
            Toasty.error(this, "Error Fetching Human Data", Toasty.LENGTH_LONG).show()
        }
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        Timber.tag(TAG).d("onRobotFocusGained()")
        _qiContext = qiContext

        startAwarenessThread()
    }

    override fun onRobotFocusLost() {
        Timber.tag(TAG).d("onRobotFocusLost()")
        _awarenessThread?.killThread()
        _qiContext = null
    }

    override fun onRobotFocusRefused(reason: String?) {
        Timber.tag(TAG).d("onRobotFocusRefused()")
    }

    private fun startAwarenessThread() {
        object : Thread() {
            override fun run() {
                Timber.tag(TAG).d("Connecting To Thread")
                _awarenessThread = AwarenessThread(_qiContext, mHandler!!)
                _awarenessThread?.start()
            }
        }.start()
    }

    private fun findHumansAround() {
        _awarenessThread?.updateThreadAction(AwarenessThread.FIND_HUMANS)
    }

    private fun recommendHumanToApproach() {
        _awarenessThread?.updateThreadAction(AwarenessThread.RECOMMEND_APPROACH)
    }

    private fun recommendHumanToEngage() {
        _awarenessThread?.updateThreadAction(AwarenessThread.RECOMMEND_ENGAGE)
    }

    override fun onHumanClicked(human: Int) {
        Timber.tag(TAG).i("Human Clicked")
        binding.llHumansData.isVisible = true
        _awarenessThread?.setThreadSelectedHuman(human)
        _awarenessThread?.retrieveCharacteristics()
    }

    private fun approachSelectedHuman() {
        _awarenessThread?.approachHuman()
    }

    private fun engageSelectedHuman() {
        _awarenessThread?.engageHuman()
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