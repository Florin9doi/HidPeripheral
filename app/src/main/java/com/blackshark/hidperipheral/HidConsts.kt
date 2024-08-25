package com.blackshark.hidperipheral

import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.os.Handler
import android.text.TextUtils
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

object HidConsts {
    const val TAG = "u-HidConsts"
    const val NAME = "BS-HID-Peripheral"
    const val DESCRIPTION = "fac"
    const val PROVIDER = "funny"

    @JvmField
    var HidDevice: BluetoothHidDevice? = null

    private var handler: Handler? = null
    private val inputReportQueue: Queue<HidReport> = ConcurrentLinkedQueue()
    var ModifierByte: Byte = 0x00
    var KeyByte: Byte = 0x00
    var gamepadXByte: Byte = 0x00
    var gamepadYByte: Byte = 0x00
    var gamepadZByte: Byte = 0x00
    var gamepadButtonByte: Byte = 0x00
    var joystickXByte: Byte = 0x00
    var joystickYByte: Byte = 0x00
    var joystickButtonByte: Byte = 0x00
    fun cleanKbd() {
        sendKeyReport(byteArrayOf(0, 0))
    }

    private fun addInputReport(inputReport: HidReport?) {
        if (inputReport != null) {
            inputReportQueue.offer(inputReport)
        }
    }

    var scheperoid: Long = 5
    fun reporters(context: Context) {
        handler = Handler(context.mainLooper)
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val report = inputReportQueue.poll()
                if (report != null) {
                    if (HidUtils.isConnected()) {
                        postReport(report)
                    }
                }
            }
        }, 0, scheperoid)
    }

    private fun postReport(report: HidReport) {
        HidReport.SendState = HidReport.State.Sending
        val ret = HidDevice!!.sendReport(HidUtils.mDevice, report.ReportId.toInt(), report.ReportData)
        if (!ret) {
            HidReport.SendState = HidReport.State.Failded
        } else {
            HidReport.SendState = HidReport.State.Sended
        }
    }

    fun sendMouseReport(reportData: ByteArray?) {
        val report = HidReport(HidReport.DeviceType.Mouse, 0x01.toByte(), reportData!!)
        addInputReport(report)
    }

    private val MouseReport = HidReport(HidReport.DeviceType.Mouse, 0x01.toByte(), byteArrayOf(0, 0, 0, 0))
    fun mouseMove(dx: Int, dy: Int, wheel: Int, leftButton: Boolean, rightButton: Boolean, middleButton: Boolean) {
        var dx = dx
        var dy = dy
        var wheel = wheel
        if (HidReport.SendState == HidReport.State.Sending) {
            return
        }
        if (dx > 127) dx = 127
        if (dx < -127) dx = -127
        if (dy > 127) dy = 127
        if (dy < -127) dy = -127
        if (wheel > 127) wheel = 127
        if (wheel < -127) wheel = -127
        if (leftButton) {
            MouseReport.ReportData[0] = MouseReport.ReportData[0] or 1
        } else {
            MouseReport.ReportData[0] = (MouseReport.ReportData[0] and 1.inv()).toByte()
        }
        if (rightButton) {
            MouseReport.ReportData[0] = MouseReport.ReportData[0] or 2
        } else {
            MouseReport.ReportData[0] = (MouseReport.ReportData[0] and 2.inv()).toByte()
        }
        if (middleButton) {
            MouseReport.ReportData[0] = MouseReport.ReportData[0] or 4
        } else {
            MouseReport.ReportData[0] = (MouseReport.ReportData[0] and 4.inv()).toByte()
        }
        MouseReport.ReportData[1] = dx.toByte()
        MouseReport.ReportData[2] = dy.toByte()
        MouseReport.ReportData[3] = wheel.toByte()
        addInputReport(MouseReport)
    }

    fun leftBtnDown() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] or 1
        sendMouseReport(MouseReport.ReportData)
    }

    fun leftBtnUp() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] and 1.inv()
        sendMouseReport(MouseReport.ReportData)
    }

    fun leftBtnClick() {
        leftBtnDown()
        UtilCls.DelayTask({ leftBtnUp() }, 20, true)
    }

    fun leftBtnClickAsync(delay: Int): TimerTask {
        return UtilCls.DelayTask({ leftBtnClick() }, delay, true)
    }

    fun rightBtnDown() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] or 2
        sendMouseReport(MouseReport.ReportData)
    }

    fun rightBtnUp() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] and 2.inv()
        sendMouseReport(MouseReport.ReportData)
    }

    fun midBtnDown() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] or 4
        sendMouseReport(MouseReport.ReportData)
    }

    fun midBtnUp() {
        MouseReport.ReportData[0] = MouseReport.ReportData[0] and 4.inv()
        sendMouseReport(MouseReport.ReportData)
    }

    fun modifierDown(UsageId: Byte): Byte {
        synchronized(HidConsts::class.java) { ModifierByte = ModifierByte or UsageId }
        return ModifierByte
    }

    fun modifierUp(UsageId: Byte): Byte {
        var UsageId = UsageId
        UsageId = UsageId.inv().toByte()
        synchronized(HidConsts::class.java) { ModifierByte = (ModifierByte and UsageId).toByte() }
        return ModifierByte
    }

    fun kbdKeyDown(usageStr: String) {
        var usageStr = usageStr
        if (!TextUtils.isEmpty(usageStr)) {
            if (usageStr.startsWith("M")) {
                usageStr = usageStr.replace("M", "")
                synchronized(HidConsts::class.java) {
                    val mod = modifierDown(usageStr.toInt().toByte())
                    sendKeyReport(byteArrayOf(mod, KeyByte))
                }
            } else {
                val key = usageStr.toInt().toByte()
                synchronized(HidConsts::class.java) {
                    KeyByte = key
                    sendKeyReport(byteArrayOf(ModifierByte, KeyByte))
                }
            }
        }
    }

    fun kbdKeyUp(usageStr: String) {
        var usageStr = usageStr
        if (!TextUtils.isEmpty(usageStr)) {
            if (usageStr.startsWith("M")) {
                usageStr = usageStr.replace("M", "")
                synchronized(HidConsts::class.java) {
                    val mod = modifierUp(usageStr.toInt().toByte())
                    sendKeyReport(byteArrayOf(mod, KeyByte))
                }
            } else {
                synchronized(HidConsts::class.java) {
                    KeyByte = 0
                    sendKeyReport(byteArrayOf(ModifierByte, KeyByte))
                }
            }
        }
    }

    private fun sendKeyReport(reportData: ByteArray) {
        val report = HidReport(HidReport.DeviceType.Keyboard, 0x02.toByte(), reportData)
        addInputReport(report)
    }

    fun gamepadKeyDown(usageStr: String) {
        if (!TextUtils.isEmpty(usageStr)) {
            val key = usageStr.toInt().toByte()
            synchronized(HidConsts::class.java) {
                gamepadButtonByte = gamepadButtonByte or key
                sendGamepadReport(byteArrayOf(
                    gamepadXByte,
                    gamepadYByte,
                    gamepadZByte,
                    gamepadButtonByte
                ))
            }
        }
    }

    fun gamepadKeyUp(usageStr: String) {
        if (!TextUtils.isEmpty(usageStr)) {
            val key = usageStr.toInt().toByte()
            synchronized(HidConsts::class.java) {
                gamepadButtonByte = gamepadButtonByte and key.inv()
                sendGamepadReport(byteArrayOf(
                    gamepadXByte,
                    gamepadYByte,
                    gamepadZByte,
                    gamepadButtonByte
                ))
            }
        }
    }

    fun gamepadGyro(x: Byte, y: Byte, z: Byte) {
        synchronized(HidConsts::class.java) {
            gamepadXByte = x
            gamepadYByte = y
            gamepadZByte = z
            sendGamepadReport(byteArrayOf(
                gamepadXByte,
                gamepadYByte,
                gamepadZByte,
                gamepadButtonByte
            ))
        }
    }

    private fun sendGamepadReport(reportData: ByteArray) {
        val report = HidReport(HidReport.DeviceType.Gamepad, 0x03.toByte(), reportData)
        addInputReport(report)
    }

    fun joystickKeyDown(usageStr: String) {
        if (!TextUtils.isEmpty(usageStr)) {
            val key = usageStr.toInt().toByte()
            synchronized(HidConsts::class.java) {
                joystickXByte = key
                sendJoystickReport(byteArrayOf(
                    joystickXByte,
                    joystickYByte,
                    0x00,
                    joystickButtonByte,
                    0x00,
                    0x00
                ))
            }
        }
    }

    fun joystickKeyUp(usageStr: String) {
        if (!TextUtils.isEmpty(usageStr)) {
            val key = usageStr.toInt().toByte()
            synchronized(HidConsts::class.java) {
                joystickXByte = key
                sendJoystickReport(byteArrayOf(
                    joystickXByte,
                    joystickYByte,
                    0x00,
                    joystickButtonByte,
                    0x00,
                    0x00
                ))
            }
        }
    }

    private fun sendJoystickReport(reportData: ByteArray) {
        val report = HidReport(HidReport.DeviceType.Joystick, 0x03.toByte(), reportData)
        addInputReport(report)
    }

    @JvmField
    val Descriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x02.toByte(),        // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x09.toByte(), 0x01.toByte(),        //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)
        0x85.toByte(), 0x01.toByte(),        //     Report ID (1)
        0x05.toByte(), 0x09.toByte(),        //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),        //     Usage Minimum (0x01)
        0x29.toByte(), 0x03.toByte(),        //     Usage Maximum (0x03)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x75.toByte(), 0x01.toByte(),        //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x75.toByte(), 0x05.toByte(),        //     Report Size (5)
        0x81.toByte(), 0x03.toByte(),        //     Input (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05.toByte(), 0x01.toByte(),        //     Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x09.toByte(), 0x38.toByte(),        //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data,Var,Rel,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),                       //   End Collection
        0xC0.toByte(),                       // End Collection

        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), 0x02.toByte(),        //   Report ID (2)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Kbrd/Keypad)
        0x19.toByte(), 0xE0.toByte(),        //   Usage Minimum (0xE0)
        0x29.toByte(), 0xE7.toByte(),        //   Usage Maximum (0xE7)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),        //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),        //   Logical Maximum (101)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0x00)
        0x29.toByte(), 0x65.toByte(),        //   Usage Maximum (0x65)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05.toByte(), 0x08.toByte(),        //   Usage Page (LEDs)
        0x95.toByte(), 0x05.toByte(),        //   Report Count (5)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x19.toByte(), 0x01.toByte(),        //   Usage Minimum (Num Lock)
        0x29.toByte(), 0x05.toByte(),        //   Usage Maximum (Kana)
        0x91.toByte(), 0x02.toByte(),        //   Output (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x03.toByte(),        //   Report Size (3)
        0x91.toByte(), 0x03.toByte(),        //   Output (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        0xC0.toByte(),                       // End Collection

        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x05.toByte(),        // Usage (Game Pad)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)
        0x85.toByte(), 0x03.toByte(),        //     Report ID (3)
        0x05.toByte(), 0x01.toByte(),        //     Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x09.toByte(), 0x32.toByte(),        //     Usage (Z)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data.toByte(),Var.toByte(),Abs.toByte(),No Wrap.toByte(),Linear.toByte(),Preferred State.toByte(),No Null Position)
        0x05.toByte(), 0x09.toByte(),        //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),        //     Usage Minimum (0x01)
        0x29.toByte(), 0x08.toByte(),        //     Usage Maximum (0x08)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //     Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //     Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //     Report Count (8)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data.toByte(),Var.toByte(),Abs.toByte(),No Wrap.toByte(),Linear.toByte(),Preferred State.toByte(),No Null Position)
        0xC0.toByte(),                       //   End Collection
        0xC0.toByte(),                       // End Collection

        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x04.toByte(),        // Usage (Joystick)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), 0x04.toByte(),        //   Report ID (4)
        0x09.toByte(), 0x01.toByte(),        //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
                                             //     Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x02.toByte(),        //     Report Count (2)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),                       //   End Collection
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x81.toByte(), 0x01.toByte(),        //   Input (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05.toByte(), 0x09.toByte(),        //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),        //   Usage Minimum (0x01)
        0x29.toByte(), 0x06.toByte(),        //   Usage Maximum (0x06)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x35.toByte(), 0x00.toByte(),        //   Physical Minimum (0)
        0x45.toByte(), 0x01.toByte(),        //   Physical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x06.toByte(),        //   Report Count (6)
        0x81.toByte(), 0x02.toByte(),        //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x02.toByte(),        //   Report Count (2)
        0x81.toByte(), 0x01.toByte(),        //   Input (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x95.toByte(), 0x02.toByte(),        //   Report Count (2)
        0x81.toByte(), 0x01.toByte(),        //   Input (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),                       // End Collection
    )
}
