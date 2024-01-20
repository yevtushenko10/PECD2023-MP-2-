package com.example.speechrecognitionapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.speechrecognitionapp.databinding.ActivityMainBinding
import com.example.speechrecognitionapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), RecordingCallback {

    private var audioRecordingService: AudioRecordingService? = null
    private var isServiceBound: Boolean = false
    private lateinit var binding: FragmentHomeBinding

    private var results = ArrayList<Result>()
    private lateinit var adapter: ResultAdapter

    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater)
        var view = binding.root

        val listView = binding.listView
        adapter = ResultAdapter(results, activity?.applicationContext)
        listView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRecord.setOnClickListener {
            if(isServiceBound) {
                // Stop the service, if running
                binding.btnRecord.text = "Record"
                stopService()
            } else {
                // Start the service, if not running
                binding.btnRecord.text = "Stop"
                startService()
            }
        }

    }
    override fun onDataUpdated(data: ArrayList<Result>) {
        Log.d(TAG, "Updated:" + data.size)
        activity?.runOnUiThread {
            adapter.clear()
            adapter.addAll(data)
            adapter.notifyDataSetChanged()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as AudioRecordingService.RunServiceBinder
            audioRecordingService = binder.service
            audioRecordingService?.setCallback(this@HomeFragment)
            isServiceBound = true
            audioRecordingService?.background()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioRecordingService = null
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isServiceBound) {
            activity?.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onStop() {
        super.onStop()

        if (isServiceBound) {
            if (audioRecordingService?.isRecording!!) {
                Log.d(TAG, "Foregrounding service")
                audioRecordingService?.foreground()
            }
        } else {
            stopService()
        }
    }

    private fun startService() {
        val serviceIntent = Intent(activity, AudioRecordingService::class.java)

        try {
            var energyThreshold = sharedPreferences?.getString("energy", "0.1")
            //Log.d(TAG, "energyThreshold: $energyThreshold")
            var probabilityThreshold = sharedPreferences?.getString("probability", "0.002")
            //Log.d(TAG, "probabilityThreshold: $probabilityThreshold")
            var windowSize = sharedPreferences?.getString("window_size", "8000")
            //Log.d(TAG, "windowSize: $windowSize")
            var topK = sharedPreferences?.getString("top_k", "1")

            serviceIntent.putExtras(Bundle().apply {
                putDouble("energyThreshold", energyThreshold?.toDouble()!!)
                putFloat("probabilityThreshold", probabilityThreshold?.toFloat()!!)
                putInt("windowSize", windowSize?.toInt()!!)
                putInt("topK", topK?.toInt()!!)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        activity?.startService(serviceIntent)
        bindService()
    }

    private fun stopService() {
        unbindService()
        val serviceIntent = Intent(activity, AudioRecordingService::class.java)
        activity?.stopService(serviceIntent)
    }

    private fun bindService() {
        val bindIntent = Intent(activity, AudioRecordingService::class.java)
        activity?.bindService(bindIntent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (isServiceBound) {
            activity?.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }
}