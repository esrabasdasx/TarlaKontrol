package com.ssyrix.tarlakontrol

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ssyrix.tarlakontrol.databinding.FragmentHavaSicaklikBinding
import java.text.SimpleDateFormat
import java.util.*

class HavaSicaklikFragment : Fragment() {

    private lateinit var binding: FragmentHavaSicaklikBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHavaSicaklikBinding.inflate(inflater, container, false)
        return binding.root
    }

    //burada metotlar tanımlanır.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        observeAirTemperatureRealtime()
        loadTemperatureGraph()
    }


//burada toolbar kısmına düzen yapılır.
    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar2)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        val dateText = toolbar.findViewById<TextView>(R.id.plantDateText)
        val animation = toolbar.findViewById<LottieAnimationView>(R.id.plantAnimation)

        dateText.text = "Ekim Tarihi: ${getOrSetPlantingDate(requireContext())}"
        animation.setAnimation(R.raw.plant_grow)
        animation.playAnimation()
    }

    //burada firebase üzerinden alınan veriler textView içerisine aktif olarak sürekli aktarılır.
    private fun observeAirTemperatureRealtime() {
        val ref = FirebaseDatabase.getInstance().getReference("veriler")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.children.lastOrNull()?.child("value")?.getValue(String::class.java)
                val temp = value
                    ?.split(",")
                    ?.find { it.startsWith("Temp:") }
                    ?.split(":")
                    ?.getOrNull(1)
                    ?.toFloatOrNull()

                temp?.let {
                    binding.havaSicaklik.text = "Hava Sıcaklığı: %.1f °C".format(it)

                    // Örneğin sıcaklığa göre görsel değiştirilebilir:
                    binding.soilProgress.setProgress(it)
                    binding.weather.setImageResource(
                        if (it < 25f) R.drawable.soguk_hava else R.drawable.sicak_hava
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Hava sıcaklığı alınamadı", error.toException())
            }
        })
    }



    //burada grafik çizilir.
    private fun loadTemperatureGraph() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("veriler")
        val entries = mutableListOf<Entry>()
        var index = 0f


        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()

                snapshot.children.forEach { child ->
                    val valueString =
                        child.child("value").getValue(String::class.java) ?: return@forEach
                    val temp = valueString
                        .split(",")
                        .find { it.startsWith("Hum:") }
                        ?.split(":")
                        ?.getOrNull(1)
                        ?.toFloatOrNull()

                    temp?.let {
                        entries.add(Entry(index, it))
                        index++
                    }
                }

                val dataSet = LineDataSet(entries, "Hava Sıcaklığı C")
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f

                val lineData = LineData(dataSet)
                binding.chartTemp.data = lineData
                binding.chartTemp.description.isEnabled = false
                binding.chartTemp.invalidate()



            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HavaSicaklikGraph", "Firebase Error", error.toException())
            }
        })
    }

//burada tarih alınır.
    private fun getOrSetPlantingDate(context: Context): String {
        val prefs = context.getSharedPreferences("plant_data", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("planting_date", null)

        return savedDate ?: SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date()).also {
            prefs.edit().putString("planting_date", it).apply()
        }
    }
}