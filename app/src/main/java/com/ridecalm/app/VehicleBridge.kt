package com.ridecalm.app

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlin.math.abs

/**
 * 표준 Android Automotive Car API로 차량 속도·기어를 읽는다.
 * 에뮬레이터의 Virtual Console에서 속도·기어를 바꾸면 앱에 그대로 반영된다.
 */
class VehicleBridge(
    private val context: Context,
    private val onSpeed: (Float) -> Unit,
    private val onGear: (String) -> Unit,
    private val onLinked: (Boolean) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> (value.value as? Float)?.let { onSpeed(abs(it) * 3.6f) }
                VehiclePropertyIds.GEAR_SELECTION -> (value.value as? Int)?.let { onGear(gearName(it)) }
            }
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {}
    }

    fun connect() {
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                if (ready) setup(c) else onLinked(false)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Car API 사용 불가", t)
            onLinked(false)
        }
    }

    private fun setup(c: Car) {
        val pm = c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return onLinked(false)
        manager = pm
        var ok = false
        for (id in listOf(VehiclePropertyIds.PERF_VEHICLE_SPEED, VehiclePropertyIds.GEAR_SELECTION)) {
            try {
                ok = pm.registerCallback(callback, id, CarPropertyManager.SENSOR_RATE_ONCHANGE) || ok
            } catch (t: Throwable) {
                Log.w(TAG, "속성 구독 실패: $id", t)
            }
        }
        onLinked(ok)
    }

    fun disconnect() {
        try {
            manager?.unregisterCallback(callback)
            car?.disconnect()
        } catch (_: Throwable) {
        }
    }

    private fun gearName(g: Int) = when (g) {
        GEAR_PARK -> "P"
        GEAR_REVERSE -> "R"
        GEAR_NEUTRAL -> "N"
        else -> "D"
    }

    companion object {
        private const val TAG = "RideCalm"
        private const val GEAR_NEUTRAL = 0x0001
        private const val GEAR_REVERSE = 0x0002
        private const val GEAR_PARK = 0x0004
    }
}
