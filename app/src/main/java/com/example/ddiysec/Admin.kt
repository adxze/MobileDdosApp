package com.example.ddiysec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdminActivity : AppCompatActivity(), UserAdapter.OnUserClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var tvUserCount: TextView
    private lateinit var btnLogout: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        recyclerView = findViewById(R.id.recycler_view_users)
        tvUserCount = findViewById(R.id.tv_user_count)
        btnLogout = findViewById(R.id.btn_admin_logout)

        recyclerView.layoutManager = LinearLayoutManager(this)

        databaseHelper = DatabaseHelper(this)

        loadUsers()

        btnLogout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loadUsers() {
        val userList = databaseHelper.getAllUsers()

        tvUserCount.text = "${userList.size}"

        userAdapter = UserAdapter(userList, this)
        recyclerView.adapter = userAdapter
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onUserClick(user: User) {
        val intent = Intent(this, EditUserActivity::class.java)
        intent.putExtra("USER_ID", user.id)
        startActivity(intent)
    }

    override fun onUserLongClick(user: User, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (databaseHelper.deleteUser(user.id)) {
                    Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show()

                    loadUsers()
                } else {
                    Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}