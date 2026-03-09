package com.example.chainrx

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.chainrx.utils.QRUtils

class QRCodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        val qrContent = intent.getStringExtra("qr_content") ?: "No Content"
        val title = intent.getStringExtra("qr_title") ?: "Verification QR"

        findViewById<TextView>(R.id.tvQRTitle).text = title
        val ivQR = findViewById<ImageView>(R.id.ivQRCode)

        val qrBitmap = QRUtils.generateQRCode(qrContent, 800)
        if (qrBitmap != null) {
            ivQR.setImageBitmap(qrBitmap)
        }

        findViewById<android.view.View>(R.id.btnClose).setOnClickListener { finish() }
    }
}
