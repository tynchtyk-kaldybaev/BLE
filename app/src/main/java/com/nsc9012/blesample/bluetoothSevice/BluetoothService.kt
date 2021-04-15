package com.nsc9012.blesample.bluetoothSevice

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.util.Log
import java.util.*

class BluetoothService : Service() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED
    private val descriptorWriteQueue: Queue<BluetoothGattDescriptor> = LinkedList()

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.e(TAG, "Attempting to start service discovery:" + mBluetoothGatt!!.discoverServices())
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.e(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    fun enableTXNotification() {
        if (mBluetoothGatt != null) {
            var RxService = mBluetoothGatt!!.getService(bt_Custom_Service)
            val TxChar = RxService.getCharacteristic(bt_Custom_Charachteristic_TX)
            val RxChar = RxService.getCharacteristic(bt_Custom_Charachteristic_RX)
            if (TxChar != null) {
                mBluetoothGatt!!.setCharacteristicNotification(TxChar, true)
                mBluetoothGatt!!.setCharacteristicNotification(RxChar, true)
            }
            if (RxChar != null) {
                mBluetoothGatt!!.setCharacteristicNotification(TxChar, true)
                mBluetoothGatt!!.setCharacteristicNotification(RxChar, true)
            }

            val descriptor = TxChar!!.getDescriptor(bt_Custom_Descriptor_OF_TX)

            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    fun setCharacteristicNotification() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
            return

        val characteristic = mBluetoothGatt!!.getService(bt_Custom_Service).getCharacteristic(bt_Custom_Charachteristic_RX)
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)

        val descriptor = BluetoothGattDescriptor(bt_Custom_Descriptor_OF_TX, BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)

        characteristic.addDescriptor(descriptor)
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        descriptorWriteQueue.add(descriptor)
        if (descriptorWriteQueue.size == 1)
           mBluetoothGatt!!.writeDescriptor(descriptor)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    public fun getbattery() {

        val customService = mBluetoothGatt!!.getService(bt_Custom_Service)
        if (customService == null)
            return
        val customCharacteristic = customService.getCharacteristic(bt_Custom_Charachteristic_RX)
        if (customCharacteristic == null)
            return

        var sendM = ByteArray(5)
        sendM[0] = 0x02
        sendM[1] = 0x61
        sendM[2] = 0x00
        sendM[3] = 0x00
        sendM[4] = 0x03

        customCharacteristic.setValue(sendM)
        mBluetoothGatt!!.writeCharacteristic(customCharacteristic)
    }


    private fun broadcastUpdate(action: String,
                                characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2).toString())
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: BluetoothService
            get() = this@BluetoothService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()

    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            return false
        }
        return true
    }

    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
                && mBluetoothGatt != null) {
            if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            return false
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }


    companion object {
        private val TAG = BluetoothService::class.java.simpleName

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2

        val bt_Custom_Service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val bt_Custom_Charachteristic_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val bt_Custom_Charachteristic_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val bt_Custom_Descriptor_OF_TX = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

    }
}