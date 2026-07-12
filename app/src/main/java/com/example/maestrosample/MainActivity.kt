package com.example.maestrosample

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱이 포그라운드에 있는 동안 화면이 꺼지지 않도록 유지
        // (Maestro 테스트 도중 화면이 꺼지면 빈 화면 계층만 보여 테스트가 실패한다).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme {
                Surface(
                    // testTagsAsResourceId: Compose의 testTag를 UIAutomator resource-id로
                    // 노출시켜 Maestro의 id: 셀렉터로 그대로 찾을 수 있게 한다.
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                ) {
                    MaestroSampleApp()
                }
            }
        }
    }
}
