package com.intdv.robotzone

import android.annotation.SuppressLint
import android.os.*
import com.aldebaran.qi.sdk.design.activity.RobotActivity

@SuppressLint("MissingPermission")
class MainActivity : RobotActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}