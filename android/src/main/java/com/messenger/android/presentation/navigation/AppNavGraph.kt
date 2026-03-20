package com.messenger.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.messenger.android.presentation.screens.auth.LoginScreen
import com.messenger.android.presentation.screens.chat.ChatScreen
import com.messenger.android.presentation.screens.chats.ChatListScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoggedIn = { navController.navigate("chats") })
        }
        composable("chats") {
            ChatListScreen(onOpenChat = { navController.navigate("chat/$it") })
        }
        composable("chat/{chatId}") { backStackEntry ->
            ChatScreen(chatId = backStackEntry.arguments?.getString("chatId").orEmpty())
        }
    }
}
