package com.blackshark.hidperipheral

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import com.blackshark.hidperipheral.databinding.ActivityJoystickBinding

class JoystickActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoystickBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoystickBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerKeyButton()
        HidConsts.cleanKbd()
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
            HidConsts.joystickKeyDown(v.tag.toString())
        } else if (event.action == MotionEvent.ACTION_UP) {
            HidConsts.joystickKeyUp(v.tag.toString())
        }
        false
    }
}
