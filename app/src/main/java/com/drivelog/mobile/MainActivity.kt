package com.drivelog.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.drivelog.mobile.feature.MobileModel
import com.drivelog.mobile.navigation.MobileNavHost
import com.drivelog.mobile.ui.theme.DRIVELOGMobileTheme

class MainActivity : ComponentActivity() {
    private lateinit var model: MobileModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[MobileModel::class.java]
        model.openIntent(intent)
        setContent { DRIVELOGMobileTheme { MobileNavHost(model) } }
    }

    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) model.sync()
    }
}
