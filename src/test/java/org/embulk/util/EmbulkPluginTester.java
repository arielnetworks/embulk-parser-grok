package org.embulk.util;

import com.google.inject.Module;
import org.embulk.EmbulkEmbed;
import org.embulk.EmbulkEmbed.Bootstrap;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.exec.ExecutionResult;
import org.embulk.plugin.InjectedPluginSource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EmbulkPluginTester {
    private static class PluginDefinition {
        public final Class<?> iface;
        public final String name;
        public final Class<?> impl;

        public PluginDefinition(Class<?> iface, String name, Class<?> impl) {
            this.iface = iface;
            this.name = name;
            this.impl = impl;
        }
    }

    private final List<PluginDefinition> plugins = new ArrayList<>();

    public EmbulkPluginTester() {
    }

    public void addPlugin(Class<?> iface, String name, Class<?> impl) {
        plugins.add(new PluginDefinition(iface, name, impl));
    }

    private EmbulkEmbed prepare() {
        Bootstrap bootstrap = new EmbulkEmbed.Bootstrap();
        bootstrap.addModules(Collections.singletonList((Module) binder -> {
            for (PluginDefinition plugin : plugins) {
                InjectedPluginSource.registerPluginTo(binder, plugin.iface, plugin.name, plugin.impl);
            }
        }));
        return bootstrap.initialize();
    }

    public void run(String ymlPath) throws IOException {
        EmbulkEmbed embed = prepare();
        try {
            ConfigSource config = embed.newConfigLoader().fromYamlFile(new File(ymlPath));
            ExecutionResult result = embed.run(config);
            result.getIgnoredExceptions().stream().forEach(ex -> System.out.println(ex.getMessage()));
        } catch (IOException e) {
            System.out.println("error:" + e.getMessage());
            throw e;
        } finally {
            embed.destroy();
        }
    }

    public String guess(String ymlPath) throws IOException {
        EmbulkEmbed embed = prepare();
        try {
            ConfigSource config = embed.newConfigLoader().fromYamlFile(new File(ymlPath));
            ConfigDiff diff = embed.guess(config);
            Object obj = embed.getModelManager().readObject(Object.class,embed.getModelManager().writeObject(diff));
            return new Yaml().dump(obj);
        } catch (IOException e) {
            System.out.println("error:" + e.getMessage());
            throw e;
        } finally {
            embed.destroy();
        }
    }

}