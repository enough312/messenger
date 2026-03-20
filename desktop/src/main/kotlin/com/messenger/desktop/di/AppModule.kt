package com.messenger.desktop.di

import com.messenger.desktop.data.DesktopClient
import com.messenger.desktop.state.DesktopAppState
import org.koin.dsl.module

val appModule = module {
    single { DesktopClient() }
    single { DesktopAppState(get()) }
}
