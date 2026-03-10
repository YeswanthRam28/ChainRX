package com.example.chainrx

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel

class SuccessActivity : AppCompatActivity() {
    private val viewModel: ChainRxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        val tvNextStop = findViewById<TextView>(R.id.tvNextStop)
        tvNextStop.visibility = View.GONE

        // Observe optimization result
        lifecycleScope.launchWhenStarted {
            viewModel.optimizedRouteResult.collect { result ->
                result?.let {
                    val next = it.optimized_order.getOrNull(1) ?: it.optimized_order.getOrNull(0) ?: "Return to base"
                    tvNextStop.visibility = View.VISIBLE
                    tvNextStop.text = "AI Suggested Next Step:\n$next"
                }
            }
        }

        // Trigger auto-opt if we're a transporter
        val role = com.example.chainrx.auth.AuthManager.getRole(this)
        if (role == "transport") {
            viewModel.runAutoRouteOptimization()
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            finish()
        }
    }
}
