package xyz.guotianqi.qtplayer.ext

import java.security.MessageDigest

fun String.md5() = encrypt(this, "MD5")

private fun encrypt(str: String, type: String): String {
    val bytes = MessageDigest.getInstance(type).digest(str.toByteArray())

    return bytes2Hex(bytes)
}

internal fun bytes2Hex(bts: ByteArray): String {
    var des = ""
    var tmp: String
    for (i in bts.indices) {
        tmp = Integer.toHexString(bts[i].toInt() and 0xFF)
        if (tmp.length == 1) {
            des += "0"
        }
        des += tmp
    }
    return des
}

/**
 * 去掉文件名的后缀
 */
fun String.removeFileExt(): String {
    val lastIndex = lastIndexOf(".")
    return if (lastIndex != -1) substring(0, lastIndex) else this
}