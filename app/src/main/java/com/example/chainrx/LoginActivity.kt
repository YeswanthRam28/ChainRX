package com.example.chainrx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel

class LoginActivity : AppCompatActivity() {
    private val viewModel: ChainRxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPassword)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPass.text.toString()
            viewModel.login(email, pass)
        }

        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        lifecycleScope.launchWhenStarted {
            viewModel.loginResult.collect {
                if (it == "SUCCESS") {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect {
                if (it.isNotEmpty()) {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
