package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreenFactory {
    public static Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.immersivecompanions.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.immersivecompanions.category.gameplay"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.enableCriticalInjury"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.enableCriticalInjury.desc")))
                                .binding(true,
                                        () -> ModConfig.enableCriticalInjury,
                                        v -> ModConfig.enableCriticalInjury = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.criticalInjuryThreshold"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.criticalInjuryThreshold.desc")))
                                .binding(4.0f,
                                        () -> ModConfig.criticalInjuryThreshold,
                                        v -> ModConfig.criticalInjuryThreshold = v)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(1.0f, 10.0f)
                                        .step(0.5f))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.criticalInjurySpeedMultiplier"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.criticalInjurySpeedMultiplier.desc")))
                                .binding(0.5f,
                                        () -> ModConfig.criticalInjurySpeedMultiplier,
                                        v -> ModConfig.criticalInjurySpeedMultiplier = v)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0.1f, 1.0f)
                                        .step(0.1f))
                                .build())
                        .build())
                .save(ModConfig::save)
                .build()
                .generateScreen(parent);
    }
}
