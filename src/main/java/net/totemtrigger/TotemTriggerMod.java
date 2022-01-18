package net.totemtrigger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TotemTriggerMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger("fabric-totem-trigger");

	private static final String ABSORPTION_KEY = "effect.minecraft.absorption";
	private static final String FIRE_RESISTANCE_KEY = "effect.minecraft.fire_resistance";
	private static final String REGENERATION_KEY = "effect.minecraft.regeneration";

	public MinecraftClient minecraftClient;

	private boolean respondedToPreviousTotemUsage = false;

	@Override
	public void onInitialize() {
		if (minecraftClient == null) {
			minecraftClient = MinecraftClient.getInstance();
		}

		ClientTickEvents.END_CLIENT_TICK.register(e -> this.onClientTick());
		LOGGER.info("Initialized Totem Trigger mod");
	}

	private void onClientTick() {
		if (minecraftClient.isPaused() && minecraftClient.isInSingleplayer()) {
			return;
		}

		ClientPlayerEntity player = minecraftClient.player;
		if (player == null) {
			return;
		}

		// We detect totem usage by monitoring the player's status effects
		//
		// When activated, a totem removes all status effects, then gives the player:
		// - Absorption 2 for 5 seconds
		// - Fire Resistance 2 for 40 seconds
		// - Regeneration 2 for 45 seconds
		//
		// Known issue: we can't detect a second totem usage less than 5 seconds after
		// the first totem usage, but this is unlikely to occur

		boolean hasAbsorption = false;
		boolean hasFireResistance = false;
		boolean hasRegeneration = false;

		for (StatusEffectInstance statusEffect : player.getStatusEffects()) {
			String key = statusEffect.getEffectType().getTranslationKey();
			int level = statusEffect.getAmplifier() + 1;
			double durationInSeconds = statusEffect.getDuration() / 20f;

			if (key.equals(ABSORPTION_KEY) && level == 2 && durationInSeconds < 5f) {
				hasAbsorption = true;
			} else if (key.equals(FIRE_RESISTANCE_KEY) && level == 1 && durationInSeconds < 40f) {
				hasFireResistance = true;
			} else if (key.equals(REGENERATION_KEY) && level == 2 && durationInSeconds < 45f) {
				hasRegeneration = true;
			}
		}

		boolean totemWasUsed = hasAbsorption && hasFireResistance && hasRegeneration;
		if (totemWasUsed) {
			if (!respondedToPreviousTotemUsage) {
				LOGGER.info("Detected totem usage; running Totem Trigger commands");
				player.sendSystemMessage(
						new TranslatableText("text.totemtrigger.runningCommands").formatted(Formatting.GREEN),
						player.getUuid());
				TotemTriggerConfig config = ConfigManager.getConfig();
				executeCommands(player, config);
				respondedToPreviousTotemUsage = true;
			}
		} else {
			respondedToPreviousTotemUsage = false;
		}
	}

	private void executeCommands(ClientPlayerEntity player, TotemTriggerConfig config) {
		List<String> commands = Arrays.asList(config.command1, config.command2, config.command3);
		for (String command : commands) {
			if (!command.isBlank()) {
				player.sendChatMessage(command);
			}
		}
	}
}
