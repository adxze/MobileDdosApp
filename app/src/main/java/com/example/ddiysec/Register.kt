package com.example.ddiysec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName = findViewById(R.id.activity_register_name_input)
        etEmail = findViewById(R.id.activity_register_email_input)
        etPassword = findViewById(R.id.activity_register_password_input)
        etConfirmPassword = findViewById(R.id.activity_register_confirm_password_input)
        btnRegister = findViewById(R.id.activity_register_register_btn)
        btnLogin = findViewById(R.id.activity_register_login_btn)

        databaseHelper = DatabaseHelper(this)

        // Set listeners for main
        btnRegister.setOnClickListener { registerUser() }
        btnLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validate input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (databaseHelper.checkUserExists(email)) {
            Toast.makeText(this, "User already exists with this email", Toast.LENGTH_SHORT).show()
            return
        }

        // Add new user
        val userId = databaseHelper.addUser(name, email, password)
        if (userId > 0) {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
        }
    }
}