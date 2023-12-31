package lol.koblizek.composer.task

import lol.koblizek.composer.ComposerPlugin
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingNsCompleter
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import net.minecraftforge.fart.api.Renamer
import net.minecraftforge.fart.api.SignatureStripperConfig
import net.minecraftforge.fart.api.SourceFixerConfig
import net.minecraftforge.fart.api.Transformer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.util.regex.Pattern

abstract class DeobfuscateTask : DefaultTask() {

    init {
        group = "composer"
        description = "Deobfusctaes source code"
        dependsOn("downloadMappings")
    }

    private fun deobfuscate(inputJar: File, outputPath: File, mappings: File) {
        val writer = StringWriter()
        MappingWriter.create(writer, MappingFormat.TINY_2).use { mapper ->
            MappingReader.read(
                mappings.toPath(), MappingNsCompleter(
                    MappingSourceNsSwitch(mapper, "official", true), emptyMap<String, String>()
                )
            )
        }
        val remapper = TinyRemapper.newRemapper().invalidLvNamePattern(Pattern.compile("\\$\\$\\d+"))
            .renameInvalidLocals(true)
            .inferNameFromSameLvIndex(true)
            .withMappings(
                TinyUtils.createTinyMappingProvider(
                    BufferedReader(StringReader(writer.toString())),
                    "official",
                    "named"
                )
            ).build()
        writer.close()
        try {
            OutputConsumerPath.Builder(outputPath.toPath()).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(inputJar.toPath(), NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(inputJar.toPath())
                remapper.apply(outputConsumer)
                outputConsumer.close()
            }
        } catch (e: IOException) {
            println("Error occurred but was ignored")
        } finally {
            remapper.finish()
        }
    }

    @TaskAction
    fun run() {
        if (temporaryDir.resolve("checked").exists()) return
        val unDeobf = ComposerPlugin.genFiles.temporaryDir.toPath().resolve("server.jar").toFile()
        val mappings = ComposerPlugin.downloadMappings.temporaryDir.toPath().resolve("mappings.tiny").toFile()
        Renamer.builder().add(Transformer.recordFixerFactory())
            .add(Transformer.sourceFixerFactory(SourceFixerConfig.JAVA))
            .add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL)).build()
            .run(unDeobf, temporaryDir.toPath().resolve("server-art.jar").toFile())
        val arted = temporaryDir.toPath().resolve("server-art.jar").toFile()
        val deobf = temporaryDir.toPath().resolve("server-deobf.jar").toFile()
        deobfuscate(
            arted,
            deobf,
            mappings
        )
        temporaryDir.resolve("checked").createNewFile()
    }
}