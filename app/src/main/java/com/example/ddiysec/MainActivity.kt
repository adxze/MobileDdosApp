package com.example.ddiysec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat





class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.show()
        setContentView(R.layout.activity_main)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        etEmail = findViewById(R.id.activity_main_mail_input)
        etPassword = findViewById(R.id.activity_main_Pass_input)
        btnLogin = findViewById(R.id.activity_main_login_btn)
        btnRegister = findViewById(R.id.activity_main_register_btn)

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Set listeners
        btnLogin.setOnClickListener { loginUser() }
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }


        // DATA ADMIN MASIH HARD CODED
        // Check for admin login (You can replace these with your desired admin credentials)
        if (email == "admin@app.com" && password == "admin123") {
            Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminActivity::class.java))
            return
        }

        // Check if user exists
        if (databaseHelper.checkUser(email, password)) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            // Navigate to home screen
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }
}