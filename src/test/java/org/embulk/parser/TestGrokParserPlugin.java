package org.embulk.parser;

import com.google.common.collect.ImmutableMap;
import org.embulk.util.StreamUtil;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class TestGrokParserPlugin extends TestGrokPluginBase{


    public void testRun(String template, String input, String expected) throws Exception {

        String inputPath = TestGrokParserPlugin.class.getClassLoader().getResource(input).getPath();
        Path outputPath = Paths.get(outputDirectoryPath.toString(), input);
        String expectedPath = TestGrokParserPlugin.class.getClassLoader().getResource(expected).getPath();

        Map<String, String> params = ImmutableMap.of(
                "__INPUT_PATH__", inputPath,
                "__OUTPUT_PATH__", outputPath.toString(),
                "__PROJECT_DIR__", System.getProperty("user.dir")
        );
        String yamlPath = generateConfigYaml(template, params);
        tester.run(yamlPath);

        Path outputFilePath = Paths.get(outputDirectoryPath.toString(), input + "000.00.output.csv");
        List<Integer> counter = new ArrayList<>();
        counter.add(0);
        StreamUtil.zip(Files.lines(outputFilePath), Files.lines(Paths.get(expectedPath)))
                .forEach(tuple -> {
                    Integer line = counter.get(0);
                    line++;
                    counter.set(0, line);
                    assertEquals(tuple.b, tuple.a);
                });
    }

    @Test
    public void testApacheLog() throws Exception {
        testRun("apache.yml", "apache.log", "expected_apache.csv");
    }

    @Test
    public void testMultiLineLog() throws Exception {
        testRun("multiline.yml", "multiline.log", "expected_multiline.csv");
    }

}
