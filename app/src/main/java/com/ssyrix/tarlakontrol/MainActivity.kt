package com.ssyrix.tarlakontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val CHANNEL_ID = "sensor_alerts"

    // 🔁 Tekrarlı bildirimleri engelleyen bayraklar
    private var soilNotified = false
    private var tempNotified = false
    private var humNotified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = TarlaAdapter(this)
        viewPager.adapter = adapter

        handleNotificationPermission()
        readFirebaseData()
    }

    private fun readFirebaseData() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("veriler")

        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists()) {
                    val firstChild = snapshot.children.firstOrNull()
                    val valueString = firstChild?.child("value")?.getValue(String::class.java)

                    if (valueString != null) {
                        val parts = valueString.split(",")
                        val temp = parts.find { it.startsWith("Temp:") }?.split(":")?.get(1)
                        val hum = parts.find { it.startsWith("Hum:") }?.split(":")?.get(1)
                        val soil = parts.find { it.startsWith("Soil:") }?.split(":")?.get(1)

                        soil?.toFloatOrNull()?.let { soilValue ->
                            Log.d("FIREBASE_OKUMA", "Toprak: $soilValue")
                            if (soilValue < 30f) {
                                if (!soilNotified) {
                                    showNotification("Düşük Toprak Nemi", "Toprak nemi çok düşük: %.1f %%".format(soilValue))
                                    soilNotified = true
                                }
                            } else {
                                soilNotified = false
                            }
                        }

                        temp?.toFloatOrNull()?.let { tempValue ->
                            Log.d("FIREBASE_OKUMA", "Sıcaklık: $tempValue")
                            if (tempValue < 25f) {
                                if (!tempNotified) {
                                    showNotification("Düşük Hava Sıcaklığı", "Hava sıcaklığı düşük: %.1f °C".format(tempValue))
                                    tempNotified = true
                                }
                            } else {
                                tempNotified = false
                            }
                        }

                        hum?.toFloatOrNull()?.let { humValue ->
                            Log.d("FIREBASE_OKUMA", "Nem: $humValue")
                            if (humValue < 30f) {
                                if (!humNotified) {
                                    showNotification("Düşük Hava Nemi", "Hava nemi düşük: %.1f %%".format(humValue))
                                    humNotified = true
                                }
                            } else {
                                humNotified = false
                            }
                        }
                    } else {
                        Log.e("FIREBASE_OKUMA", "value alanı null")
                    }
                } else {
                    Log.d("FIREBASE_OKUMA", "Snapshot boş.")
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("FIREBASE_OKUMA", "Veri alınamadı", error.toException())
            }
        })
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensör Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Toprak nemi, hava sıcaklığı ve nem uyarıları"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.uygulama_gorsel)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            } else {
                createNotificationChannel()
            }
        } else {
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sensör Uyarıları"
            val descriptionText = "Toprak sıcaklığı ve nem bildirimleri"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createNotificationChannel()
        }
    }

    private fun getOrSetPlantingDate(context: Context): String {
        val prefs = context.getSharedPreferences("plant_data", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("planting_date", null)

        return savedDate ?: SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date()).also {
            prefs.edit().putString("planting_date", it).apply()
        }
    }
}
