package com.example.botadmin

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var checkedCount = 0
    private var totalCount = 1_000_000
    private var elapsedSeconds = 0L
    private var startTimeMs = 0L

    private lateinit var statusValueText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var checkedText: TextView
    private lateinit var speedText: TextView
    private lateinit var elapsedText: TextView
    private lateinit var foundText: TextView
    private lateinit var validText: TextView
    private lateinit var deadText: TextView
    private lateinit var currentText: TextView

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000
            if (checkedCount < totalCount) {
                checkedCount += 1
            }

            val digitsCount = totalCount.toString().length.coerceAtLeast(1)
            val currentCode = checkedCount.toString().padStart(digitsCount, '0')
            val elapsedMinutes = (System.currentTimeMillis() - startTimeMs) / 60000.0
            val speedPerMin = if (elapsedMinutes > 0.01) {
                (checkedCount / elapsedMinutes).toInt()
            } else {
                checkedCount * 60
            }

            statusValueText.text = "Running"
            progressBar.max = totalCount.coerceAtLeast(1)
            progressBar.progress = checkedCount.coerceAtMost(totalCount)
            checkedText.text = "$checkedCount / $totalCount"
            speedText.text = "$speedPerMin/min"
            elapsedText.text = formatElapsed(elapsedSeconds)
            currentText.text = currentCode

            if (checkedCount >= totalCount) {
                stopScanUi("Completed")
                return
            }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageBg = Color.parseColor("#F3F4F6")
        val cardBg = Color.WHITE
        val inputBg = Color.parseColor("#ECEEF2")
        val btnGrayBg = Color.parseColor("#E5E7EB")
        val bluePrimary = Color.parseColor("#0A66FF")
        val textDark = Color.parseColor("#1F2937")
        val textMuted = Color.parseColor("#6B7280")
        val greenActive = Color.parseColor("#059669")
        val redDead = Color.parseColor("#DC2626")

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(pageBg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
        }
        scrollView.addView(root)

        // Header Row: ● License active | Voucher Code Scan
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 28)
        }

        val licenseText = TextView(this).apply {
            text = "● License active"
            setTextColor(greenActive)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val screenTitle = TextView(this).apply {
            text = "Voucher Code Scan"
            setTextColor(textDark)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        headerRow.addView(licenseText)
        headerRow.addView(screenTitle)
        root.addView(headerRow)

        // Setup Card
        val setupCard = createCard(cardBg, 24f).apply {
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 28
            }
        }

        val setupLabel = TextView(this).apply {
            text = "Setup"
            setTextColor(textDark)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 20)
        }
        setupCard.addView(setupLabel)

        val portalInput = EditText(this).apply {
            hint = "Portal URL or gateway IP"
            setHintTextColor(textMuted)
            setTextColor(textDark)
            textSize = 15f
            setPadding(28, 28, 28, 28)
            background = createBoxDrawable(inputBg, 14f)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        setupCard.addView(portalInput)

        // Digits (0-9) Row + 6 Input
        val digitsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val digitsLabelBox = TextView(this).apply {
            text = "Digits (0-9)"
            setTextColor(textDark)
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 26, 28, 26)
            background = createBoxDrawable(inputBg, 14f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12
            }
        }

        val digitsValueInput = EditText(this).apply {
            setText("6")
            setTextColor(textDark)
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 26, 28, 26)
            background = createBoxDrawable(inputBg, 14f)
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
        }

        digitsRow.addView(digitsLabelBox)
        digitsRow.addView(digitsValueInput)
        setupCard.addView(digitsRow)

        // Auto-detect + Start scan Row
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val autoDetectBtn = Button(this).apply {
            text = "Auto-detect"
            setTextColor(textDark)
            textSize = 14f
            isAllCaps = false
            setPadding(20, 24, 20, 24)
            background = createBoxDrawable(btnGrayBg, 14f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12
            }
        }

        val startScanBtn = Button(this).apply {
            text = "Start scan"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setPadding(24, 28, 24, 28)
            background = createBoxDrawable(bluePrimary, 14f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).apply {
                marginStart = 12
            }
        }

        actionRow.addView(autoDetectBtn)
        actionRow.addView(startScanBtn)
        setupCard.addView(actionRow)

        // Stop Button
        val stopBtn = Button(this).apply {
            text = "Stop"
            setTextColor(textDark)
            textSize = 15f
            isAllCaps = false
            setPadding(24, 26, 24, 26)
            background = createBoxDrawable(btnGrayBg, 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        setupCard.addView(stopBtn)
        root.addView(setupCard)

        // Status Card
        val statusCard = createCard(cardBg, 24f).apply {
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val statusTitle = TextView(this).apply {
            text = "Status"
            setTextColor(textDark)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        statusCard.addView(statusTitle)

        statusValueText = TextView(this).apply {
            text = "Idle"
            setTextColor(textMuted)
            textSize = 15f
            setPadding(0, 8, 0, 24)
        }
        statusCard.addView(statusValueText)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            ).apply {
                bottomMargin = 28
            }
        }
        statusCard.addView(progressBar)

        // Top 3 Metrics: Checked | Speed | Elapsed
        val topMetricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 28
            }
        }

        val checkedPair = createMetricColumn("Checked", "0 / 0", textMuted, textDark)
        val speedPair = createMetricColumn("Speed", "0/min", textMuted, textDark)
        val elapsedPair = createMetricColumn("Elapsed", "00:00:00", textMuted, textDark)

        checkedText = checkedPair.second
        speedText = speedPair.second
        elapsedText = elapsedPair.second

        topMetricsRow.addView(checkedPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topMetricsRow.addView(speedPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topMetricsRow.addView(elapsedPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statusCard.addView(topMetricsRow)

        // Bottom 4 Metrics: Found | Valid | Dead | Current
        val bottomMetricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val foundPair = createMetricColumn("Found", "0", greenActive, greenActive)
        val validPair = createMetricColumn("Valid", "0", textMuted, textDark)
        val deadPair = createMetricColumn("Dead", "0", redDead, redDead)
        val currentPair = createMetricColumn("Current", "—", textMuted, textDark)

        foundText = foundPair.second
        validText = validPair.second
        deadText = deadPair.second
        currentText = currentPair.second

        bottomMetricsRow.addView(foundPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bottomMetricsRow.addView(validPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bottomMetricsRow.addView(deadPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bottomMetricsRow.addView(currentPair.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statusCard.addView(bottomMetricsRow)

        root.addView(statusCard)
        setContentView(scrollView)

        autoDetectBtn.setOnClickListener {
            if (portalInput.text.toString().isBlank()) {
                portalInput.setText("http://192.168.1.1")
            }
            statusValueText.text = "Gateway detected"
        }

        startScanBtn.setOnClickListener {
            val digits = digitsValueInput.text.toString().toIntOrNull()?.coerceIn(1, 8) ?: 6
            totalCount = 10.0.pow(digits).toInt()
            checkedCount = 0
            elapsedSeconds = 0L
            startTimeMs = System.currentTimeMillis()
            isRunning = true

            statusValueText.text = "Scanning"
            checkedText.text = "0 / $totalCount"
            handler.removeCallbacks(tickerRunnable)
            handler.post(tickerRunnable)
        }

        stopBtn.setOnClickListener {
            stopScanUi("Stopped")
        }
    }

    private fun stopScanUi(stateLabel: String) {
        isRunning = false
        handler.removeCallbacks(tickerRunnable)
        statusValueText.text = stateLabel
    }

    private fun formatElapsed(totalSec: Long): String {
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun createMetricColumn(
        label: String,
        initialValue: String,
        labelColor: Int,
        valueColor: Int
    ): Pair<LinearLayout, TextView> {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val lbl = TextView(this).apply {
            text = label
            setTextColor(labelColor)
            textSize = 14f
        }
        val valView = TextView(this).apply {
            text = initialValue
            setTextColor(valueColor)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 6, 0, 0)
        }
        col.addView(lbl)
        col.addView(valView)
        return Pair(col, valView)
    }

    private fun createCard(bgColor: Int, radiusPx: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createBoxDrawable(bgColor, radiusPx)
            elevation = 4f
        }
    }

    private fun createBoxDrawable(color: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radiusPx
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(tickerRunnable)
    }
}
