package com.nsc9012.blesample.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nsc9012.blesample.R
import com.nsc9012.blesample.devices.DeviceConnectActivity
import kotlinx.android.synthetic.main.fragment_battery.*

class BatteryFragment : Fragment() {
    private lateinit var activity: DeviceConnectActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = getActivity() as DeviceConnectActivity

        return inflater.inflate(R.layout.fragment_battery, container, false)
    }

    override fun onResume() {
        super.onResume()
        battery?.text = ((activity.BatteryLevel).toString() + "%")
    }
}