package com.example.maestrosample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MaestroSampleApp() {
    val navController = rememberNavController()

    // 카운트를 화면 밖(NavHost 위)으로 호이스팅한다.
    // remember만 쓰면 기록 화면으로 이동하는 순간 카운터 화면이 백스택에서
    // 파기되면서 값이 사라지므로 rememberSaveable을 사용한다.
    var count by rememberSaveable { mutableIntStateOf(0) }

    // 기록 목록. 스크롤 테스트가 가능하도록 예시 기록 15개를 미리 채워 둔다.
    val records = remember {
        mutableStateListOf<String>().apply {
            addAll((1..15).map { "예시 기록 $it" })
        }
    }

    NavHost(navController = navController, startDestination = "counter") {
        composable("counter") {
            CounterScreen(
                count = count,
                onCountChange = { count = it },
                onNavigateToHistory = { navController.navigate("history") },
            )
        }
        composable("history") {
            HistoryScreen(
                currentCount = count,
                records = records,
                onSave = { name -> records.add("$name - $count") },
            )
        }
    }
}
