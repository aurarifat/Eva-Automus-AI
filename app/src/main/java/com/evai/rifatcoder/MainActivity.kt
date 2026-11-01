package com.evai.rifatcoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.evai.rifatcoder.ui.EvaApp
import com.evai.rifatcoder.ui.theme.EvaAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EvaAiTheme {
                EvaApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvaAppPreview() {
    EvaAiTheme {
        EvaApp()
    }
}
