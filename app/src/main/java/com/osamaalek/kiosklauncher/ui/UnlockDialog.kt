package com.osamaalek.kiosklauncher.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.util.KioskPreferences

class UnlockDialog(
    context: Context,
    private val onUnlockSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var pinInput: EditText
    private val kioskPrefs = KioskPreferences(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent dialog from being dismissed
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        val view = layoutInflater.inflate(R.layout.dialog_unlock, null)
        setContentView(view)

        pinInput = view.findViewById(R.id.pinInput)
        val btnUnlock: Button = view.findViewById(R.id.btnUnlock)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)

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