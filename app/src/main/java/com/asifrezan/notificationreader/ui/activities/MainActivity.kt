package com.asifrezan.notificationreader.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.asifrezan.notificationreader.databinding.ActivityMainBinding
import com.asifrezan.notificationreader.utils.NotificationAccessHelper
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var currentValue = "0"
    private var storedValue: Double? = null
    private var pendingOperator: String? = null
    private var shouldResetDisplay = false
    private var expressionValue = ""
    private var notificationPromptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
            return
        }
        PreferenceUtils.saveString(this, PreferenceUtils.USER_ID_KEY, auth.currentUser?.uid.orEmpty())

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCalculator()


        if (!notificationPromptShown && !NotificationAccessHelper.isEnabled(this)) {
            notificationPromptShown = true
            NotificationAccessHelper.showPrompt(this)
        }
    }

    private fun setupCalculator() {
        binding.displayText.text = currentValue

        val numberButtons = listOf(
            binding.button0,
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4,
            binding.button5,
            binding.button6,
            binding.button7,
            binding.button8,
            binding.button9
        )

        numberButtons.forEach { button ->
            button.setOnClickListener { appendNumber(button.text.toString()) }
        }

        binding.buttonDecimal.setOnClickListener { appendDecimal() }
        binding.buttonClear.setOnClickListener { clearCalculator() }
        binding.buttonSign.setOnClickListener { toggleSign() }
        binding.buttonPercent.setOnClickListener { applyPercent() }
        binding.buttonDivide.setOnClickListener { chooseOperator("/") }
        binding.buttonMultiply.setOnClickListener { chooseOperator("*") }
        binding.buttonMinus.setOnClickListener { chooseOperator("-") }
        binding.buttonPlus.setOnClickListener { chooseOperator("+") }
        binding.buttonEquals.setOnClickListener { calculateResult() }
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            PreferenceUtils.remove(this, PreferenceUtils.USER_ID_KEY)
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun appendNumber(number: String) {
        currentValue = if (shouldResetDisplay || currentValue == "0") number else currentValue + number
        shouldResetDisplay = false
        updateDisplay()
    }

    private fun appendDecimal() {
        if (shouldResetDisplay) {
            currentValue = "0"
            shouldResetDisplay = false
        }
        if (!currentValue.contains(".")) {
            currentValue += "."
            updateDisplay()
        }
    }

    private fun chooseOperator(operator: String) {
        val value = currentValue.toDoubleOrNull() ?: return
        if (storedValue != null && pendingOperator != null && !shouldResetDisplay) {
            calculateResult()
        }
        storedValue = currentValue.toDoubleOrNull() ?: value
        pendingOperator = operator
        expressionValue = "${formatDisplayValue(storedValue ?: value)} ${operatorSymbol(operator)}"
        binding.expressionText.text = expressionValue
        shouldResetDisplay = true
    }

    private fun calculateResult() {
        val first = storedValue ?: return
        val second = currentValue.toDoubleOrNull() ?: return
        val operator = pendingOperator ?: return

        val result = when (operator) {
            "/" -> if (second == 0.0) null else first / second
            "*" -> first * second
            "-" -> first - second
            "+" -> first + second
            else -> null
        }

        if (result == null) {
            currentValue = "Error"
            expressionValue = ""
        } else {
            expressionValue = "${formatDisplayValue(first)} ${operatorSymbol(operator)} ${formatDisplayValue(second)} ="
            currentValue = formatResult(result)
            storedValue = result
        }
        pendingOperator = null
        shouldResetDisplay = true
        updateDisplay()
    }

    private fun clearCalculator() {
        currentValue = "0"
        storedValue = null
        pendingOperator = null
        expressionValue = ""
        shouldResetDisplay = false
        updateDisplay()
    }

    private fun toggleSign() {
        currentValue = when {
            currentValue == "0" || currentValue == "Error" -> currentValue
            currentValue.startsWith("-") -> currentValue.removePrefix("-")
            else -> "-$currentValue"
        }
        updateDisplay()
    }

    private fun applyPercent() {
        val value = currentValue.toDoubleOrNull() ?: return
        currentValue = formatResult(value / 100)
        updateDisplay()
    }

    private fun updateDisplay() {
        binding.displayText.text = currentValue
        binding.expressionText.text = expressionValue
    }

    private fun formatResult(value: Double): String {
        val formatted = if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString().trimEnd('0').trimEnd('.')
        }
        return if (formatted.length > 12) "%.8g".format(value) else formatted
    }

    private fun formatDisplayValue(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private fun operatorSymbol(operator: String): String {
        return when (operator) {
            "*" -> "x"
            else -> operator
        }
    }

}
