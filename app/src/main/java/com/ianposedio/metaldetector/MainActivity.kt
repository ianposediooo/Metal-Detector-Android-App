package com.ianposedio.metaldetector

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
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val MY_PERMISSIONS_REQUEST_CAMERA = 2

    private var hasFlash = false
    private var isLighOn = false
    private var isSensorOn = false


    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var magneticFieldStrengthTextView: TextView
    private lateinit var vibrator: Vibrator
    private lateinit var fab: FloatingActionButton
    private var vibServiceIntent: Intent? = null
    private lateinit var darkmode: ImageButton
    private lateinit var cardView: MaterialCardView
    private lateinit var button: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.darkmode = this.findViewById(R.id.darkMode);
        if (getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            this.darkmode.setImageResource(R.drawable.darkmode)
        }
        if (getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            this.darkmode.setImageResource(R.drawable.lightmode)
        }


        this.darkmode.setOnClickListener {
            if (darkmode.isPressed() && getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    /*this.darkmode.setImageResource(R.drawable.lightmode)*/
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Toast.makeText(this, "Light mode turned ON", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                /*this.darkmode.setImageResource(R.drawable.darkmode)*/
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Dark mode turned ON", Toast.LENGTH_SHORT).show()
        }


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        fab = findViewById(R.id.floatingActionButton)
        textView = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressbar)
        button = findViewById(R.id.button3)
        magneticFieldStrengthTextView = findViewById(R.id.magneticFieldStrengthTextView)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        this.button.setOnClickListener(View.OnClickListener {
            if (this.button.isPressed && magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } == true){
                button.setText("Pause Detection")
                button.setIconResource(R.drawable.pause_button)
                /*Toast.makeText(this,"Sensor is ON",Toast.LENGTH_SHORT).show()*/
                magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                return@OnClickListener
            }else{
                button.setText("Start Detection")
                button.setIconResource(R.drawable.play_icon)
                /*Toast.makeText(this,"Sensor Paused",Toast.LENGTH_SHORT).show()*/
                sensorManager.unregisterListener(this)
            }
        })


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

        val hasSystemFeature: Boolean = this.packageManager.hasSystemFeature("android.hardware.camera.flash")
        hasFlash = hasSystemFeature
        if (!hasSystemFeature) {
            val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
            builder.setTitle("Error!")
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
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        vibrator.cancel()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.drawer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_settings -> {
                // Handle menu item 1 click
                val window = window
                val checkBoxView = View.inflate(this, R.layout.checkbox, null)

                val checkBox = checkBoxView.findViewById<CheckBox>(R.id.checkbox)
                val checkBox1 = checkBoxView.findViewById<CheckBox>(R.id.checkbox1)
                val checkBox2 = checkBoxView.findViewById<CheckBox>(R.id.checkbox2)

                val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                checkBox.isChecked = sharedPreferences.getBoolean("isChecked1", false)
                checkBox1.isChecked = sharedPreferences.getBoolean("isChecked2", false)
                checkBox2.isChecked = sharedPreferences.getBoolean("isChecked3", false)

                //vibration
                vibServiceIntent = Intent(this, vib_service::class.java)
                checkBox.text = "Vibration"
                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Toast.makeText(this, "Vibration on", Toast.LENGTH_SHORT).show()
                        /*startService(vibServiceIntent)*/
                        // Save the state of the checkbox to SharedPreferences
                        editor.putBoolean("isChecked1", isChecked)
                        editor.apply()
                    } else {
                        Toast.makeText(this, "Vibration off", Toast.LENGTH_SHORT).show()
                        /*stopService(vibServiceIntent)*/
                        // Save the state of the checkbox to SharedPreferences
                        getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                        editor.putBoolean("isChecked1", false)
                        editor.apply()
                    }
                }

                //sound
                checkBox1.text = "Sound"
                checkBox1.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Toast.makeText(this, "Sound on", Toast.LENGTH_SHORT).show()

                        // Save the state of the checkbox to SharedPreferences
                        editor.putBoolean("isChecked2", isChecked)
                        editor.apply()
                    } else {
                        Toast.makeText(this, "Sound off", Toast.LENGTH_SHORT).show()

                        // Save the state of the checkbox to SharedPreferences
                        getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                        editor.putBoolean("isChecked2", false)
                        editor.apply()
                    }
                }

                //keep screen on
                checkBox2.text = "Keep Screen On"
                checkBox2.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Toast.makeText(this, "Keeping screen on", Toast.LENGTH_SHORT).show()
                        // Set the FLAG_KEEP_SCREEN_ON flag to keep the screen on
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        // Save the state of the checkbox to SharedPreferences
                        editor.putBoolean("isChecked3", isChecked)
                        editor.apply()
                    } else {
                        // Clear the FLAG_KEEP_SCREEN_ON flag to allow the screen to turn off
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        // Save the state of the checkbox to SharedPreferences
                        getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                        editor.putBoolean("isChecked3", false)
                        editor.apply()
                    }
                }

                //dialog
                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                builder.setTitle("Settings")
                    .setIcon(R.drawable.settings_icon)
                    .setView(checkBoxView)
                    .setCancelable(false)
                    .setPositiveButton("OK") { dialog, id ->
                        dialog.cancel()
                    }
                builder.show()
                return true
            }

            R.id.nav_share -> {
                var myVersionName: String? = "Not Available" // initialize String
                try {
                    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                val Sub = "Get the Latest Version of Metal Detector"+ " v." + myVersionName +" on GitHub\n\nhttps://github.com/ianposediooo/"
                intent.putExtra(Intent.EXTRA_TEXT, Sub)
                startActivity(Intent.createChooser(intent, "Share " + this.getString(R.string.app_name) + " v." + myVersionName))
                return true
            }


            R.id.nav_about -> {
                val aboutButton = View.inflate(this, R.layout.aboutbutton, null)
                val button = aboutButton.findViewById<MaterialButton>(R.id.button)
                val button2 = aboutButton.findViewById<MaterialButton>(R.id.button2)

                button.setOnClickListener {
                    Toast.makeText(this, "Donation Coming soon!", Toast.LENGTH_SHORT).show()
                }

                button2.setOnClickListener {
                    Toast.makeText(this, "Rate App Coming soon!", Toast.LENGTH_SHORT).show()
                }

                var myVersionName: String? = "Not Available" // initialize String
                try {
                    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

                val builder = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                builder.setMessage("Developed and Designed by:\nIan Baldwin Aquino Posedio" + "\n\nMade in PH \uD83C\uDDF5\uD83C\uDDED❤️")
                builder.setTitle("About:\n\n" + this.getString(R.string.app_name) + " v." + myVersionName)
                    .setIcon(R.drawable.about_icon)
                    .setView(aboutButton)
                    .setCancelable(false)
                    .setPositiveButton("View App on GitHub \uD83E\uDDD1\u200D\uD83D\uDCBB") { dialog, id ->
                        val url = "https://github.com/ianposediooo/"
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    }
                    .setNegativeButton("Close ❌"){ dialog, id ->
                        dialog.cancel()
                    }
                builder.show()
                return true
            }
            // Add more menu items as needed
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magneticFieldStrength = Math.sqrt(Math.pow(event.values[0].toDouble(), 2.0) +
                    Math.pow(event.values[1].toDouble(), 2.0) +
                    Math.pow(event.values[2].toDouble(), 2.0))

            progressBar.progress = magneticFieldStrength.toInt()
            progressBar.max = 1000

            cardView = findViewById(R.id.cardView)
            if (magneticFieldStrength > 0){
                magneticFieldStrengthTextView.text = "${magneticFieldStrength.toInt()}"
            }
            else{
                magneticFieldStrengthTextView.text = "0"
            }


            if (magneticFieldStrength > 50) {
                textView.text = "Metal Nearby! 👀"
                cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.flashyellow))
                if (magneticFieldStrength > 100){
                    cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.orange))
                    textView.text = "Metal Detected! 🙌"
                }
                if (magneticFieldStrength > 300){
                    cardView.setCardBackgroundColor(ActivityCompat.getColor(this, R.color.red))
                    textView.text = "Metal Detected! 😊🙌"
                }
            } else {
                cardView.setCardBackgroundColor(Color.GRAY)
                textView.text = "No metal detected. 🥲"
                vibrator.cancel()
            }
        }
    }

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

    /*private fun playmidlong() {
        val sound = R.raw.beepmidlong
        val mediaPlayer = android.media.MediaPlayer.create(this, sound)
        mediaPlayer.start()
    }

    private fun playmidshort() {
        val sound = R.raw.beepmidshort
        val mediaPlayer = android.media.MediaPlayer.create(this, sound)
        mediaPlayer.start()
    }

    private fun vibratenear() {
        if (vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 100, 100)
            vibrator.vibrate(pattern, 0)
        }
    }*/

    //flashlight
    fun turnOnFlash() {
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
    fun turnOffFlash() {
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


}

