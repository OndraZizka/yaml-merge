package ch.zizka.yaml.merger

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.*

class YamlMergerTest {
    private val yaml = Yaml()
    private val merger = YamlMerger()

    private val log: Logger = LoggerFactory.getLogger(YamlMergerTest::class.java)

    val YAML_1: String = getResourceFile("test1.yaml")
    val YAML_2: String = getResourceFile("test2.yaml")
    val YAML_NULL: String = getResourceFile("test-null.yaml")
    val YAML_COLON: String = getResourceFile("test-colon.yaml")
    val MERGE_YAML_1: String = getResourceFile("testListMerge1.yaml")
    val MERGE_YAML_2: String = getResourceFile("testListMerge2.yaml")

    @Test
    fun testMerge2Files() {
        val merged = merger.mergeYamlFiles(arrayOf(YAML_1, YAML_2))
        var dbconfig = merged["database"] as Map<String, Any>?
        Assertions.assertEquals(dbconfig!!["user"].toString(), "alternate-user", "wrong user")
        Assertions.assertEquals(dbconfig["url"], "jdbc:mysql://localhost:3306/some-db", "wrong db url")

        val mergedYmlString = merger.exportToString(merged)
        log.info("Resulting YAML: \n$mergedYmlString")

        val reloadedYaml = yaml.load<Map<String, Any>>(mergedYmlString)
        dbconfig = reloadedYaml["database"] as Map<String, Any>?
        Assertions.assertEquals(dbconfig!!["user"], "alternate-user", "wrong user")
        Assertions.assertEquals(dbconfig["url"], "jdbc:mysql://localhost:3306/some-db", "wrong db url")
        val dbProperties = dbconfig["properties"] as Map<String, Any>?
        Assertions.assertEquals(dbProperties!!["hibernate.dialect"], "org.hibernate.dialect.MySQL5InnoDBDialect", "wrong db url")
    }

    @Test @Disabled("I need to generate some test data.")
    fun testMerge2Files_Large() {
        val merged = merger.mergeYamlFiles(arrayOf(
            getResourceFile("largeFiles_/report-2024-08-23-part1.json"),
            getResourceFile("largeFiles_/report-2024-08-23-part2.json"),
        ))
        val mergedYmlString = merger.exportToString(merged)
    }

    @Test
    fun testMergeFileIntoSelf() {
        val merged = merger.mergeYamlFiles(arrayOf(YAML_1, YAML_1))

        val dbconfig = merged["database"] as Map<String, Any>?
        Assertions.assertEquals(dbconfig!!["user"], "some-user", "wrong user")
        Assertions.assertEquals(dbconfig["url"], "jdbc:mysql://localhost:3306/some-db", "wrong db url")
    }

    @Test
    fun testNullValue() {
        val merged = merger.mergeYamlFiles(arrayOf(YAML_NULL))

        Assertions.assertNotNull(merged["prop1"])
        Assertions.assertNull(merged["prop2"])
    }

    @Test
    fun testSubstitutionValueWithColon() {
        val variables = Collections.singletonMap("ENV_VAR", "localhost")
        val merged = YamlMerger().setVariablesToReplace(variables).mergeYamlFiles(arrayOf(YAML_COLON))

        val hash = merged["memcache"] as Map<String, Any>?
        Assertions.assertEquals(hash!!["one_key"], "value1")
        Assertions.assertEquals(hash["another_key"], "localhost:22133")
        Assertions.assertEquals(hash["some_other_key"], "value2")
    }

    @Test
    fun testMerge2Lists() {
        val merged = merger.mergeYamlFiles(arrayOf(MERGE_YAML_1, MERGE_YAML_2))

        val hash1 = merged["hashlevel1"] as Map<String, Any>?
        val list1 = hash1!!["listlevel2"] as List<Any>?
        Assertions.assertEquals(list1!!.size, 2, "NotEnoughEntries")
        val optionSet1 = list1[0] as Map<String, Any>
        val optionSet2 = list1[1] as Map<String, Any>
        Assertions.assertEquals(optionSet1["namespace"], "namespace1")
        Assertions.assertEquals(optionSet1["option_name"], "option1")
        Assertions.assertEquals(optionSet2["namespace"], "namespace2")
        Assertions.assertEquals(optionSet2["option_name"], "option2")
    }


    fun getResourceFile(file: String) = File(System.getProperty("user.dir") + "/src/test/resources/" + file).absolutePath
}
