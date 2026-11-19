package com.example.botadmin

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("bot_admin_prefs", Context.MODE_PRIVATE) }
    private lateinit var contentView: TextView
    private lateinit var baseUrlInput: EditText
    private lateinit var apiKeyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultUrl = prefs.getString("base_url", "https://api-l3m2.onrender.com") ?: "https://api-l3m2.onrender.com"
        val defaultKey = prefs.getString("api_key", "") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Bot Admin Control"
            textSize = 22f
            setPadding(0, 0, 0, 20)
        }
        root.addView(title)

        // Top Navigation Tabs: Home, Users, Logs, Settings
        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnHome = Button(this).apply { text = "Home" }
        val btnUsers = Button(this).apply { text = "Users" }
        val btnLogs = Button(this).apply { text = "Logs" }
        val btnSettings = Button(this).apply { text = "Settings" }

        navRow.addView(btnHome)
        navRow.addView(btnUsers)
        navRow.addView(btnLogs)
        navRow.addView(btnSettings)
        root.addView(navRow)

        baseUrlInput = EditText(this).apply {
            hint = "Base URL (https://api-l3m2.onrender.com)"
            setText(defaultUrl)
        }
        apiKeyInput = EditText(this).apply {
            hint = "ADMIN_API_KEY"
            setText(defaultKey)
        }

        val btnSaveConfig = Button(this).apply {
            text = "Save URL & Key"
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        contentView = TextView(this).apply {
            text = "Home / Users / Logs / Settings ကို နှိပ်ပြီး စစ်ဆေးပါ။"
            textSize = 15f
            setPadding(0, 24, 0, 24)
        }
        scrollView.addView(contentView)

        root.addView(baseUrlInput)
        root.addView(apiKeyInput)
        root.addView(btnSaveConfig)
        root.addView(scrollView)

        setContentView(root)

        btnSaveConfig.setOnClickListener {
            saveConfig()
            contentView.text = "Saved Base URL & ADMIN_API_KEY!"
        }

        btnHome.setOnClickListener {
            saveConfig()
            fetchEndpoint("/admin/status", "Home Status")
        }

        btnUsers.setOnClickListener {
            saveConfig()
            fetchEndpoint("/admin/users", "Users List")
        }

        btnLogs.setOnClickListener {
            saveConfig()
            fetchEndpoint("/admin/logs", "System Logs")
        }

        btnSettings.setOnClickListener {
            saveConfig()
            fetchEndpoint("/admin/settings", "Settings")
        }
    }

    private fun saveConfig() {
        val cleanUrl = baseUrlInput.text.toString().trim().removeSuffix("/")
        val cleanKey = apiKeyInput.text.toString().trim()
        prefs.edit()
            .putString("base_url", cleanUrl)
            .putString("api_key", cleanKey)
            .apply()
    }

    private fun fetchEndpoint(path: String, label: String) {
        val baseUrl = prefs.getString("base_url", "")?.trim()?.removeSuffix("/") ?: ""
        val apiKey = prefs.getString("api_key", "")?.trim() ?: ""

        if (baseUrl.isEmpty()) {
            contentView.text = "Base URL ထည့်ပါ။"
            return
        }

        contentView.text = "Loading $label..."

        thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl$path")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("X-API-Key", apiKey)
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                val formatted = try {
                    JSONObject(body).toString(2)
                } catch (_: Exception) {
                    body
                }

                runOnUiThread {
                    contentView.text = "[$label] HTTP $code\n\n$formatted"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    contentView.text = "[$label] Error:\n${e.message}"
                }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
