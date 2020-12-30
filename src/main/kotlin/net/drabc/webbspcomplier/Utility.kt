package net.drabc.webbspcomplier

import org.springframework.web.multipart.MultipartFile

object Utility {
    fun getExtension(fileName: String): String{
        return fileName.substring(fileName.indexOf('.'))
    }
    fun isMapFile( file: MultipartFile): Boolean {
        return !file.isEmpty && file.contentType == "text/plain" && getExtension(file.originalFilename!!).toLowerCase() == ".map" && file.size < 5242880
    }
}