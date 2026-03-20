package com.messenger.desktop.di

import com.messenger.desktop.data.DesktopClient
import org.koin.dsl.module

val appModule = module {
    single { DesktopClient() }
}
