// https://www.jfrog.com/confluence/display/RTF/User+Plugins


import groovy.json.JsonSlurper
import groovy.json.JsonException
import groovy.json.JsonBuilder
import org.slf4j.Logger
import org.artifactory.repo.RepoPathFactory
import org.artifactory.exception.CancelException
import org.artifactory.util.HttpUtils
import org.artifactory.schedule.CachedThreadPoolTaskExecutor
import org.apache.http.client.methods.*
import org.apache.http.impl.client.HttpClients
import java.util.concurrent.CountDownLatch
import org.artifactory.repo.RepoPath

// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/invalidate?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm
// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/sync?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm
// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/syncQuery?params=uid=1234


// const
deleteMagicMd5		= "5f8c0cb"
armoryCacheAddr		= "http://localhost:8042"
createAction		= "upload"
createPropertyAction	= "upload"
deletePropertyAction	= "upload"
deleteAction		= "overwrite"
overwriteAction		= "overwrite"
aclInvalidate		= 0
aclAllow		= 1
aclDeny			= 2
aclSkip			= 3
pkgNameTrim 		= ~/-\d.*$/
tagAdmin                = ['deploy_ep']


def settings = [
	// a thread pool, for spawning threaded tasks
	threadPool: ctx.beanForType(CachedThreadPoolTaskExecutor.class),
	// a global hash map, mapping paths to run uids
	// "repo-name/path/to/dir" -> uid
	// also used as the mutex controlling access to both pathMap and uidMap
	pathMap: new HashMap(),
	// a global hash map, mapping run uids to status and path values
	// "uid" -> [status: "current run status", path: "repo-name/path/to/dir"]
	uidMap: new HashMap(),
	// repo path factory
	rpf: new RepoPathFactory(),
	// config
	config:(new MasterConfigurationHolder(ctx, log)).getCurrent(),
]

realms {
	fwRealm(autoCreateUsers: true) {
		authenticate { username, credentials ->
			return authenticateHandle(username, credentials)
		}
		userExists { username ->
			return true
		}
	}
}

upload {
	beforeUploadRequest { request, repoPath ->
		log.debug "entering upload beforeUploadRequest ${repoPath.id}" 
		beforeUploadRequestHandle(settings, request, repoPath)
	}
}

// storage is called after upload
storage {
	beforeCreate { item ->
		log.debug "entering storage.beforeCreate ${security.getCurrentUsername()} ${item.getRepoPath().id}" 
		storageBeforeFilter(settings, createAction, item.getRepoPath())
	}

	beforeDelete { item ->
		def userName = security.getCurrentUsername()
		def rpath = item.getRepoPath()

		log.debug "entering storage.beforeDelete ${userName} ${rpath.id}" 
		if (isEmptyDir(rpath)) {
			log.info "delete ${userName}@${rpath.id} allow empty dir"
			return
		}
		storageBeforeFilter(settings, deleteAction, item.getRepoPath())
	}

	beforeMove { item, targetRepoPath, properties ->
		log.debug "entering storage.beforeMove ${security.getCurrentUsername()} ${item.getRepoPath().id} ${targetRepoPath.id}" 
		storageBeforeFilter(settings, deleteAction, item.getRepoPath())
		storageBeforeFilter(settings, createAction, targetRepoPath)
	}

	beforeCopy { item, targetRepoPath, properties ->
		log.debug "entering storage.beforeCopy ${security.getCurrentUsername()} ${item.getRepoPath().id} ${targetRepoPath.id}" 
		storageBeforeFilter(settings, createAction, targetRepoPath)
	}

	beforePropertyCreate { item, name, values ->
		log.debug "entering storage.beforePropertyCreate ${security.getCurrentUsername()} ${item.getRepoPath().id} ${name}=${values}" 
		if (isTagAdmin()) return
		propertyFilter(settings, createPropertyAction, item.getRepoPath())
	}

	beforePropertyDelete{ item, name ->
		log.debug "entering storage.beforePropertyDelete ${security.getCurrentUsername()} ${item.getRepoPath().id} ${name}" 
		if (isTagAdmin()) return
		propertyFilter(settings, deletePropertyAction, item.getRepoPath())
	}
}


executions {
	hosts(httpMethod:'GET', users:['anonymous'], groups:['readers']) { params ->
		def json = new JsonBuilder()

		json {
			invalidate_hosts settings.config.invalidateHosts
			sync_hosts settings.config.syncHosts
		}
		status = 200
		message = json.toString()
	}

	repos(httpMethod:'GET', users:['anonymous'], groups:['readers']) { params ->
		def json = new JsonBuilder()
		json {
			repo_keys settings.config.repoKeys
			repo_index settings.config.repoIndex
		}
		status = 200
		message = json.toString()
	}

	invalidate(groups:['readers']) { params ->
		log.debug "invalidate ${params}"
		(status, message) = invalidateHandle(settings, params)
		log.debug "invalidate resp ${status} ${message}"
	}

	sync(groups:['readers']) { params ->
		log.debug "sync ${params}"
		(status, message) = syncHandle(settings, params)
		log.debug "sync resp ${status} ${message}"
	}

	syncQuery(groups:['readers']) { params ->
		log.debug "syncQuery ${params}"
		(status, message) = syncQueryHandle(settings, params)
		log.debug "syncQuery resp ${status} ${message}"
	}

	dryrun(groups:['readers']) { params ->
		log.debug "invalidate ${params}"
		(status, message) = dryrunHandle(settings, params)
		log.debug "resp ${status} ${message}"
	}
}


// return acl, dir, pkg
def getResourceKey(repo, rpath, beforeUpload = false) {
	def dir = rpath.getParent()
	def fileName = rpath.getName()

	// must call by before upload reqeust
	if (rpath.isFolder()) {
		log.debug "getResourceKey ${rpath.id} is not file return acl invalidate"

		dir = rpath
		switch (repo['type']) {
		case "docker":
			dir = dir.getParent().getParent()
			return [aclInvalidate, dir, ""]
		default:
			log.debug "${repo['type']}  repo type"
			return [aclInvalidate, dir, ""]
		}
	}


	// isFile
	log.debug "getResourceKey ${repo['type']} ${rpath.id} is file dir ${dir} filename ${fileName}"

	switch (repo['type']) {
	case "debian":
		def pkg = fileName.replaceFirst(pkgNameTrim, "")
		log.debug "getResourceKey return [aclInvalidate, ${dir}, ${pkg}]"
		return [aclInvalidate, dir, pkg]
	case "docker":
		if(rpath.id.contains("repository.catalog") || rpath.id.contains("tags.json")){
			log.debug "getResourceKey skip ${rpath.id} return [aclAllow, ${dir}, '']"
			return [aclAllow, dir, ""]
		}
		dir = dir.getParent().getParent()
		log.debug "getResourceKey return [aclInvalidate, ${dir}, '']"
		return [aclInvalidate, dir, ""]
	case "gems":
		if (beforeUpload) {
			log.debug "getResourceKey return [aclAllow, ${dir}, '']"
			return [aclAllow, dir, ""]
		}

		def pkg = fileName.replaceFirst(pkgNameTrim, "")
		def dir_get_path = dir.getPath()
		log.debug "getResourceKey check ${dir_get_path} pkg: ${pkg}"

		if (dir.getPath() != "gems") {
			// gems -> gems -> quick/Marshal.4.8/(as system)
			log.debug "getResourceKey return [aclSkip, ${dir}, '']"
			return [aclSkip, dir, ""]
		}
		// gems/xxx-version.gem
		log.debug "getResourceKey return [aclInvalidate, null, ${pkg}]"
		return [aclInvalidate, null, pkg]

	case "go":
		dir = dir.getParent().getParent()
		log.debug "getResourceKey return [aclInvalidate, ${dir}, '']"
		return [aclInvalidate, dir, ""]
	case "generic":
	case "helm":
		log.debug "getResourceKey return [aclInvalidate, ${dir}, '']"
		return [aclInvalidate, dir, ""]
	case ["maven", "sbt"]:
		if (fileName.startsWith("maven-metadata.xml")) {
			if (dir.getPath().endsWith("-SNAPSHOT")) {
				dir = dir.getParent().getParent()
			}else{
				dir = dir.getParent()
			}
			log.debug "getResourceKey return [aclInvalidate, ${dir}, '']"
			return [aclInvalidate, dir, ""]
		}

		dir = dir.getParent().getParent()
		log.debug "getResourceKey return [aclInvalidate, ${dir}, '']"
		return [aclInvalidate, dir, ""]
	case "npm":
		// demo/-/demo-1.0.0.tgz
		dir = dir.getParent()
		def pkg = fileName.replaceFirst(pkgNameTrim, "")
		log.debug "npm dir ${dir.getPath()} pkg ${pkg}"

		if (dir.getPath() != pkg) {
			log.debug "getResourceKey return [aclDeny, null, ${pkg}]"
			return [aclDeny, null, pkg]
		}

		log.debug "getResourceKey return [aclInvalidate, null, ${pkg}]"
		return [aclInvalidate, null, pkg]

		// TODO: deny path set
		// for (;!dir.isRoot() && !dir.getParent().isRoot();)
		// 	dir = dir.getParent()
		// pkg = dir.getPath()
		// dir = null
		// break
	case "pypi":
		dir = dir.getParent()
		def pkg = fileName.replaceFirst(pkgNameTrim, "")
		log.debug "pypi dir ${dir.getPath()} pkg ${pkg}"

		if (dir.getPath() != pkg) {
			log.debug "getResourceKey return [aclDeny, null, ${pkg}]"
			return [aclDeny, null, pkg]
		}
		log.debug "getResourceKey return [aclInvalidate, null, ${pkg}]"
		return [aclInvalidate, null, pkg]

		// TODO: deny path set
		//def pkgDir = dir.getParent()
		//dir = pkgDir.getParent().isRoot() ? null : pkgDir.getParent()
		//pkg = pkgDir.getPath()
		//break
	case ["yum", "rpm"]:
		def pkg = fileName.replaceFirst(pkgNameTrim, "")
		log.debug "getResourceKey return [aclInvalidate, ${dir}, ${pkg}]"
		return [aclInvalidate, dir, pkg]
	case "buildinfo":
		def pkg = dir.getPath()
		log.debug "getResourceKey return [aclInvalidate, null, ${pkg}]"
		return [aclInvalidate, null, pkg]
	default:
		log.debug "${repo['type']} unknown repo type"
		log.debug "getResourceKey return [aclDeny, ${dir}, '']"
		return [aclDeny, dir, ""]
	}
}

def actionAccess(user, action, resources) {
	log.debug "entering actionAccess ${user} ${action} ${resources}"

	def url = "${armoryCacheAddr}/api/v1/resources/user/${action}?user=${user}"
	def urlResource = ""
	resources.each {
		if (it)
			urlResource += "&resources=${URLEncoder.encode("${it}", "UTF-8")}"
	}

	if (urlResource == "") {
		throw new CancelException("path is invalid", 400)
	}

	url += urlResource

	log.debug "GET ${url}"

	def req = new HttpGet(url)
	def resp = makeRequest(req)

	if (resp[0] == 200) {
		log.debug "leaving actionAccess"
		return
	}

	throw new CancelException(resp[1], resp[0])
}

def getUsername(user = null) {
	if (!user)
		user = security.getCurrentUsername()

	def n = user.indexOf(":")
	if (n > 0) {
		log.debug "trim user name ${user}"
		user = user.substring(n+1)
	}
						  
	n = user.indexOf("@")
	if (n > 0) {
		log.debug "trim user name ${user}"
		user = user.substring(0,n)
	}

	return user
}


def authenticateHandle(username, credentials) {
	log.debug "entering fwRealm ${username} ${credentials}"

	if (security.isAdmin() || security.isAnonymous())
		return false

	def req = new HttpGet("${armoryCacheAddr}/api/v1/auth/info")
	def code = credentials.toLowerCase()

	if (code.startsWith("bearer-")) {
		req.addHeader("Authorization", "Bearer ${credentials.substring(7)}")
	} else if (code.startsWith("api-key-")) {
		req.addHeader("X-API-Key", "${credentials.substring(8)}")
	} else {
		return false
	}

	def resp = makeRequest(req)

	if (resp[0] != 200) {
		log.debug "${resp[0]} ${resp[1]} not 200, false"
		return false
	}

	def info = null
	try {
		log.debug "parse json ${resp[0]} ${resp[1]}"
		info = new JsonSlurper().parseText(resp[1])
	} catch (JsonException ex) {
		log.debug "json parse error ${ex.message}"
		return false
	}

	if  (info.userName != username) {
		log.debug "leaving ssoAuth username ${info.user} want ${username}"
		return false
	}


	if (info.scope['upload'] || info.scope['overwrite']) {
		log.debug "return true"
		return true
	}

	return false 
}

def beforeUploadRequestHandle(settings, request, rpath) {
	if (security.isAdmin())
		log.debug "security is Admin, skip check"
		return

	// ignore old repo
	def config = settings.config
	def repo = config.repoIndex[config.repoKeys[rpath.repoKey]]
	if (!repo)
		log.debug "beforeUploadRequestHandle no repo for: ${rpath.repoKey}"
		return

	def (acl, dir, pkg) = getResourceKey(repo, rpath, true)


	if (acl == aclAllow || !dir || repositories.exists(dir))
		return

	if (acl == aclDeny)
		throw new CancelException("deny access to ${rpath.id}", 404)

	actionAccess(getUsername(), createAction, [dir ? dir.getPath() : "" ])
}


def invalidateHandle(settings, params) {
	String path = params?.get('path')?.get(0) as String
	if (!path)
		return [400, "Need a path parameter"]
	
	path = convertLocalPath(settings.config, path)

	log.debug "entering invalidateHandle path ${path}"

	def rpath = settings.rpf.create(path)
	def (repo, files, err) = collectSyncFiles(settings, rpath)
	if (err != "")
		return [400, err]

	def resp = []
	def ret = null
	def status_ = "done"
	def hosts = settings.config.invalidateHosts

	hosts.each { host->
		files.each { file ->
			ret = invalidateApiCall(host, "${repo['remote']}/${file['rpath'].getPath()}", file['md5'])
			if (ret[0] != 200) {
				status_ = "failed"
			}
			resp << "${host}\t${file['rpath'].getName()} ${ret[1]}"
		}
	}
	def json = new JsonBuilder()
	json {
		status "${status_}"
		message resp.join("\n") as String
	}
	return [200, json.toString()]
}

def syncHandle(settings, params) {
	def pathMap = settings.pathMap
	def uidMap = settings.uidMap
	def threadPool = settings.threadPool
	String path = params?.get('path')?.get(0) as String
	if (!path)
		return [400, "Need a path parameter"]

	path = convertLocalPath(settings.config, path)

	def rpath = settings.rpf.create(path)
	def (repo, files, err) = collectSyncFiles(settings, rpath)
	if (err != "")
		return [400, err]

	def uid = null
	synchronized (pathMap) {
		if (path in pathMap) {
			uid = pathMap[path]
		} else {
			uid = UUID.randomUUID()
			uidMap[uid] = [status: 'processing', path: path]
			pathMap[path] = uid
			threadPool.submit {
				log.debug "begin sync ${path} for uid ${uid}"

				def resp = syncDo(threadPool, settings.config.syncHosts, repo['remote'], files)
				synchronized (pathMap) {
					uidMap[uid]['status'] = 'done'
					uidMap[uid]['resp'] = resp.join("\n")
					log.debug "uid ${uid} has done, resp ${resp}"
					pathMap.remove(path)
				}
				threadPool.submit {
					Thread.sleep(600000) // 600s
					if (uid in uidMap)
						uidMap.remove(uid)
				}

			}
		}
	}
	return [200, "{\"uid\":\"$uid\"}"]
}

// open a new thread, and run calculation for a uid, as well as any
// other ids that are queued for the same path
def syncDo(threadPool, hosts, remoteRepo, files) {
	def resp = []
	def ret = null

	log.debug "entering syncDo hosts ${hosts} remoteRpo ${remoteRepo} files ${files}"

	CountDownLatch latch = new CountDownLatch(hosts.size() * files.size())

	hosts.each { host ->
		log.debug "host ${host}"
		files.each { file ->
			log.debug "file ${file}"
			threadPool.submit {
				ret = syncApiCall(host, "${remoteRepo}/${file['rpath'].getPath()}", file['md5'])
				synchronized (resp) {
					resp << "${host}\t${file['rpath'].getName()} ${ret[1]}"
				}
				latch.countDown()
			}
		}
	}
	latch.await()
	return resp
}

// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/sync?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm;md5=xxxxxxx
def syncApiCall(host, path, md5) {
	log.debug "entering syncCacheRpath ${host} ${path}"
	def url = "${host}/artifactory/api/plugins/execute/sync?params=path=${URLEncoder.encode("${path}", "UTF-8")};md5=${md5}"
	def req = new HttpPost(url)
	def resp = makeRequest(req)
	log.info "syncApiCall ${path} ${md5} ${host} ${resp}"
	return resp
}

// usage: curl -X POST http://localhost:8081/artifactory/api/plugins/execute/invalidate?params=path=rpm/ep/iaas/demo-0.1.4-7.Linux.x86_64.rpm;md5=xxxxxxx
def invalidateApiCall(host, path, md5) {
	if(path.contains("generic-remote-fw/adserver/reg/svc-fwk/job-info/")){
		return [200, "Skip ADS Reg Tools Generic Files"]
	}
	log.debug "entering invalidateApiCall ${host} ${path}"
	def url = "${host}/artifactory/api/plugins/execute/invalidate?params=path=${URLEncoder.encode("${path}", "UTF-8")};md5=${md5}"
	def req = new HttpPost(url)
	def resp = makeRequest(req)
	log.info "invalidateApiCall ${path} ${md5} ${host} ${resp}"
	return resp
}

def makeRequest(req) {
	def resp = null, httpclient = HttpClients.createDefault()
	req.addHeader("User-Agent", HttpUtils.artifactoryUserAgent)

	try {
		resp = httpclient.execute(req)
		def ips = resp.entity.content
		def statusCode = resp.statusLine.statusCode
		return [statusCode, ips.text]
	} catch (ex) {
		log.error("Problem making request: $ex.message")
		return [502, "Problem making request: $ex.message"]
	} finally {
		httpclient?.close()
		resp?.close()
	}
}

def syncQueryHandle(settings, params) {
	String uidstr = params?.get('uid')?.get(0) as String
	if (!uidstr)
		return [400, "Need a path parameter"]

	def uidMap = settings.uidMap
	def uid = null
	try {
		uid = UUID.fromString(uidstr)
	} catch (IllegalArgumentException ex) {
		return [400, "Given uid parameter is not a valid uid"]
	}

	def ret = null
	def resp = null
	synchronized (settings.pathMap) {
		if (uid in uidMap) {
			ret = uidMap[uid]['status']
			resp = uidMap[uid]['resp']
		}
	}

	if (!ret)
		return [400, "Given uid was not found"]


	def json = new JsonBuilder()
	json {
		status "${ret}"
		message "${resp}"
	}
	return [200, json.toString()]
}

def dryrunHandle(settings, params) {
	String path = params?.get('path')?.get(0) as String
	String action = params?.get('action')?.get(0) as String

	log.debug "${params}"

	def config = settings.config
	def rpath = settings.rpf.create(path)
	def repo = config.repoIndex[config.repoKeys[rpath.repoKey]]
	if (!repo)
		return [400, "${rpath.repoKey} is unsupported repo"]

	if (action == createAction) {
		if (rpath.isFolder())
			return [400, "${path} is folder"]
		if (repositories.exists(rpath))
			action = overwriteAction
	} else if (action == "delete") {
		if (isEmptyDir(rpath))
			return [200, "OK empty directory"]
		action = overwriteAction
	} else {
		return [400, "bad request, invalid action ${action} (upload/delete)"]
	}

	if (security.isAdmin())
		return [200, "you're an admin"]

	def user = getUsername()
	def (acl, dir, pkg) = getResourceKey(repo, rpath)
	if (acl == aclDeny)
		return [413, "access deny for ${user}@${rpath.id}"]

	if (acl == aclAllow)
		return [200, "allowed directly"]

	actionAccess(user, action, [pkg, dir ? dir.getPath() : ""])

	return [200, "OK ${user} ${action} ${path}\n"]
}

class MasterConfigurationHolder {
	File confFile
	Logger log
	MasterConfiguration current = null
	long confFileLastChecked = 0L
	long confFileLastModified = 0L
	List<String> errors
	String dir = "${ctx.artifactoryHome.etcDir}/plugins"

	MasterConfigurationHolder(ctx, log) {
		this.log = log
		this.confFile = new File(dir, "master.json")
	}

	MasterConfiguration getCurrent() {
		log.debug "Retrieving current conf $confFileLastChecked $confFileLastModified $current"
		if (current == null || needReload()) {
			log.debug "Reloading configuration from ${confFile.getAbsolutePath()}"
			if (!confFile || !confFile.exists()) {
				errors = ["The conf file ${confFile.getAbsolutePath()} does not exists!"]
			} else {
				try {
					current = new MasterConfiguration(confFile, log)
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


class MasterConfiguration {
	String[] invalidateHosts = []
	String[] syncHosts = []
	HashMap repoKeys = new HashMap()
	HashMap repoIndex = new HashMap()

	MasterConfiguration(File confFile, log) {
		def reader
		try {
			reader = new FileReader(confFile)
			def config = new JsonSlurper().parse(reader)
			invalidateHosts = config['invalidate_hosts']
			syncHosts = config['sync_hosts']
			repoKeys = config['repos']['keys']
			repoIndex = config['repos']['index']
		} finally {
			if (reader)
				reader.close()
		}
	}


	def findErrors() {
		if (!(invalidateHosts && syncHosts && repoKeys && repoIndex))
			return ["No data found or declared in build master JSON configuration"]
		return []
	}
}


// precheck file/folder
def collectSyncFiles(settings, rpath) {
	def repo = settings.config.repoIndex[settings.config.repoKeys[rpath.repoKey]]
	if (!repo)
		return [null, null, "${rpath.repoKey} is unsupported"]

	if (!repositories.exists(rpath)) {
		log.debug "${rpath.id} is not exists at repositories, used deleteMagicMd5 to delete from cache node"
		return [repo, [["rpath":rpath, "md5": deleteMagicMd5]], ""]
	}

	def files = []
	def err = ""
	switch (repo['type']) {
	case 'docker':
		// folder -> files
		if (!repositories.exists(settings.rpf.create(rpath.repoKey, "${rpath.getPath()}/manifest.json")))
			return [null, null, "${rpath.id} does not exists!"]

		def children = repositories.getChildren(rpath)
		children.each { child -> 
			def childPath = child.getRepoPath()
			if (childPath.isFile())
				files << ["rpath":childPath, "md5": repositories.getFileInfo(childPath).checksumsInfo.md5]
		}
		break
	case ["maven", "sbt"]:
		err = "unsupported maven and sbt"
		break
	case 'debian':
	case 'gems':
	case 'generic':
	case 'go':
	case 'helm':
	case 'npm':
	case 'pypi':
	case 'rpm':
		if (rpath.isFile())
			files << ["rpath":rpath, "md5": repositories.getFileInfo(rpath).checksumsInfo.md5]
		break
	}

	if (err != "")
		return [null, null, err]

	if (files.size() == 0)
		return [null, null, "invalid repo path"]

	return [repo, files, ""]
}

def convertLocalPath(config, path) {
	def n = path.indexOf("/")
	if (n<0)
		return path

	def key = config.repoKeys[path.substring(0, n)]
	if (!key)
		return path

	return config.repoIndex[key]['local']+path.substring(n)
}

// ugly hack!!
def isTagAdmin() {
	return tagAdmin.contains(getUsername())
}


def storageBeforeFilter(settings, action, rpath) {
	def threadPool = settings.threadPool
	def config = settings.config
	def repo = config.repoIndex[config.repoKeys[rpath.repoKey]]

	def needInvalidate = {
		if (!repo)
			return false

		if (action == createAction) {
			if (rpath.isFolder())
				return false
			if (repositories.exists(rpath))
				action = overwriteAction
		}

		if (security.isAdmin())
			//return true // load too hight, off it
			return false

		def user = getUsername()
		def (acl, dir, pkg) = getResourceKey(repo, rpath)
		if (acl == aclDeny) {
			log.info "${action} ${user}@${rpath.id} deny"
			throw new CancelException("access deny for ${user}@${rpath.id}", 413)
		}

		if (acl == aclAllow) {
			return true
		}
		if (acl == aclSkip) {
			return false
		}

		actionAccess(user, action, [pkg, dir ? dir.getPath() : ""])
		log.info "${action} ${user}@${rpath.id} allow"
		return true
	}


	if (!needInvalidate())
		return

	log.debug "action ${action}"
	if (action == overwriteAction || action == deleteAction) {
		threadPool.submit {
			config.invalidateHosts.each { host ->
				invalidateApiCall(host, "${repo['remote']}/${rpath.getPath()}", deleteMagicMd5)
			}
		}
	}
}

def propertyFilter(settings, action, rpath) {
	def config = settings.config
	def repo = config.repoIndex[config.repoKeys[rpath.repoKey]]

	if (!repo)
		return

	if (security.isAdmin())
		return

	def user = getUsername()
	def (acl, dir, pkg) = getResourceKey(repo, rpath)
	actionAccess(user, action, [pkg, dir ? dir.getPath() : ""])

	log.info "property ${user}@${rpath.id} allow"
	return
}

def isEmptyDir(RepoPath rpath) {
	return repositories.getItemInfo(rpath).folder && repositories.getChildren(rpath).empty
}
