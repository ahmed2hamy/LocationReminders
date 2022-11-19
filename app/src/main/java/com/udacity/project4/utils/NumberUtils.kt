package com.udacity.project4.utils

import androidx.annotation.IntRange
import java.text.DecimalFormat

fun Double.truncate(@IntRange(from = 1, to = 16) decimalPlaces: Int): Double {
    val patternBuilder = StringBuilder("#.")
    var places = decimalPlaces
    while (places > 0) {
        patternBuilder.append("#")
        places--
    }
    return DecimalFormat(patternBuilder.toString()).format(this).toDouble()
}
