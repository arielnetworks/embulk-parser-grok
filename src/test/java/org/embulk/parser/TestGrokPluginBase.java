package org.embulk.parser;

import org.embulk.parser.grok.GrokGuessPlugin;
import org.embulk.parser.grok.GrokParserPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.util.EmbulkPluginTester;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class TestGrokPluginBase {

    protected String generateConfigYaml(String template, Map<String, String> params) throws IOException {
        File generatedFile = File.createTempFile("generated", "yaml");

        String templatePath = TestGrokParserPlugin.class.getClassLoader().getResource(template).getPath();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(templatePath)));
             BufferedWriter writer = new BufferedWriter(new FileWriter(generatedFile))) {
            String line;
            while ((line = br.readLine()) != null) {

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    line = line.replaceAll(entry.getKey(), entry.getValue());
                }
                writer.write(line + "\n");
            }
        }

        return generatedFile.getAbsolutePath();
    }

    protected Path outputDirectoryPath;
    protected EmbulkPluginTester tester;

    @Before
    public void before() throws IOException {
        outputDirectoryPath = Files.createTempDirectory(null);
        tester = new EmbulkPluginTester();
        tester.addPlugin(ParserPlugin.class, "grok", GrokParserPlugin.class);
        tester.addPlugin(GuessPlugin.class, "grok", GrokGuessPlugin.class);
    }

    @After
    public void after() throws IOException {
        Files.walkFileTree(outputDirectoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return super.visitFile(file, attrs);
            }
        });
    }

}
