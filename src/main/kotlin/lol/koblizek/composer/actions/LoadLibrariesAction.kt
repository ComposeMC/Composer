package lol.koblizek.composer.actions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import lol.koblizek.composer.ComposerPlugin
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import kotlin.io.path.notExists
import kotlin.io.path.readText

class LoadLibrariesAction : Action() {

    private fun shouldDownload(library: JsonObject): Boolean {
        if (library.getAsJsonArray("rules") == null) return true
        val name = library.getAsJsonArray("rules")[0]
            .asJsonObject.getAsJsonObject("os")
            .getAsJsonPrimitive("name").asString
        return if (SystemUtils.IS_OS_WINDOWS && name == "windows") {
            true
        } else if (SystemUtils.IS_OS_MAC_OSX && name == "osx") {
            true
        } else if (SystemUtils.IS_OS_LINUX && name == "linux") {
            true
        } else {
            false
        }
    }

    private fun isNative(libraryName: String): Boolean {
        return libraryName.contains("native")
    }

    override fun run(project: Project) {
        val l = ComposerPlugin.genFiles.temporaryDir.toPath().resolve("libraries.json")
        if (l.notExists()) {
            println("Cannot apply libraries: libraries.json is missing")
            return
        }
        project.repositories.add(project.repositories.maven {
            it.url = project.uri("https://libraries.minecraft.net")
        })

        val libraries = Gson().fromJson(l.readText(), JsonArray::class.java)

        libraries.forEach { t ->
            val library = t.asJsonObject
            if (shouldDownload(library)) {
                project.dependencies.add(if (isNative(library.getAsJsonPrimitive("name").asString)) "runtimeOnly" else "implementation", library.getAsJsonPrimitive("name").asString)
            }
        }
    }
}