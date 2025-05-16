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
import com.ssyrix.tarlakontrol.databinding.FragmentHavaNemBinding
import java.text.SimpleDateFormat
import java.util.*

class HavaNemFragment : Fragment() {

    private lateinit var binding: FragmentHavaNemBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHavaNemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view)
        observeHumidityRealtime()
        loadHumidityGraph()


    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar3)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        val dateText = toolbar.findViewById<TextView>(R.id.plantDateText)
        val animation = toolbar.findViewById<LottieAnimationView>(R.id.plantAnimation)

        dateText.text = "Ekim Tarihi: ${getOrSetPlantingDate(requireContext())}"
        animation.setAnimation(R.raw.plant_grow)
        animation.playAnimation()
    }


    //burada firebase üzerinden alınan veriler düzenli olarak textView içinde sürekli olarak değiştirilecektir.

    private fun observeHumidityRealtime() {
        val ref = FirebaseDatabase.getInstance().getReference("veriler")

        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot){
                val value = snapshot.children.lastOrNull()?.child("value")?.getValue(String::class.java)
                val hum = value
                    ?.split(",")
                    ?.find { it.startsWith("Hum:") }
                    ?.split(":")
                    ?.getOrNull(1)
                    ?.toFloatOrNull()

                hum?.let{
                    binding.havaNemm.text = "Hava Nemi: %.1f %%".format(it)

                    binding.soilProgress.setProgress(it)
                    binding.havaNem.setImageResource(
                        if(it < 48f) R.drawable.nemsiz_hava else R.drawable.nemli_hava
                    )


                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Toprak nemi alınamadı", error.toException())
            }
        })


    }



    //burada grafik çizilir.
    private fun loadHumidityGraph() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("veriler")
        val entries = mutableListOf<Entry>()
        var index = 0f

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()

                snapshot.children.forEach { child ->
                    val valueString =
                        child.child("value").getValue(String::class.java) ?: return@forEach
                    val hum = valueString
                        .split(",")
                        .find { it.startsWith("Hum:") }
                        ?.split(":")
                        ?.getOrNull(1)
                        ?.toFloatOrNull()

                    hum?.let {
                        entries.add(Entry(index, it))
                        index++
                    }
                }


                val dataSet = LineDataSet(entries, "Hava Nemi %")
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f

                val lineData = LineData(dataSet)
                binding.chartHum.data = lineData
                binding.chartHum.description.isEnabled = false
                binding.chartHum.invalidate()



            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NemGraph", "Firebase Error", error.toException())
            }
        })
    }


    private fun getOrSetPlantingDate(context: Context): String {
        val prefs = context.getSharedPreferences("plant_data", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("planting_date", null)

        return savedDate ?: SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date()).also {
            prefs.edit().putString("planting_date", it).apply()
        }
    }

}