package com.ianposedio.metaldetector

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val MY_PERMISSIONS_REQUEST_CAMERA = 2

    private var hasFlash = false
    private var isLighOn = false
    private var isVibrating = false
    private var isBeeping = false
    private var isMagnetometerEnabled: Boolean = false

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBar1: ProgressBar
    private lateinit var progressBar2: ProgressBar
    private lateinit var magneticFieldStrengthTextView: TextView
    private lateinit var vibrator: Vibrator
    private lateinit var fab: FloatingActionButton
    private lateinit var cardView: MaterialCardView
    private lateinit var button: MaterialButton
    private lateinit var accuracyTextView: TextView
    private lateinit var vibrateButton: MaterialButton
    private lateinit var beepButton: MaterialButton
    private lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab = findViewById(R.id.floatingActionButton)
        textView = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressbar)
        progressBar1 = findViewById(R.id.progressbar1)
        progressBar2 = findViewById(R.id.progressbar2)
        button = findViewById(R.id.button3)
        magneticFieldStrengthTextView = findViewById(R.id.magneticFieldStrengthTextView)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        accuracyTextView = findViewById(R.id.accuracyTextView)
        vibrateButton = findViewById(R.id.vibrationButton)
        beepButton = findViewById(R.id.beepButton)
        mediaPlayer = MediaPlayer.create(this, R.raw.beepfast)


        //if has sensor or none
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            //nothing
        } else {
            val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
            builder.setTitle("No Magnet Sensor!")
                .setMessage("Sorry but it looks like your device doesn't have a Magnetometer sensor. You cannot use this app.")
                .setIcon(R.drawable.warning)
                .setCancelable(false)
                .setNegativeButton("Exit"){ dialog, id ->
                    finish()
                }
            builder.show()
        }

        //keepscreenonswitch
        val coordinatorLayout = findViewById<CoordinatorLayout>(R.id.coordinator)
        val keepScreenOnSwitch = findViewById<MaterialSwitch>(R.id.keepScrenOn_Switch)
        keepScreenOnSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Snackbar.make(coordinatorLayout, "Keeping Screen ON", Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Snackbar.make(coordinatorLayout, "Keep Screen ON disabled", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }


        //vibration and beep buttons
            beepButton.setOnClickListener {
                isBeeping = !isBeeping
                if (isBeeping) {
                    beepButton.setIconResource(R.drawable.soundon_icon)
                    startBeeping()
                    Snackbar.make(coordinatorLayout, "Sound ON", Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    beepButton.setIconResource(R.drawable.mute_button)
                    stopBeeping()
                    Snackbar.make(coordinatorLayout, "Sound OFF", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            vibrateButton.setOnClickListener {
                isVibrating = !isVibrating
                if (isVibrating) {
                    vibrateButton.setIconResource(R.drawable.vibration_button)
                    startVibrating()
                    Snackbar.make(coordinatorLayout, "Vibration ON", Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    vibrateButton.setIconResource(R.drawable.vibrateoff)
                    stopVibrating()
                    Snackbar.make(coordinatorLayout, "Vibration OFF", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }


        //material toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //detection button
        button.setOnClickListener {
            if (isMagnetometerEnabled) {
                stopBeeping()
                sensorManager.unregisterListener(this)
                isMagnetometerEnabled = false
                button.text = "Start Detection"
                button.setIconResource(R.drawable.play_icon)
            } else {
                magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                isMagnetometerEnabled = true
                button.text = "Stop Detection"
                button.setIconResource(R.drawable.stop_icon)
            }
        }

        //floatingactionbutton
        this.fab.setOnClickListener(
            View.OnClickListener
            {
                if (this.isLighOn) {
                    this.turnOffFlash()
                    return@OnClickListener
                }
                this.turnOnFlash()
            })

        //if device has flash?
        val hasSystemFeature: Boolean = this.packageManager.hasSystemFeature("android.hardware.camera.flash")
        hasFlash = hasSystemFeature
        if (!hasSystemFeature) {
            val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
            builder.setTitle("Error!")
            builder.setCancelable(true)
            builder.setMessage("Sorry your device doesn't have a flashlight")
            builder.setNegativeButton(
                "OK"
            ) { dialogInterface, i ->
                dialogInterface.cancel()
            }
            builder.show()
        }
    }

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (isMagnetometerEnabled) {
            sensorManager.unregisterListener(this)
            isMagnetometerEnabled = false
            button.text = "Start Detection"
            button.setIconResource(R.drawable.play_icon)
        } else {
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            isMagnetometerEnabled = true
            button.text = "Stop Detection"
            button.setIconResource(R.drawable.stop_icon)
        }
    }


    override fun onPause() {
        super.onPause()
        stopBeeping()
        turnOffFlash()
        stopVibrating()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val accuracyText = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
            else -> "UNRELIABLE"
        }
        if (sensor!!.type == Sensor.TYPE_MAGNETIC_FIELD){
            accuracyTextView.text = "Sensor Accuracy: $accuracyText"
        }
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                val calibrationview = View.inflate(this, R.layout.calibrationview, null)
                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                builder.setTitle("Calibrate Sensor")
                    .setIcon(R.drawable.warning)
                    .setMessage("The magnetometer sensor accuracy is $accuracyText. Please calibrate the sensor like in the image shown below (4 times).")
                    .setView(calibrationview)
                    .setCancelable(false)
                    .setPositiveButton("Done") { dialog, id ->
                        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                            dialog.dismiss()
                        }
                    }
                builder.show()
            }


            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                val calibrationview = View.inflate(this, R.layout.calibrationview, null)
                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                builder.setTitle("Calibrate Sensor")
                    .setIcon(R.drawable.warning)
                    .setMessage("The magnetometer sensor accuracy is $accuracyText. Please calibrate the sensor like in the image shown below (4 times).")
                    .setView(calibrationview)
                    .setCancelable(false)
                    .setPositiveButton("Done") { dialog, id ->
                        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                            dialog.dismiss()
                        }
                    }
                builder.show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.drawer_menu, menu)
        return true
    }

    //menu items
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            //share menu item
            R.id.nav_share -> {
                var myVersionName: String? = "Not Available" // initialize String
                try {
                    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
                //share intent
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                val Sub = "Get the Latest Version of Metal Detector"+ " v." + myVersionName +" on GitHub\n\nhttps://github.com/ianposediooo/"
                intent.putExtra(Intent.EXTRA_TEXT, Sub)
                startActivity(Intent.createChooser(intent, "Share " + this.getString(R.string.app_name) + " v." + myVersionName))
                return true
            }

            //about menu item
            R.id.nav_about -> {
                val aboutButton = View.inflate(this, R.layout.aboutbutton, null)
                val button = aboutButton.findViewById<MaterialButton>(R.id.button)
                val button2 = aboutButton.findViewById<MaterialButton>(R.id.button2)
                val button3 = aboutButton.findViewById<MaterialButton>(R.id.button3)
                val button4 = aboutButton.findViewById<MaterialButton>(R.id.button4)
                val button5 = aboutButton.findViewById<MaterialButton>(R.id.button5)

                //about buttons
                button.setOnClickListener {
                    val url = "https://paypal.me/ianposedio"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                button2.setOnClickListener {
                    Toast.makeText(this, "Rating App coming soon!", Toast.LENGTH_SHORT).show()
                }
                button3.setOnClickListener {
                    val url = "https://github.com/ianposediooo/Metal-Detector-Android-App.git"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                button4.setOnClickListener {
                    val mailto = "mailto:posedioian@gmail.com" +
                            "?cc=" +
                            "&subject=" + Uri.encode("Metal Detector App Feedback") +
                            "&body=" + Uri.encode("")
                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                    emailIntent.data = Uri.parse(mailto)

                    try {
                        startActivity(emailIntent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this, "Error to open email app", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                button5.setOnClickListener {
                    val mailto = "mailto:posedioian@gmail.com" +
                            "?cc=" +
                            "&subject=" + Uri.encode("Metal Detector Bug Report") +
                            "&body=" + Uri.encode("")
                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                    emailIntent.data = Uri.parse(mailto)

                    try {
                        startActivity(emailIntent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this, "Error to open email app", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                //version name
                var myVersionName: String? = "Not Available" // initialize String
                try {
                    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

                //about dialog builder
                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                /*builder.setMessage("Developed and Designed by:\nIan Baldwin Aquino Posedio" + "\n\nMade in PH \uD83C\uDDF5\uD83C\uDDED❤️")*/
                builder.setTitle(this.getString(R.string.app_name) + " v." + myVersionName)
                    .setIcon(R.drawable.about_icon)
                    .setView(aboutButton)
                    .setCancelable(true)
                    .setNegativeButton("Close"){ dialog, id ->
                        dialog.cancel()
                    }
                builder.show()
                return true
            }
            // Add more menu items as needed
            else -> return super.onOptionsItemSelected(item)
        }
    }

    //magnetometer sensor
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magneticFieldStrength = sqrt(
                event.values[0].toDouble().pow(2.0) +
                        event.values[1].toDouble().pow(2.0) +
                        event.values[2].toDouble().pow(2.0)
            )

            progressBar.progress = magneticFieldStrength.toInt()
            progressBar.max = 200

            //magnetometer display
            cardView = findViewById(R.id.cardView)
            if (magneticFieldStrength > 0){
                magneticFieldStrengthTextView.text = "${magneticFieldStrength.toInt()}"
            }


            //magnetometer conditions
            if (magneticFieldStrength > 50 && magneticFieldStrength < 100){
                cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.orange))
                textView.text = "Metal Nearby 👀"
            }
            else {
                cardView.setCardBackgroundColor(Color.GRAY)
                textView.text = "No metal Detected 😢"
            }

            if (magneticFieldStrength > 70 && magneticFieldStrength < 400) {
                cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.red))
                textView.text = "Metal Detected 👏"
            }

            if (magneticFieldStrength > 70 && isVibrating) {
                startVibrating()
            }else{
                stopVibrating()
            }


            if (magneticFieldStrength > 70 && isBeeping) {
                mediaPlayer.isLooping = true
                startBeeping()
            }else{
                stopBeeping()
            }

            if (magneticFieldStrength > 400) {
                progressBar1.max = 100
                progressBar1.progress = 100
                cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.red))
                textView.text = "Metal Detected 👏"
            }
            else {
                progressBar1.progress = 0
            }

            if (magneticFieldStrength > 800){
                progressBar2.max = 100
                progressBar2.progress = 100
                cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.red))
                textView.text = "Metal Detected 👏"
            }
            else{
                progressBar2.progress = 0
            }

        }
    }

    //flash permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                turnOnFlash()
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                builder.setTitle("Please allow Camera permission so that you can use the flashlight.")
                builder.setIcon(R.drawable.warning)
                builder.setCancelable(false)
                builder.setPositiveButton(
                    "Allow in Settings"
                ) { dialogInterface, i ->
                    openAppSettings()
                }
                builder.show()
            }
        }
    }


    //beep
    private fun startBeeping() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun stopBeeping() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.beepfast)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVibrating()
        stopBeeping()
    }

    //vibration
    private fun startVibrating() {
        if (vibrator.hasVibrator()) {
            val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                vibrator.vibrate(100)
                TODO("VERSION.SDK_INT < O")
            }
            vibrator.vibrate(vibrationEffect)
        }
    }


    /*private fun startVibratingNear() {
        if (vibrator.hasVibrator() && isMagnetometerEnabled) {
            val pattern = longArrayOf(0, 50,50)
            vibrator.vibrate(pattern,0)
        }
    }*/

    private fun stopVibrating() {
        vibrator.cancel()
    }

    //flashlight
    private fun turnOnFlash() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            requestPermissions(arrayOf("android.permission.CAMERA"), 2)
        } else
            try {
                var cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
                if (!this.isLighOn) {
                    this.fab.setImageResource(R.drawable.flashlight)
                    cameraManager.setTorchMode(cameraManager.cameraIdList[0], true)
                    this.isLighOn = true
                    /*Toast.makeText(this, "Flashlight turned ON", Toast.LENGTH_SHORT).show()*/
                }
            } catch (e: CameraAccessException) {
                Log.e("ContentValues", e.toString())
            }
    }
    private fun turnOffFlash() {
        try {
            var cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
            if (this.isLighOn) {
                fab.setImageResource(R.drawable.flashlight_off)
                cameraManager.setTorchMode(cameraManager.cameraIdList[0], false)
                this.isLighOn = false
                /*Toast.makeText(this, "Flashlight turned OFF", Toast.LENGTH_SHORT).show()*/
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    //open permission settings
    private fun openAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }


    //back pressed exit
    override fun onBackPressed() {
        val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
        builder.setTitle("Exit the App?")
            .setIcon(R.drawable.warning)
            .setCancelable(true)
            .setPositiveButton("Yes"){ dialog, id ->
                finish()
            }
            .setNegativeButton("No"){ dialog, id ->
                dialog.cancel()
            }
        builder.show()
    }

}

