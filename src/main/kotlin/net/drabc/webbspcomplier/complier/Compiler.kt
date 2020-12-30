package net.drabc.webbspcomplier.complier

import net.drabc.webbspcomplier.coinfig.Config
import nl.komponents.kovenant.task
import nl.komponents.kovenant.thenApply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import com.sun.mail.util.MailSSLSocketFactory
import org.springframework.util.FileSystemUtils


class Compiler constructor(uid: String, _fileName: String?, _email: String){
    private val rootLocation: Path = Paths.get("${Config.filesLoacation}/$uid")
    val fileName: String = _fileName?.substring(0, _fileName.lastIndexOf('.')) ?: "file"
    private val email:String = _email
    var stats = COMPILER_STATS.COMPILER_UNINIT
    private var done = false
    val name = uid
    val createTime = Date()

    private fun getFullPath(): String{
        return "${rootLocation.toAbsolutePath()}/$fileName"
    }

    private fun startProcess(szCommand: List<String>){
        try {
            val process = ProcessBuilder()
                .command(szCommand)
                //.redirectErrorStream(true)
                //.inheritIO()
            val p = process.start()
            while (true) {
                if (!p.isAlive) {
                    p.destroyForcibly()
                    break
                }
            }
        }catch (e:Exception){
            println(e)
        }
    }

    @ExperimentalPathApi
    private fun startExe(s: COMPILER_STATS){
        when(s){
            COMPILER_STATS.COMPILER_CSG -> {
                task {
                    startProcess(listOf(Config.csgPath,"-wadautodetect","-chart","-estimate", getFullPath()))
                } thenApply  {
                    stats = COMPILER_STATS.COMPILER_BSP
                    startExe(stats)
                }
            }
            COMPILER_STATS.COMPILER_BSP -> {
                task {
                    startProcess(listOf(Config.bspPath,"-chart","-estimate", getFullPath()))
                } thenApply {
                    stats = COMPILER_STATS.COMPILER_VIS
                    startExe(stats)
                }
            }
            COMPILER_STATS.COMPILER_VIS -> {
                task {
                    startProcess(listOf(Config.visPath,"-full","-chart","-estimate", getFullPath()))
                } thenApply {
                    stats = COMPILER_STATS.COMPILER_RAD
                    startExe(stats)
                }
            }
            COMPILER_STATS.COMPILER_RAD -> {
                task {
                    startProcess(listOf(Config.radPath,"-extra","-customshadowwithbounce","-chart","-estimate", getFullPath()))
                } thenApply {
                    var errFile: File? = null
                    rootLocation.toFile().walk()
                        .maxDepth(1)
                        .filter { it.isFile }
                        .filter { it.extension in listOf("err") }
                        .forEach { errFile = it }
                    stats = if(errFile == null || errFile!!.totalSpace <= 0L)
                        COMPILER_STATS.COMPILER_SUCCESS
                    else
                        COMPILER_STATS.COMPILER_FAIL
                    //println("编译结果为:$stats")
                    //打包文件
                    zipByFolder(rootLocation.toAbsolutePath().toString())
                    FileSystemUtils.deleteRecursively(rootLocation.toAbsolutePath().toFile())
                    sendMail()
                    //递归下一个
                    if(Config.waitList.count() > 1)
                        task {
                            Config.waitList[1].start()
                        }
                    Config.doneList.add(Config.waitList[0])
                    Config.waitList.removeAt(0)
                }
            }
            else -> return
        }
    }

    private fun createFile(filePath: String): File {
        val file = File(filePath)
        val parentFile = file.parentFile!!
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    private fun zip(files: List<File>, zipFilePath: String) {
        if (files.isEmpty()) return

        val zipFile = createFile(zipFilePath)
        val buffer = ByteArray(1024)
        var zipOutputStream: ZipOutputStream? = null
        var inputStream: FileInputStream? = null
        try {
            zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
            files.forEach {file ->
                if (file.exists() && file.extension != "zip") {
                    zipOutputStream.putNextEntry(ZipEntry(file.name))
                    inputStream = FileInputStream(file)
                    var len: Int
                    while (inputStream!!.read(buffer).also { len = it } > 0) {
                        zipOutputStream.write(buffer, 0, len)
                    }
                    zipOutputStream.closeEntry()
                }
            }
        } finally {
            inputStream?.close()
            zipOutputStream?.close()
        }
    }

    private fun zipByFolder(fileDir: String) {
        //println("zip in $fileDir")
        val folder = File(fileDir)
        if (folder.exists() && folder.isDirectory) {
            val filesList: List<File> = folder.listFiles()!!.toList()
            zip(filesList, "${Config.zipLocation}/$name.zip")
            //File(fileDir).listFiles()!!.forEach {
                //println("delete in ${it.name} | ${it.extension}")
            //    if(it.extension != "zip")
            //        it.delete()
            //}
        }
    }

    @ExperimentalPathApi
    private fun sendMail(){
        if(Regex("^\\s*\\w+(?:\\.?[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$").matches(email)){
            val props = Properties()
            val sf = MailSSLSocketFactory()
                sf.isTrustAllHosts = true
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.ssl.socketFactory"] = sf
            props["mail.smtp.host"] = Config.SMTP_host
            props["mail.smtp.port"] = Config.SMTP_port.toString()
            props["mail.smtp.auth"] = Config.SMTP_auth.toString()
            props["mail.transport.protocol"] = "SMTP"
            props["mail.smtp.starttls.enable"] = "true"
            // SSL
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory";
            props["mail.smtp.socketFactory.fallback"] = "false"
            props["mail.smtp.socketFactory.port"] = Config.SMTP_port.toString()

            props["mail.smtp.connectiontimeout"] = "15000";//SMTP服务器连接超时时间
            props["mail.smtp.timeout"] = "60000";//发送邮件超时时间
            val mailSession:Session = Session.getInstance(props)
            //mailSession.debug = true
            val mMessage = MimeMessage(mailSession)

            val formAddress = InternetAddress(Config.SMTP_from)
            formAddress.personal = Config.SMTP_fromnick
            mMessage.setFrom(formAddress)
            mMessage.sentDate = Date()
            mMessage.setRecipients(Message.RecipientType.TO,email)
            mMessage.subject = "Your map compiler result"
            val contentBodyPart = MimeBodyPart()
            var szContent = "<div>此邮件为系统自动发送, 请勿直接回复该邮件</div><img src='cid:a'><img src='cid:a'>\n"
            szContent += "<div>" + if(stats == COMPILER_STATS.COMPILER_FAIL)
                "<h1>Your map compiled failed, Reason:</h1><div>\n\t\t${File("${rootLocation.toAbsolutePath()}/$fileName.err").readText()}</div>\n"
            else
                "<h1>Your map compiled successfully</h1>\n"
            szContent += "</div>Please grab your file via this code: $name"
            contentBodyPart.setContent(szContent,"text/html;charset=UTF-8")
            val addM = MimeMultipart()
            addM.addBodyPart(contentBodyPart)
            mMessage.setContent(addM)
            mMessage.saveChanges()
            val transport = mailSession.getTransport("smtp")
            transport.connect(Config.SMTP_host, Config.SMTP_port, Config.STMP_user, Config.STMP_pass)
            transport.sendMessage(mMessage, mMessage.allRecipients)
            transport.close()
        }
    }

    @ExperimentalPathApi
    fun start(){
        if(stats != COMPILER_STATS.COMPILER_READY){
            return
        }
        //预处理
        val mapFileList:MutableList<String> = mutableListOf()
        val filePath = "${rootLocation.toAbsolutePath()}/$fileName.map"
        println(filePath)
        File(filePath).useLines{
                lines -> lines.forEach{
                    if(it.startsWith("\"wad\"")){
                        var emptyString = "\"wad\" \""
                        val tempString = it.substring(7)
                        tempString.split(';').forEach{ wad ->
                            val p = Paths.get(wad.trim('"'))
                            emptyString += "${Config.wadPath}/${p.name};"
                        }
                        emptyString += "\""
                        mapFileList.add(emptyString)
                    }else{
                        mapFileList.add(it)
                    }
                }
        }
        val file = File(filePath)
        file.writeText("")
        mapFileList.forEach{
            file.appendText("$it\n")
        }
        stats = COMPILER_STATS.COMPILER_CSG
        task {
            startExe(stats)
        } fail {
            stats = COMPILER_STATS.COMPILER_FAIL
        }
        done = true
    }

    init{
        //println("new Compiler for ${rootLocation.toAbsolutePath()}")
        stats = if(fileName.isBlank()){
            println("no map file")
            COMPILER_STATS.COMPILER_FAIL
        } else{
            //println("compiler ready")
            COMPILER_STATS.COMPILER_READY
        }
    }
}