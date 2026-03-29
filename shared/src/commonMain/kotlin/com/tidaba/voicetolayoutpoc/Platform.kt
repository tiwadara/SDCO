package com.tidaba.voicetolayoutpoc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform