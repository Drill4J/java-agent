package com.epam.drill.agent

import com.epam.drill.kni.*

@Kni
actual object NativeRegistry {
    actual fun loadLibrary(path: String){
        System.load(path)
    }
}
