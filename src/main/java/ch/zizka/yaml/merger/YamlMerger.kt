@file:Suppress("LoggingStringTemplateAsArgument")

package ch.zizka.yaml.merger

import com.github.mustachejava.DefaultMustacheFactory
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class YamlMerger {
    private val snakeYaml: Yaml
    private val variablesToReplace: MutableMap<String, Any> = HashMap()

    init {
        // See https://github.com/spariev/snakeyaml/blob/master/src/test/java/org/yaml/snakeyaml/DumperOptionsTest.java
        val dumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        dumperOptions.isPrettyFlow = true
        //dumperOptions.setCanonical(true);
        dumperOptions.timeZone = TimeZone.getTimeZone("UTC")
        this.snakeYaml = Yaml(dumperOptions)
    }

    fun setVariablesToReplace(vars: Map<String, String>): YamlMerger {
        variablesToReplace.clear()
        variablesToReplace.putAll(vars)
        return this
    }


    @Throws(IOException::class)
    fun mergeYamlFiles(pathsStr: Array<String>): Map<String, Any?> {
        return mergeYamlFiles(stringsToPaths(pathsStr))
    }

    /**
     * Merges the files at given paths to a map representing the resulting YAML structure.
     */
    @Throws(IOException::class)
    fun mergeYamlFiles(paths: List<Path>): Map<String, Any?> {
        val mergedResult: MutableMap<String, Any?> = LinkedHashMap()
        for (yamlFilePath in paths) {
            var inputStream: InputStream? = null
            try {
                val file = yamlFilePath.toFile()
                if (!file.exists()) throw FileNotFoundException("YAML file to merge not found: " + file.canonicalPath)

                // Read the YAML file into a String
                inputStream = FileInputStream(file)
                val entireFile = IOUtils.toString(inputStream, StandardCharsets.UTF_8) // TBD allow setting the charset?

                // Substitute variables. TODO: This should be done by a resolver when parsing.
                val bufferSize = entireFile.length + 100
                val writer = StringWriter(bufferSize)
                DEFAULT_MUSTACHE_FACTORY.compile(StringReader(entireFile), "yaml-mergeYamlFiles-" + System.currentTimeMillis())
                    .execute(writer, variablesToReplace)

                // Parse the YAML.
                val yamlString = writer.toString()
                val yamlToMerge = snakeYaml.load<Map<String, Any>>(yamlString)

                // Merge into results map.
                mergeStructures(mergedResult, yamlToMerge)
                LOG.debug("Loaded YAML from $yamlFilePath: $yamlToMerge")
            }
            finally {
                inputStream?.close()
            }
        }
        return mergedResult
    }

    private fun mergeStructures(targetTree: MutableMap<String, Any?>, sourceTree: Map<String, Any>?) {
        if (sourceTree == null) return

        for (key in sourceTree.keys) {
            val yamlValue = sourceTree[key]
            if (yamlValue == null) {
                addToMergedResult(targetTree, key, yamlValue)
                continue
            }

            val existingValue = targetTree[key]
            if (existingValue != null) {
                if (yamlValue is Map<*, *>) {
                    if (existingValue is Map<*, *>) {
                        mergeStructures(existingValue as MutableMap<String, Any?>, yamlValue as Map<String, Any>)
                    }
                    else if (existingValue is String) { throw IllegalArgumentException("Cannot mergeYamlFiles complex element into a simple element: $key") }
                    else throw unknownValueType(key, yamlValue)
                }
                else if (yamlValue is List<*>) {
                    mergeLists(targetTree, key, yamlValue)
                }
                else if (yamlValue is String
                    || yamlValue is Boolean
                    || yamlValue is Double
                    || yamlValue is Int
                ) {
                    LOG.debug("Overriding value of $key with value $yamlValue")
                    addToMergedResult(targetTree, key, yamlValue)
                }
                else {
                    throw unknownValueType(key, yamlValue)
                }
            } else {
                if (yamlValue is Map<*, *>
                    || yamlValue is List<*>
                    || yamlValue is String
                    || yamlValue is Boolean
                    || yamlValue is Int
                    || yamlValue is Double
                ) {
                    LOG.debug("Adding new key->value: $key -> $yamlValue")
                    addToMergedResult(targetTree, key, yamlValue)
                } else {
                    throw unknownValueType(key, yamlValue)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun mergeToString(filesToMerge: List<Path>): String {
        val merged = mergeYamlFiles(filesToMerge)
        return exportToString(merged)
    }

    fun exportToString(merged: Map<String, Any?>?): String {
        return snakeYaml.dump(merged)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(YamlMerger::class.java)
        val DEFAULT_MUSTACHE_FACTORY: DefaultMustacheFactory = DefaultMustacheFactory()

        private fun unknownValueType(key: String, yamlValue: Any): IllegalArgumentException {
            val msg = "Cannot mergeYamlFiles element of unknown type: " + key + ": " + yamlValue.javaClass.name
            LOG.error(msg)
            return IllegalArgumentException(msg)
        }

        private fun addToMergedResult(mergedResult: MutableMap<String, Any?>, key: String, yamlValue: Any?) {
            mergedResult[key] = yamlValue
        }

        private fun mergeLists(mergedResult: Map<String, Any?>, key: String, yamlValue: Any) {
            require(yamlValue is List<*> && mergedResult[key] is List<*>) { "Cannot mergeYamlFiles a list with a non-list: $key" }

            val originalList = mergedResult[key] as MutableList<Any>?
            originalList!!.addAll(yamlValue as List<Any>)
        }


        // Util methods
        fun stringsToPaths(pathsStr: Array<String>): List<Path> {
            val paths: MutableSet<Path> = LinkedHashSet()
            for (pathStr in pathsStr) {
                paths.add(Paths.get(pathStr))
            }
            val pathsList: MutableList<Path> = ArrayList(paths.size)
            pathsList.addAll(paths)
            return pathsList
        }
    }
}
