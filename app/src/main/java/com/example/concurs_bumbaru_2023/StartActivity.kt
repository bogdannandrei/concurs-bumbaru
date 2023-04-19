package com.example.concurs_bumbaru_2023

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val realtimeDetectionBtn = findViewById<Button>(R.id.buttonLeft)
        val storageBtn = findViewById<Button>(R.id.buttonRight)

        realtimeDetectionBtn.setOnClickListener{
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

        storageBtn.setOnClickListener{
            val intent = Intent(this,Video::class.java)
            startActivity(intent)
        }
    }
}