package com.example.otospec.main

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
        val distanceRef = database.getReference("KONTROL/RangeDistance")
        binding.btFod.setOnClickListener {
            startActivity(Intent(this,FODActivity::class.java))
        }
        binding.btStop.setOnClickListener {
            binding.radioGroup.clearCheck()
            ref.setValue(0)
            distanceRef.setValue(0)
        }

    }
    private fun sendData(){
        val buttonRef= database.getReference("KONTROL/RUNNING")
        val switchRef = database.getReference("KONTROL/LED")
        val radioRef = database.getReference("KONTROL/RangeDistance")
        binding.btStart.setOnClickListener {
            buttonRef.setValue(1)
        }

        binding.radioGroup.setOnCheckedChangeListener { _, selectedRadioButtonId ->
            // Jika ada RadioButton yang dipilih
            if (selectedRadioButtonId != -1) {
                when(selectedRadioButtonId) {

                    binding.rb1.id->{
                        // RadioButton 1 dipilih, kirim nilai 1 ke Firebase
                        radioRef.setValue(1)
                        Log.d("TAG", getString(R.string.rb1_msg))
                    }

                    binding.rb2.id->{
                        // RadioButton 1 dipilih, kirim nilai 3 ke Firebase
                        radioRef.setValue(2)
                        Log.d("TAG", getString(R.string.rb2_msg))
                    }

                    binding.rb3.id->{
                        // RadioButton 1 dipilih, kirim nilai 4 ke Firebase
                        radioRef.setValue(3)
                        Log.d("TAG", getString(R.string.rb3_msg))
                    }

                    binding.rb4.id->{
                        // RadioButton 1 dipilih, kirim nilai 8 ke Firebase
                        radioRef.setValue(4)
                        Log.d("TAG", getString(R.string.rb4_msg))
                    }


                }
            }
        }

        binding.swMode.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) 1 else 0
            switchRef.setValue(value)
            if (isChecked){
                //Switch On
                binding.swMode.thumbTintList = ColorStateList.valueOf(Color.WHITE)
                binding.swMode.trackTintList = ColorStateList.valueOf(getColor(R.color.pastel_orange))
            }else{
                //Switch Off
                binding.swMode.thumbTintList = ColorStateList.valueOf(Color.WHITE)
                binding.swMode.trackTintList = ColorStateList.valueOf(getColor(R.color.blue))
            }
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
                        Log.d("Location","Lat:$latitude, Lng:$longitude")
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
        val bitmap = BitmapFactory.decodeResource(resources,R.drawable.logo_png)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,100,100,true)
        if (bitmap != null) {
            mMaps.clear()
            mMaps.addMarker(
                MarkerOptions().position(latlng).title(getString(R.string.current_location))
                    .snippet(latlng.toString())
                    .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
            )

            mMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f))
        }else {
            Log.e("Error", "Bitmap is null. Please check the resource ID.")
        }
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