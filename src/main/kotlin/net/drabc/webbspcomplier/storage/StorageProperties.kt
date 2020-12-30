package net.drabc.webbspcomplier.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("storage")
class StorageProperties {
	var location = "upload-dir"
	var zipLocation = "zip-dir"
}