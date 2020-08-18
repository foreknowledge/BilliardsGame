package com.ellie.billiardsgame.data

import kotlin.math.hypot

class Line {
    var start = Point(0f, 0f)
    var end = Point(0f, 0f)

    val points: FloatArray
        get() = floatArrayOf(start.x, start.y, end.x, end.y)

    val length
        get() = hypot(dx, dy)

    val dx
        get() = end.x - start.x

    val dy
        get() = end.y - start.y

    fun setPoints(startX: Float, startY: Float, endX: Float, endY: Float) {
        start.x = startX
        start.y = startY
        end.x = endX
        end.y = endY
    }
}