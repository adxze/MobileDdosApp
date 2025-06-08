package com.example.ddiysec

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "UserManager.db"

        private const val TABLE_USER = "users"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_USER_EMAIL = "user_email"
        private const val COLUMN_USER_PASSWORD = "user_password"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_USER_TABLE = ("CREATE TABLE " + TABLE_USER + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_NAME + " TEXT,"
                + COLUMN_USER_EMAIL + " TEXT,"
                + COLUMN_USER_PASSWORD + " TEXT" + ")")
        db.execSQL(CREATE_USER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")

        onCreate(db)
    }

    // Add a new user
    fun addUser(name: String, email: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USER_NAME, name)
        values.put(COLUMN_USER_EMAIL, email)
        values.put(COLUMN_USER_PASSWORD, password)

        val id = db.insert(TABLE_USER, null, values)
        db.close()
        return id
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COLUMN_USER_EMAIL = ? AND $COLUMN_USER_PASSWORD = ?"
        val selectionArgs = arrayOf(email, password)

        val cursor = db.query(
            TABLE_USER,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val count = cursor.count
        cursor.close()
//        db.close() kemungkinan menutup seluruh db

        return count > 0
    }

    // Check if email already exists
    fun checkUserExists(email: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COLUMN_USER_EMAIL = ?"
        val selectionArgs = arrayOf(email)

        val cursor = db.query(
            TABLE_USER,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val count = cursor.count
        cursor.close()
//        db.close()

        return count > 0
    }


    // -=-== ADMIN ONLY

    // Get user by ID
    fun getUserById(id: Int): User? {
        val db = this.readableDatabase
        var user: User? = null

        val cursor = db.query(
            TABLE_USER,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_PASSWORD))
            )
        }

        cursor.close()
        return user
    }

    // Update user
    fun updateUser(id: Int, name: String, email: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USER_NAME, name)
        values.put(COLUMN_USER_EMAIL, email)
        values.put(COLUMN_USER_PASSWORD, password)

        val rowsAffected = db.update(
            TABLE_USER,
            values,
            "$COLUMN_USER_ID = ?",
            arrayOf(id.toString())
        )

        db.close()
        return rowsAffected > 0
    }

    // Delete user
    fun deleteUser(id: Int): Boolean {
        val db = this.writableDatabase
        val rowsAffected = db.delete(
            TABLE_USER,
            "$COLUMN_USER_ID = ?",
            arrayOf(id.toString())
        )

        db.close()
        return rowsAffected > 0
    }

    fun getAllUsers(): List<User> {
        val userList = mutableListOf<User>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USER"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(COLUMN_USER_ID)
                val nameIndex = cursor.getColumnIndex(COLUMN_USER_NAME)
                val emailIndex = cursor.getColumnIndex(COLUMN_USER_EMAIL)
                val passwordIndex = cursor.getColumnIndex(COLUMN_USER_PASSWORD)

                val id = if (idIndex != -1) cursor.getInt(idIndex) else -1
                val name = if (nameIndex != -1) cursor.getString(nameIndex) else ""
                val email = if (emailIndex != -1) cursor.getString(emailIndex) else ""
                val password = if (passwordIndex != -1) cursor.getString(passwordIndex) else ""

                val user = User(id, name, email, password)
                userList.add(user)
            } while (cursor.moveToNext())
        }

        cursor.close()

        return userList
    }
}