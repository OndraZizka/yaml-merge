package ch.zizka.yaml.merger.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ch.zizka.yaml.merger.YamlMerger;

public class MergeYaml {

    public static void main (String[] args) throws Exception {

        YamlMerger yamlMerger = new YamlMerger();
        yamlMerger.setVariablesToReplace(System.getenv());

        List<Path> filesToMerge = new ArrayList<>();

        //Arrays.asList(args).stream().map(Paths::get).collect(Collectors.toSet());
        for (String arg : args) {
            filesToMerge.add(Paths.get(arg));
        }

        String resultingYaml = yamlMerger.mergeToString(filesToMerge);

        System.out.println(resultingYaml);
    }

}
