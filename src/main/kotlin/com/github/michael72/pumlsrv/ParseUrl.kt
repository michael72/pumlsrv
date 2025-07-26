package com.github.michael72.pumlsrv

class ParseUrl(path: String) {
    val imageType: String
    val content: String
    val index: Int
    
    init {
        val urlParts = path.split("/")
        imageType = urlParts[0]
        
        index = if (urlParts.size > 2) {
            urlParts[urlParts.size - 2].trim().toIntOrNull() ?: 0
        } else {
            0
        }
        
        content = urlParts[urlParts.size - 1].trim()
    }
}