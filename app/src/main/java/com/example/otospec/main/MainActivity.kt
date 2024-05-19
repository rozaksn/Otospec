package com.example.otospec.main

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import com.example.otospec.FOD.FODActivity
import com.example.otospec.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.otospec.databinding.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy (LazyThreadSafetyMode.NONE){
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var database:FirebaseDatabase
    private lateinit var mMaps:GoogleMap




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // Initialize the map fragment only if savedInstanceState is null
        if (savedInstanceState == null) {
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.gMap) as? SupportMapFragment
            mapFragment?.getMapAsync(this@MainActivity)
        }
        database = FirebaseDatabase.getInstance()

        setupListener()
        sendData()
        reset()
        checkLocationServices()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMaps=googleMap
        fetchLocationUpdate()
    }

    private fun setupListener(){
        val ref = database.getReference("KONTROL/RUNNING")
        binding.btFod.setOnClickListener {
            startActivity(Intent(this,FODActivity::class.java))
        }
        binding.btStop.setOnClickListener {
            binding.radioGroup.clearCheck()
            ref.setValue(0)
        }

    }
    private fun sendData(){
        val buttonRef= database.getReference("KONTROL/RUNNING")
        val switchRef = database.getReference("KONTROL/LED")
        binding.btStart.setOnClickListener {
            // Mendapatkan ID RadioButton yang dipilih
            val selectedRadioButtonId = binding.radioGroup.checkedRadioButtonId
            // Jika ada RadioButton yang dipilih
            if (selectedRadioButtonId != -1){
                // Mendapatkan referensi RadioButton yang dipilih
                val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
                // Mendapatkan teks dari RadioButton yang dipilih
                val selectedText = selectedRadioButton.text.toString().toInt()
                buttonRef.setValue(selectedText)
                Log.d("TAG", selectedText.toString())
            }else{
                Toast.makeText(this@MainActivity, R.string.warning_start, Toast.LENGTH_SHORT).show()
            }
        }
        binding.swMode.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) 1 else 0
            switchRef.setValue(value)
        }
        switchRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Int::class.java) ?: 0
                binding.swMode.isChecked = value==1
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error",R.string.error_message.toString())
            }

        })

    }

    private fun reset(){
        val buttonRef=database.getReference("KONTROL/RESET")
        binding.btReset.setOnClickListener {

                buttonRef.setValue(1)

        }
        buttonRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Int::class.java) ?: 0
                if(value==1){
                    binding.btReset.setBackgroundColor(getColor(R.color.navy_blue))
                    binding.btReset.setTextColor(getColor(R.color.pastel_yellow))



                    //Menunda pengubahan nilai menjadi 0 selama 5 detik
                    Handler(Looper.getMainLooper()).postDelayed({
                        buttonRef.setValue(0)

                    },5000)//delay 5 detik
                }else{
                    binding.btReset.setBackgroundColor(getColor(R.color.pastel_yellow))
                    binding.btReset.setTextColor(getColor(R.color.navy_blue))
                    
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error",R.string.error_message.toString())
            }

        })
    }

    private fun fetchLocationUpdate(){
        val locationRef = database.getReference("Location")
        locationRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (locationSnapshot in snapshot.children){
                    val latitude = locationSnapshot.child("lat").getValue().toString().toDouble()
                    val longitude = locationSnapshot.child("lng").getValue().toString().toDouble()
                    if (latitude != null && longitude != null) {
                        updateLocation(latitude,longitude)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error",R.string.error_message.toString())
            }

        })
    }

    private fun updateLocation(lat:Double,lng:Double){
        val latlng = LatLng(lat,lng)
        mMaps.clear()
        mMaps.addMarker(MarkerOptions().position(latlng).title(getString(R.string.current_location)).snippet(latlng.toString()))
        mMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f))
    }

    private fun checkLocationServices() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "Location services are disabled", Toast.LENGTH_LONG).show()
            // Optionally, you can prompt the user to enable location services
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Location services are enabled", Toast.LENGTH_SHORT).show()
        }
    }
}