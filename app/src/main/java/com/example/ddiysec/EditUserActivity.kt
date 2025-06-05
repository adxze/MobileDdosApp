package com.example.ddiysec

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditUserActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        // Initialize views
        etName = findViewById(R.id.et_edit_name)
        etEmail = findViewById(R.id.et_edit_email)
        etPassword = findViewById(R.id.et_edit_password)
        btnSave = findViewById(R.id.btn_save_user)
        btnCancel = findViewById(R.id.btn_cancel_edit)

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Get user ID from intent
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user data
        loadUserData()

        // Set click listeners
        btnSave.setOnClickListener { saveUser() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun loadUserData() {
        // Get user from database
        val user = databaseHelper.getUserById(userId)
        if (user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPassword.setText(user.password)
        } else {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Update user in database
        val success = databaseHelper.updateUser(userId, name, email, password)
        if (success) {
            Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show()
        }
    }
}