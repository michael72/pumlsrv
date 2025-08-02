package com.github.michael72.pumlsrv

data class ConverterResult(
    val bytes: ByteArray,
    val description: String,
    val imageType: String,
    val isError: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConverterResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (description != other.description) return false
        if (imageType != other.imageType) return false
        if (isError != other.isError) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + imageType.hashCode()
        result = 31 * result + isError.hashCode()
        return result
    }
}