package com.osamaalek.kiosklauncher.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences

class UnlockDialog(
    context: Context,
    private val onUnlockSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var pinInput: EditText
    private lateinit var btnUnlock: Button
    private lateinit var btnCancel: Button
    private val kioskPrefs = KioskPreferences(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent dialog from being dismissed
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        setContentView(R.layout.dialog_unlock)

        pinInput = findViewById(R.id.pinInput)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnCancel = findViewById(R.id.btnCancel)

        pinInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        btnUnlock.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            if (enteredPin == kioskPrefs.pin) {
                kioskPrefs.isLocked = false
                Toast.makeText(context, "Kiosk Unlocked", Toast.LENGTH_SHORT).show()
                onUnlockSuccess()
                dismiss()
            } else {
                Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        // Show keyboard automatically
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}
