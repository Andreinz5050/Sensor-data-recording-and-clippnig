package com.android.coding.test.sensor

import kotlinx.serialization.*
import kotlinx.serialization.json.*

data class JsonDataParser(

    var time: Long? = null,

    var x: Float? = null,

    var y: Float? = null,

    var z: Float? = null

    )
{}
