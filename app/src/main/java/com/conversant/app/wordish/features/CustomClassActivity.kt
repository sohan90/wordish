package com.conversant.app.wordish.features

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.conversant.app.wordish.R

class CustomClassActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_layout)
    }
}