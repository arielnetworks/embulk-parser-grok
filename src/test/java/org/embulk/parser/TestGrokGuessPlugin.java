package org.embulk.parser;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestGrokGuessPlugin extends TestGrokPluginBase {

    public void testGuess(String template, String input, String expected) throws Exception {

        String inputPath = TestGrokParserPlugin.class.getClassLoader().getResource(input).getPath();

        Map<String, String> params = ImmutableMap.of(
                "__INPUT_PATH__", inputPath,
                "__PROJECT_DIR__", System.getProperty("user.dir")
        );
        String yamlPath = generateConfigYaml(template, params);
        String diff = tester.guess(yamlPath);

        assertThat(diff, is(expected));
    }

    @Test
    public void testApacheGuess() throws Exception {
        testGuess("guess.yml", "apache.log", "in:\n" +
                "  parser:\n" +
                "    grok_pattern: '%{COMBINEDAPACHELOG}'\n" +
                "    columns:\n" +
                "    - {name: request, type: string}\n" +
                "    - {name: agent, type: string}\n" +
                "    - {name: COMMONAPACHELOG, type: string}\n" +
                "    - {name: auth, type: string}\n" +
                "    - {name: ident, type: string}\n" +
                "    - {name: verb, type: string}\n" +
                "    - {name: referrer, type: string}\n" +
                "    - {name: bytes, type: long}\n" +
                "    - {name: response, type: long}\n" +
                "    - {name: clientip, type: string}\n" +
                "    - {name: COMBINEDAPACHELOG, type: string}\n" +
                "    - {name: httpversion, type: string}\n" +
                "    - {name: rawrequest, type: string}\n" +
                "    - {name: timestamp, format: '%d/%b/%Y:%T %z', type: timestamp}\n");

    }

}
