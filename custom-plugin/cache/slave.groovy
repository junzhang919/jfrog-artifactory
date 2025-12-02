// https://www.jfrog.com/confluence/display/RTF/User+Plugins

import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.artifactory.repo.service.InternalRepositoryService
import org.artifactory.request.NullRequestContext
import org.artifactory.repo.RepoPathFactory

deleteMagicMd5          = "5f8c0cb"

def settings = [
		// repo path factory
		rpf: new RepoPathFactory(),
		// config
		config: (new SlaveConfigurationHolder(ctx, log)).getCurrent(),
]


// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/invalidate?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm;md5=xxxxxxx
// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/sync?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm;md5=xxxxxxx
executions {
	invalidate(users:['anonymous'], groups:['readers']) { params ->
		log.debug "invalidate $params"
		asSystem {
			(status, message) = invalidateHandle(settings, params)
		}
	}

	sync(users:['anonymous'], groups:['readers']) { params ->
		log.debug "sync $params"
		asSystem {
			(status, message) = syncHandle(settings, params)
		}
	}
}

def invalidateHandle(settings, params) {
	log.debug "param ${params}"
	String md5 = params?.get('md5')?.get(0) as String
	String path = params?.get('path')?.get(0) as String
	if (!path || !md5) {
		return [400, "invalid param"]
	}

	def rpath = settings.rpf.create(path)

	if (!settings.config.repoKeys[rpath.repoKey])
		return  [200, "${rpath.repoKey} is not necessary invalidate"]

	if (!repositories.exists(rpath)) {
		return [200, "no cached"]
	}

	if (md5 == deleteMagicMd5) {
		repositories.delete(rpath)
		return [200, "invalid file deleted"]
	}

	// is file
	if (md5 != repositories.getFileInfo(rpath).checksumsInfo.md5) {
		log.debug "md5 got ${repositories.getFileInfo(rpath).checksumsInfo.md5} want ${md5}"
		repositories.delete(rpath)
		return [200, "invalid file deleted"]
	}

	return [200, "invalidated"]
}


def syncHandle(settings, params) {
	String md5 = params?.get('md5')?.get(0) as String
	String path = params?.get('path')?.get(0) as String
	if (!path || !md5) {
		return [400, "invalid param"]
	}

	def rpath = settings.rpf.create(path)

	if (!settings.config.repoKeys[rpath.repoKey])
		return  [200, "${rpath.repoKey} is not necessary sync"]

	if (md5 == deleteMagicMd5) {
		repositories.delete(rpath)
		return [200, "synchronized"]
	}

	if (repositories.exists(rpath)) {
		if (md5 == repositories.getFileInfo(rpath).checksumsInfo.md5) {
			return [200, "synchronized"]
		}
		repositories.delete(rpath)
	}


	// download
	def repoService = ctx.beanForType(InternalRepositoryService)
	def repo = repoService.remoteRepositoryByKey(rpath.repoKey)
	log.debug "get repo from remote ${repo}"
	//def cache = repo.localCacheRepo

	log.info "Repodata file '$path' not cached, downloading"

	def ctx = new NullRequestContext(rpath)
	def res = repo.getInfo(ctx)
	repo.getResourceStreamHandle(ctx, res)?.close()


	def info = repositories.getFileInfo(rpath)
	if (md5 == info.checksumsInfo.md5) {
		return [200, "synchronized"]
	}

	return [400, "synchronization failed"]
}


class SlaveConfigurationHolder {
	File confFile
	Logger log
	SlaveConfiguration current = null
	long confFileLastChecked = 0L
	long confFileLastModified = 0L
	List<String> errors
	String dir = "${ctx.artifactoryHome.etcDir}/plugins"

	SlaveConfigurationHolder(ctx, log) {
		this.log = log
		this.confFile = new File(dir, "slave.json")
	}

	SlaveConfiguration getCurrent() {
		log.debug "Retrieving current conf $confFileLastChecked $confFileLastModified $current"
		if (current == null || needReload()) {
			log.debug "Reloading configuration from ${confFile.getAbsolutePath()}"
			if (!confFile || !confFile.exists()) {
				errors = ["The conf file ${confFile.getAbsolutePath()} does not exists!"]
			} else {
				try {
					current = new SlaveConfiguration(confFile, log)
					errors = current.findErrors()
					if (errors.isEmpty()) {
						confFileLastChecked = System.currentTimeMillis()
						confFileLastModified = confFile.lastModified()
					}
				} catch (Exception e) {
					def err = "Something not good happen during parsing: ${e.getMessage()}"
					log.error(err, e)
					errors = [err]
				}
			}
			if (errors)
				log.error("Some validation errors appeared while parsing "+
						"${confFile.absolutePath}\n${errors.join("\n")}")
		}
		current
	}

	boolean needReload() {
		// Every 120secs check
		if ((System.currentTimeMillis() - confFileLastChecked) > 1200000L)
			return !confFile.exists() || confFile.lastModified() != confFileLastModified

		return false
	}
}


class SlaveConfiguration {
	HashMap repoKeys = new HashMap()

	SlaveConfiguration(File confFile, log) {
		def reader
		try {
			reader = new FileReader(confFile)
			def config = new JsonSlurper().parse(reader)
			config['repos']['index'].each {
				log.debug "${it.key} ${it.value}"
				if (it.value['remote']) {
					repoKeys[it.value['remote']] = true
				}
				log.debug "${repoKeys}"
			}
		} finally {
			if (reader)
				reader.close()
		}
	}

	def findErrors() {
		if (!(repoKeys))
			return ["No data found or declared in build master JSON configuration"]
		return []
	}
}
