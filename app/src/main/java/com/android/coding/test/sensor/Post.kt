package com.android.coding.test.sensor


class Post {


    var time: Long? = null

    var x: Float? = null
    var y: Float? = null
    var z: Float? = null

    constructor() : super() {}

    constructor(time: Long, x: Float, y: Float, z: Float) : super() {
        this.time = time
        this.x = x
        this.y = y
        this.z = z
    }

    fun getTime(): Long {
        return time!!
    }
    fun getX(): Float {
        return x!!
    }
    fun getY(): Float {
        return y!!
    }
    fun getZ(): Float {
        return z!!
    }
}