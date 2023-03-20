package com.ianposedio.metaldetector

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.Nullable

class vib_service : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var vibrator: Vibrator


    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magneticFieldStrength = Math.sqrt(Math.pow(event.values[0].toDouble(), 2.0) +
                    Math.pow(event.values[1].toDouble(), 2.0) +
                    Math.pow(event.values[2].toDouble(), 2.0))

            if (magneticFieldStrength > 60) {
                if (vibrator.hasVibrator()) {
                    val pattern = longArrayOf(0, 100, 100)
                    vibrator.vibrate(pattern, 0)
                }

                if (magneticFieldStrength > 120){
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(500)
                    }
                }
            } else {
                vibrator.cancel()
            }
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return START_NOT_STICKY
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onDestroy() {
        vibrator.cancel()
        sensorManager.unregisterListener(this)
        Toast.makeText(this, "Destroy", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}