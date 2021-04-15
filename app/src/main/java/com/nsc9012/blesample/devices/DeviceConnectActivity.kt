package com.nsc9012.blesample.devices

import android.bluetooth.BluetoothGattCharacteristic
import android.content.*
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import androidx.navigation.findNavController
import com.nsc9012.blesample.R
import com.nsc9012.blesample.bluetoothSevice.BluetoothService


class DeviceConnectActivity : AppCompatActivity() {
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mBluetoothLeService: BluetoothService? = null
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    public var BatteryLevel = 0


    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            Log.e(TAG, "mBLUETOOTHSERVICE CONNECTING")
            val flag = mBluetoothLeService!!.connect(mDeviceAddress)

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e(TAG, "mBLUETOOTHSERVICE DISCONNECTED")
            mBluetoothLeService = null
            finish()
        }
    }

    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                val countDownTimer = object : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val seconds = (millisUntilFinished / 1000) % 60
                        val timeLeftFormattedsec = String.format("%02d", seconds)
                    }
                    override fun onFinish() {
                        findNavController(R.id.nav_host_fragment2).navigate(R.id.action_connectedFragment_to_timerFragment)
                    }
                }.start()
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                Log.e("STATUS", "disconnected")
                finish()
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                mBluetoothLeService!!.enableTXNotification()

                val countDownTimer = object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        mBluetoothLeService!!.getbattery()
                        mBluetoothLeService!!.setCharacteristicNotification()
                    }
                    override fun onFinish() {
                    }
                }.start()

            } else if (BluetoothService.ACTION_DATA_AVAILABLE == action) {
                BatteryLevel = intent.getStringExtra(BluetoothService.EXTRA_DATA).toInt()
                Log.e("EXTRA DATA", intent.getStringExtra(BluetoothService.EXTRA_DATA))
            }
        }
    }
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        val gattServiceIntent = Intent(this, BluetoothService::class.java)
        try {
            val flag = bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
            Log.e(TAG, "service: $flag")
        } catch (e: Exception) {
            Log.e(TAG, "service: Exception:: " + e.message)
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.e(TAG, "Connect request result=" + result)
        }
        else {
            Log.e(TAG, "mBluetoothLeService is null")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        mBluetoothLeService?.disconnect()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    companion object {
        private val TAG = DeviceConnectActivity::class.java.getSimpleName()

        @JvmField var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        @JvmField var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}
