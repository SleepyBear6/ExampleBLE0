package com.exampleble.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.exampleble.R
import java.text.ParseException

class PersonalData : AppCompatActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_personal_data)
        try {
            initComponent()
            //loadService()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }



    @Throws(ParseException::class)
    private fun initComponent(){
        val currentFocus = currentFocus
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val editTextName = findViewById<EditText>(R.id.editTextName)
        val editTextBirthday = findViewById<EditText>(R.id.editTextBirthday)
        val editTextID = findViewById<EditText>(R.id.editTextID)
        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)

        var infoSharedPref: SharedPreferences = getSharedPreferences(
            this.getString(R.string.preference_info_personal), MODE_PRIVATE
        )
        var infoEditor: SharedPreferences.Editor = infoSharedPref.edit()

        infoEditor.apply()

        val btnStoreData = findViewById<Button>(R.id.btnStoreData)

        btnStoreData.setOnClickListener {
            infoEditor.putString(
                "Personal_NAME", editTextName.text.toString()
            )

            infoEditor.putString(
                "Personal_Birthday", editTextBirthday.text.toString()
            )
            finish()
        }
    }
}