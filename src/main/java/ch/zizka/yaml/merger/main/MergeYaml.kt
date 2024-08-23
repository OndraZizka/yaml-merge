package ch.zizka.yaml.merger.main

import ch.zizka.yaml.merger.YamlMerger
import java.nio.file.Path
import java.nio.file.Paths

object MergeYaml {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val yamlMerger = YamlMerger()
        yamlMerger.setVariablesToReplace(System.getenv())

        val filesToMerge: MutableList<Path> = ArrayList()

        //Arrays.asList(args).stream().map(Paths::get).collect(Collectors.toSet());
        for (arg in args) {
            filesToMerge.add(Paths.get(arg))
        }

        val resultingYaml = yamlMerger.mergeToString(filesToMerge)

        println(resultingYaml)
    }
}
