package lol.koblizek.composer.actions

import lol.koblizek.composer.ComposerPlugin
import org.gradle.api.Project
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

abstract class Action() {
    lateinit var temporaryDir: File

    init {
        this::class.simpleName?.let {
            temporaryDir = ComposerPlugin.project.projectDir.toPath().resolve("composer")
                .resolve("actions").resolve(it).createDirectories().toFile()
        }
    }

    abstract fun run(project: Project)

    fun start(project: Project) {
        if (this::class.simpleName != "LoadLibrariesAction" && temporaryDir.toPath().resolve("done.txt").exists()) return
        run(project)
        // temporaryDir.toPath().resolve("done.txt").createFile()
    }
}