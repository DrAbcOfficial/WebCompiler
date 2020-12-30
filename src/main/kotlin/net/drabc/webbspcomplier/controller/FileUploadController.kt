package net.drabc.webbspcomplier.controller

import net.drabc.webbspcomplier.Utility
import net.drabc.webbspcomplier.coinfig.Config
import net.drabc.webbspcomplier.storage.FileSystemStorageService
import org.apache.catalina.util.URLEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.IOException
import org.springframework.web.bind.annotation.ResponseBody
import java.lang.Exception
import java.util.*
import kotlin.io.path.ExperimentalPathApi


@Controller
class FileUploadController @Autowired constructor(private val storageService: FileSystemStorageService) {
    @GetMapping("/")
    @Throws(IOException::class)
    fun uploadFilePage(model: Model): String {
        model.addAttribute("files", Config.waitList.map{"${it.fileName}\t|\t${it.stats}\t|\t${it.createTime}"}.toList())
        model.addAttribute("compliedNum", storageService.getStoreNum())
        return "index"
    }

    @GetMapping("/uploaded")
    fun messagePage(@RequestParam(value = "stats", defaultValue = "error") stats: String,
                    @RequestParam(value = "name", defaultValue = "")filename: String,
                    @RequestParam(value = "uid", defaultValue = "")uid: String,
                    model: Model): String {
        model.addAttribute("message",
            when(stats) {
                "dropped" -> "Unknown file: $filename, dropped into junk bin."
                "success" -> "You successfully uploaded $filename, we will compile it as soon as possible"
                "error" -> "Error stats"
                else -> "Error stats"
            })
        model.addAttribute("uid", uid)
        return "stats"
    }

    @GetMapping("/error")
    fun errorPage(model: Model): String {
        return "error"
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    fun getFile(@PathVariable filename: String): ResponseEntity<Resource> {
        val file = storageService.loadAsResource("$filename.zip")
        return ResponseEntity.ok().header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.filename + "\""
        ).body(file)
    }

    @RequestMapping("/download")
    fun handleFileDownload(@RequestParam("downloadCode") code: String,
                         redirectAttributes: RedirectAttributes): String {
        var compiler = Config.doneList.find{ it.name == code}
        if(compiler == null){
            redirectAttributes.addFlashAttribute("extractError", code)
            var szReason = ""
            compiler = Config.waitList.find{it.name == code}
            szReason = if(compiler != null)
                "Compiling not done yet, Statue: ${compiler.stats}"
            else
                "No compiling task named $code"
            redirectAttributes.addFlashAttribute("extractErrorReason", szReason)
        }
        else
            return "redirect:/files/$code"
        return "redirect:/"
    }

    @ExperimentalPathApi
    @PostMapping("/upload")
    fun handleFileUpload(@RequestParam("file") file: MultipartFile, @RequestParam("email") email: String): String {
        val fileUrlname = URLEncoder.DEFAULT.encode(file.originalFilename, Charsets.UTF_8)
        if(!Utility.isMapFile(file)){
            return "redirect:/uploaded?stats=dropped&name=$fileUrlname"
        }
        val uid = UUID.randomUUID().toString()
        storageService.store(file, uid, email)
        return "redirect:/uploaded?stats=success&name=$fileUrlname&uid=$uid"
    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun dealCommonException(e: Exception?): ResponseEntity<*> {
        return ResponseEntity.notFound().build<Any>()
    }
}