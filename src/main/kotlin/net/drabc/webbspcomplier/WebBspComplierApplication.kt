package net.drabc.webbspcomplier

import org.springframework.boot.runApplication
import net.drabc.webbspcomplier.storage.FileSystemStorageService
import net.drabc.webbspcomplier.storage.StorageProperties
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import kotlin.io.path.ExperimentalPathApi

object Main {
	@ExperimentalPathApi
	@JvmStatic
	fun main(args: Array<String>) {
		runApplication<UploadingFilesApplication>(*args)
	}
}

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
class UploadingFilesApplication {
	@Bean
	fun init(storageService: FileSystemStorageService): CommandLineRunner {
		return CommandLineRunner { args: Array<String>? ->
			storageService.deleteAll()
			storageService.init()
		}
	}
}
