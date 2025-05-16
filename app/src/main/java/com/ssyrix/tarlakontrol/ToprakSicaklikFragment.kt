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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ssyrix.tarlakontrol.databinding.FragmentToprakSicaklikBinding
import java.text.SimpleDateFormat
import java.util.*

class ToprakSicaklikFragment : Fragment() {

    private lateinit var binding: FragmentToprakSicaklikBinding
    private lateinit var textSoil: TextView
    private lateinit var chartSoil: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentToprakSicaklikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        observeSoilMoistureRealtime()
        loadSoilGraphData()

    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        val dateText = toolbar.findViewById<TextView>(R.id.plantDateText)
        val animation = toolbar.findViewById<LottieAnimationView>(R.id.plantAnimation)

        dateText.text = "Ekim Tarihi: ${getOrSetPlantingDate(requireContext())}"
        animation.setAnimation(R.raw.plant_grow)
        animation.playAnimation()
    }



    // değişken toprak sıcaklığını alıp, grafik üzerinde gösterir

    private fun loadSoilGraphData() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("veriler")
        val entries = mutableListOf<Entry>()
        var index = 0f

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()

                snapshot.children.forEach { child ->
                    val valueString = child.child("value").getValue(String::class.java) ?: return@forEach
                    val soil = valueString
                        .split(",")
                        .find { it.startsWith("Soil:") }
                        ?.split(":")
                        ?.getOrNull(1)
                        ?.toFloatOrNull()

                    soil?.let {
                        entries.add(Entry(index, it))
                        index++
                    }
                }

                val dataSet = LineDataSet(entries, "Toprak Nemi(%)")
                dataSet.lineWidth = 2f
                dataSet.circleRadius = 3f

                val lineData = LineData(dataSet)
                binding.chartSoil.data = lineData
                binding.chartSoil.description.isEnabled = false
                binding.chartSoil.invalidate()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ToprakGraph", "Firebase Error", error.toException())
            }
        })
    }


    // uygulama çalıştığında o anki toprak sıcaklık değerini verir.
    private fun observeSoilMoistureRealtime() {
        val ref = FirebaseDatabase.getInstance().getReference("veriler")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.children.lastOrNull()?.child("value")?.getValue(String::class.java)
                val soil = value
                    ?.split(",")
                    ?.find { it.trim().startsWith("Soil:") }
                    ?.split(":")
                    ?.getOrNull(1)
                    ?.toFloatOrNull()


                soil?.let { soilValue ->
                    // Log ile kontrol edelim
                    Log.d("FirebaseSoil", "Toprak Nemi Değeri: $soilValue")

                    // 1. TextView güncelle
                    binding.toprakSicaklik.text = "Toprak Nemi: %.1f %%".format(soilValue)

                    // 2. Grafik güncelle
                    binding.soilProgress.setProgress(soilValue)

                    // 3. İkonu güncelle (isteğe bağlı)
                    binding.soilImage.setImageResource(
                        if (soilValue < 40f) R.drawable.dry_soil else R.drawable.nemli_toprak
                    )
                } ?: run {
                    Log.w("FirebaseSoil", "Toprak nemi parse edilemedi. Gelen veri: $value")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Toprak nemi alınamadı", error.toException())
            }
        })
    }


//güncel tarihi veriyor.

    private fun getOrSetPlantingDate(context: Context): String {
        val prefs = context.getSharedPreferences("plant_data", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("planting_date", null)

        return savedDate ?: SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date()).also {
            prefs.edit().putString("planting_date", it).apply()
        }
    }
}