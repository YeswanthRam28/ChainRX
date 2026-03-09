package com.example.chainrx

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.databinding.ActivityRegisterBinding
import com.example.chainrx.network.ChainRxViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: ChainRxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Role Spinner
        val roles = arrayOf("hospital", "transport", "admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRole.adapter = adapter

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegName.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val pass = binding.etRegPassword.text.toString().trim()
            val wallet = binding.etRegWallet.text.toString().trim()
            val role = binding.spRole.selectedItem.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || wallet.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, pass, role, wallet)
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.loginResult.collect { result ->
                if (result == "SUCCESS") {
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect { msg ->
                if (msg.isNotEmpty()) {
                    Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
