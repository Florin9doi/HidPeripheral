package com.blackshark.hidperipheral

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import com.blackshark.hidperipheral.databinding.ActivityGamepadBinding

class GamepadActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityGamepadBinding
    private lateinit var mSensorManager: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGamepadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerKeyButton()
        HidConsts.cleanKbd()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    private fun registerKeyButton() {
        for (i in 0 until binding.keysButtons.childCount) {
            val view: View = binding.keysButtons.getChildAt(i)
            view.setOnTouchListener(onTouchListener)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    var onTouchListener = OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            HidConsts.gamepadKeyDown(v.tag.toString())
        } else if (event.action == MotionEvent.ACTION_UP) {
            HidConsts.gamepadKeyUp(v.tag.toString())
        }
        false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            HidConsts.gamepadGyro(
                (event.values[0] * 128).toInt().toByte(),
                (event.values[1] * 128).toInt().toByte(),
                (event.values[2] * 128).toInt().toByte()
            )
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO("Not yet implemented")
    }
}
