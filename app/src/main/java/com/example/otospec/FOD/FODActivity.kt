package com.example.otospec.FOD

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.otospec.R
import com.example.otospec.data.Lokasi
import com.example.otospec.databinding.ActivityFodactivityBinding
import com.example.otospec.main.MainActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FODActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE){
        ActivityFodactivityBinding.inflate(layoutInflater)
    }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val locationRef: DatabaseReference by lazy { database.getReference("Obstacle") }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        buttonAction()
        fetchLocationData()
    }

    private fun buttonAction(){
        binding.btBack.setOnClickListener {
           startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
        binding.btExit.setOnClickListener {
            deleteLocationData()
            finishAffinity()
        }
    }

    private fun fetchLocationData(){
        locationRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ambil data koordinat dari dataSnapshot
                val lokasi1 = snapshot.child("Obstacle1").getValue(Lokasi::class.java)
                val lokasi2 = snapshot.child("Obstacle2").getValue(Lokasi::class.java)
                val lokasi3 = snapshot.child("Obstacle3").getValue(Lokasi::class.java)
                val lokasi4 = snapshot.child("Obstacle4").getValue(Lokasi::class.java)

                // Buat tautan Google Maps
                lokasi1?.let { updateTextView(binding.tvLink1, it) }
                lokasi2?.let { updateTextView(binding.tvLink2, it) }
                lokasi3?.let { updateTextView(binding.tvLink3, it) }
                lokasi4?.let { updateTextView(binding.tvLink4, it) }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error",R.string.error_message.toString())
            }

        })
    }

    private fun updateTextView(textView: TextView, lokasi: Lokasi) {
        val mapLink = "https://www.google.com/maps?q=${lokasi.lat},${lokasi.lng}"
        textView.text = mapLink
        textView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapLink))
            startActivity(intent)
        }
    }

    private fun deleteLocationData(){
        val updates = mapOf(
            "Obstacle1/lat" to null,
            "Obstacle1/lng" to null,
            "Obstacle2/lat" to null,
            "Obstacle2/lng" to null,
            "Obstacle3/lat" to null,
            "Obstacle3/lng" to null,
            "Obstacle4/lat" to null,
            "Obstacle4/lng" to null
        )
        locationRef.updateChildren(updates).addOnCompleteListener{task->
            if (task.isSuccessful){
                Toast.makeText(this@FODActivity, R.string.delete_data, Toast.LENGTH_SHORT).show()
                Log.d("Data",R.string.delete_data.toString())
            }
        }
    }
}
