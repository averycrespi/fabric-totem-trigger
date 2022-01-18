package net.totemtrigger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

public class ConfigManager {
    private static File findConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = Paths.get(configDir.toString(), "totemtrigger", "config.json").toFile();
        configFile.getParentFile().mkdirs();
        try {
            configFile.createNewFile();
        } catch (IOException e) {
            TotemTriggerMod.LOGGER.error("Failed to create config file because: " + e.getMessage());
        }
        return configFile;
    }

    private static TotemTriggerConfig loadConfigFromFile(File configFile) {
        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(configFile.toPath());
            TotemTriggerConfig config = gson.fromJson(reader, TotemTriggerConfig.class);
            if (config == null) {
                TotemTriggerMod.LOGGER.warn("Config is null; falling back to default config");
                return new TotemTriggerConfig();
            } else {
                return config;
            }
        } catch (IOException e) {
            TotemTriggerMod.LOGGER.error("Failed to load config from file because: " + e.getMessage());
            TotemTriggerMod.LOGGER.warn("Falling back to default config");
            return new TotemTriggerConfig();
        }
    }

    private static void saveConfigToFile(TotemTriggerConfig config, File configFile) {
        Gson gson = new Gson();
        String configString = gson.toJson(config);
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(configString);
            writer.close();
            TotemTriggerMod.LOGGER.info("Saved config to file");
        } catch (IOException e) {
            TotemTriggerMod.LOGGER.error("Failed to save config to file because: " + e.getMessage());
        }
    }

    public static TotemTriggerConfig getConfig() {
        return loadConfigFromFile(findConfigFile());
    }

    public static Screen createConfigScreen(Screen parentScreen) {
        File configFile = findConfigFile();
        TotemTriggerConfig config = loadConfigFromFile(configFile);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(new TranslatableText("title.totemtrigger.config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory commandsCategory = builder
                .getOrCreateCategory(new TranslatableText("category.totemtrigger.commands"));

        commandsCategory
                .addEntry(
                        entryBuilder.startStrField(new TranslatableText("option.totemtrigger.command"), config.command)
                                .setDefaultValue("/say Hello!")
                                .setTooltip(new TranslatableText("option.totemtrigger.command.tooltip"))
                                .setSaveConsumer(newValue -> config.command = newValue)
                                .build());

        builder.setSavingRunnable(() -> {
            saveConfigToFile(config, configFile);
        });

        return builder.build();
    }
}
