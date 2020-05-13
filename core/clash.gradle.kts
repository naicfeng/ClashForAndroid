import org.apache.tools.ant.taskdefs.condition.Os
import java.io.*
import java.util.*
import java.net.*

object Constants {
    const val GEOIP_DATABASE_URL = "https://github.com/Dreamacro/maxmind-geoip/releases/latest/download/Country.mmdb"
    const val GEOIP_INVALID_INTERVAL = 1000L * 60 * 60 * 24 * 7

    const val SOURCE_PATH = "src/main/golang"
    const val OUTPUT_PATH = "extraSources"

    const val GOLANG_BASE = "intermediates/golang"
    const val GOLANG_PATH = "$GOLANG_BASE/path"
    const val GOLANG_BIND = "$GOLANG_BASE/bind"
    const val GOLANG_BINARY = "$GOLANG_PATH/bin"
    const val GOLANG_OUTPUT = "$GOLANG_BASE/bridge.aar"
    const val GOLANG_OUTPUT_SOURCES = "$GOLANG_BASE/bridge-sources.jar"

    val STUB_GO_FILE_CONTENT = """
        package main

        import "github.com/kr328/cfa/bridge"
            
        func main() {}
    """.trimIndent()
    val STUB_GO_MOD_CONTENT = """
        module github.com/kr328/cfa-bind
        
        require github.com/kr328/cfa v0.0.0 // redirect
        
        replace github.com/kr328/cfa => {SOURCE_PATH}
        
    """.trimIndent()

    val REGEX_REPLACE = Regex("replace\\s+(\\S+)\\s+(\\S*)\\s*=>\\s*(\\S+)\\s*(\\S*)\\s*")
    val REGEX_JNI = Regex("^jni/")
}

fun generateGolangBuildEnvironment(vararg pathAppend: String): Map<String, String> {
    val environment = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).apply { putAll(System.getenv()) }
    val properties = Properties().apply { load(rootProject.file("local.properties").inputStream()) }

    val sdkPath = properties.getProperty("sdk.dir")
        ?: throw GradleScriptException("sdk.dir not found", FileNotFoundException())
    val ndkPath = properties.getProperty("ndk.dir")
        ?: throw GradleScriptException("ndk.dir not found", FileNotFoundException())

    val pathSeparator = if ( Os.isFamily(Os.FAMILY_WINDOWS) ) ";" else ":"

    environment["GOPATH"] = listOf(buildDir.resolve(Constants.GOLANG_PATH).absolutePath, *pathAppend)
        .joinToString(separator = pathSeparator)
    environment["ANDROID_HOME"] = sdkPath
    environment["ANDROID_NDK_HOME"] = ndkPath
    environment["PATH"] += "$pathSeparator${buildDir.resolve(Constants.GOLANG_BINARY)}"

    return environment
}

fun generateGolangModule(): String {
    val moduleFile = file(Constants.SOURCE_PATH).resolve("go.mod")

    val replaces = moduleFile
        .readLines()
        .asSequence()
        .map { line -> Constants.REGEX_REPLACE.matchEntire(line) }
        .filterNotNull()
        .map { match ->
            val source = match.groupValues[1].trim()
            val sVersion = match.groupValues[2].trim()
            val target = match.groupValues[3].trim()
            val tVersion = match.groupValues[4].trim()

            val resolvedTarget = if ( target.startsWith("./") )
                moduleFile.parentFile!!.resolve(target).canonicalPath
            else
                target

            "replace $source $sVersion => $resolvedTarget $tVersion"
        }.joinToString(separator = "\n")

    return Constants.STUB_GO_MOD_CONTENT
        .replace("{SOURCE_PATH}", file(Constants.SOURCE_PATH).absolutePath) + replaces
}

fun String.exec(pwd: File = buildDir, env: Map<String, String> = System.getenv()): String {
    val process = ProcessBuilder().run {
        if ( Os.isFamily(Os.FAMILY_WINDOWS) )
            command("cmd.exe", "/c", this@exec)
        else
            command("bash", "-c", this@exec)

        environment().putAll(env)
        directory(pwd)

        redirectErrorStream(true)

        start()
    }

    val outputStream = ByteArrayOutputStream()
    process.inputStream.copyTo(outputStream)

    if ( process.waitFor() != 0 ) {
        println(outputStream.toString("utf-8"))
        throw GradleScriptException("Exec $this failure", IOException())
    }

    return outputStream.toString("utf-8")
}

task("generateClashBindSources") {
    onlyIf {
        val lastModified = file(Constants.SOURCE_PATH).walk()
            .filter { it.extension == "go" || it.extension == "mod" }
            .map { it.lastModified() }
            .max() ?: 0L

        return@onlyIf lastModified > buildDir.resolve(Constants.GOLANG_OUTPUT).lastModified()
    }

    doFirst {
        buildDir.resolve(Constants.GOLANG_BIND).apply {
            deleteRecursively()
            mkdirs()
        }
    }

    doLast {
        val environment = generateGolangBuildEnvironment()

        val bind = buildDir.resolve(Constants.GOLANG_BIND).apply {
            resolve("main.go").writeText(Constants.STUB_GO_FILE_CONTENT)
            resolve("go.mod").writeText(generateGolangModule())
        }

        "go mod vendor".exec(pwd = bind, env = environment)

        buildDir.resolve(Constants.GOLANG_BIND).apply {
            resolve("vendor").renameTo(resolve("src"))
            resolve("go.mod").delete()
            resolve("main.go").delete()
            resolve("go.sum").delete()
        }
    }
}

task("assembleClashCore") {
    dependsOn(tasks["generateClashBindSources"])

    onlyIf {
        !tasks["generateClashBindSources"].state.skipped
    }

    doFirst {
        val environment = generateGolangBuildEnvironment()

        "go get golang.org/x/mobile/cmd/gomobile".exec(env = environment)
    }

    doLast {
        val bind = buildDir.resolve(Constants.GOLANG_BIND)
        val environment = generateGolangBuildEnvironment(bind.absolutePath)

        "gomobile init".exec(pwd = bind, env = environment)
        "gomobile bind -target=android -trimpath github.com/kr328/cfa/bridge"
            .exec(pwd = buildDir.resolve(Constants.GOLANG_BASE), env = environment)
    }
}

task("extractSources", type = Copy::class) {
    dependsOn(tasks["assembleClashCore"])

    from(zipTree(buildDir.resolve(Constants.GOLANG_OUTPUT))) {
        include("**/*.so")
        eachFile {
            path = path.replace(Constants.REGEX_JNI, "jniLibs/")
        }
    }
    from(zipTree(buildDir.resolve(Constants.GOLANG_OUTPUT_SOURCES))) {
        include("**/*.java")
        into("classes")
    }

    destinationDir = buildDir.resolve(Constants.OUTPUT_PATH)
}

task("downloadGeoipDatabase") {
    dependsOn(tasks["extractSources"])

    onlyIf {
        val file = buildDir.resolve(Constants.OUTPUT_PATH).resolve("assets/Country.mmdb")

        System.currentTimeMillis() - file.lastModified() > Constants.GEOIP_INVALID_INTERVAL
    }

    doLast {
        val assets = buildDir.resolve(Constants.OUTPUT_PATH).resolve("assets")

        assets.mkdirs()

        URL(Constants.GEOIP_DATABASE_URL).openConnection().getInputStream().use { input ->
            FileOutputStream(assets.resolve("Country.mmdb")).use { output ->
                input.copyTo(output)
            }
        }
    }
}

task("resetGolangPathMode", type = Exec::class) {
    onlyIf {
        !Os.isFamily(Os.FAMILY_WINDOWS)
    }

    commandLine("chmod", "-R", "777", buildDir.resolve(Constants.GOLANG_PATH))

    isIgnoreExitValue = true
}