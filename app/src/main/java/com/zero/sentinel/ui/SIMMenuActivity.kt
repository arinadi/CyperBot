package com.zero.sentinel.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zero.sentinel.R

class SIMMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sim_menu)

        val listView = findViewById<ListView>(R.id.listView)
        val items = arrayOf(
            "Search",
            "Favorites",
            "Transaction Services",
            "Information Services",
            "Entertainment Services",
            "USSD Service Catalog",
            "Help and Support"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (item == "Help and Support") {
                showPinDialog()
            } else {
                Toast.makeText(this, "Service not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPinDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        input.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 20, 50, 20)
        }
        
        // Add container for margin
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Service Code")
            .setMessage("Enter MMI Code to access.")
            .setView(container)
            .setPositiveButton("Send") { _, _ ->
                val pin = input.text.toString()
                validatePin(pin)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePin(pin: String) {
        val prefs = com.zero.sentinel.data.EncryptedPrefsManager(this)
        val correctPin = prefs.getAppPassword()
        
        if (pin == correctPin) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("EXTRA_AUTHENTICATED", true)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid MMI Code", Toast.LENGTH_SHORT).show()
        }
    }
}
