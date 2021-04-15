package com.nsc9012.blesample.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.nsc9012.blesample.R
import com.nsc9012.blesample.devices.DeviceConnectActivity
import kotlinx.android.synthetic.main.fragment_timer.*

class TimerFragment : Fragment() {
    private lateinit var activity: DeviceConnectActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { activity = getActivity() as DeviceConnectActivity
        counter()
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    fun setText(text : String) {
        countdown?.text = text + "/15"
    }

    fun counter() {
        val countDownTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) % 60
                val timeLeftFormattedsec = String.format("%02d", seconds)
                setText(timeLeftFormattedsec)
            }

            override fun onFinish() {
                if(activity.BatteryLevel != 0) {
                    view?.findNavController()?.navigate(R.id.action_timerFragment_to_batteryFragment)
                }
                else {
                    view?.findNavController()?.navigate(R.id.action_timerFragment_to_failedFragment)
                }
            }
        }.start()
    }
}