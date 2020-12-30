package net.drabc.webbspcomplier.storage

import net.drabc.webbspcomplier.Utility
import net.drabc.webbspcomplier.complier.Compiler
import net.drabc.webbspcomplier.coinfig.Config
import nl.komponents.kovenant.task
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import kotlin.io.path.ExperimentalPathApi

@Service
class FileSystemStorageService @Autowired constructor(properties: StorageProperties) {
	private val rootLocation: Path
	private val zipLocation: Path

	fun getStoreNum(): String{
		return Config.doneList.count().toString();
	}

	@ExperimentalPathApi
	fun store(file: MultipartFile, uuid: String, email: String) {
		try {
			val filePath = Paths.get("${rootLocation}/$uuid")
			Config.waitList.add(Compiler(uuid, file.originalFilename, email))
			if(Config.waitList.count() == 1){
				task {
					Config.waitList[0].start()
				}
			}
			Files.createDirectory(filePath)
			Files.copy(file.inputStream, filePath.resolve(Objects.requireNonNull("${file.originalFilename}")))
		} catch (e: IOException) {
			throw StorageException("Failed to store file " + file.originalFilename, e)
		}
	}

	fun load(filename: String?): Path {
		return zipLocation.resolve(filename)
	}

	fun loadAsResource(filename: String): Resource {
		return try {
			val file = load(filename)
			val resource: Resource = UrlResource(file.toUri())
			if (resource.exists() || resource.isReadable) {
				resource
			} else {
				throw StorageFileNotFoundException("Could not read file: $filename")
			}
		} catch (e: MalformedURLException) {
			throw StorageFileNotFoundException("Could not read file: $filename", e)
		}
	}

	fun deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile())
		FileSystemUtils.deleteRecursively(zipLocation.toFile())
	}

	fun init() {
		try {
			Files.createDirectory(rootLocation)
			Files.createDirectory(zipLocation)
		} catch (e: IOException) {
			throw StorageException("Could not initialize storage", e)
		}
	}

	init {
		rootLocation = Paths.get(properties.location)
		zipLocation = Paths.get(properties.zipLocation)
		Config.filesLoacation = properties.location
		Config.zipLocation = zipLocation.toAbsolutePath().toString()
	}
}