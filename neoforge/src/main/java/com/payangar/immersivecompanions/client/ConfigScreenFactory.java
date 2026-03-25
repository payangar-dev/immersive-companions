package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
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
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.enableCompanionRevival"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.enableCompanionRevival.desc")))
                                .binding(true,
                                        () -> ModConfig.enableCompanionRevival,
                                        v -> ModConfig.enableCompanionRevival = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.companionRevivalTicks"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.companionRevivalTicks.desc")))
                                .binding(60,
                                        () -> ModConfig.companionRevivalTicks,
                                        v -> ModConfig.companionRevivalTicks = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(20, 200)
                                        .step(10))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.enableAgony"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.enableAgony.desc")))
                                .binding(true,
                                        () -> ModConfig.enableAgony,
                                        v -> ModConfig.enableAgony = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.agonyDurationTicks"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.agonyDurationTicks.desc")))
                                .binding(1200,
                                        () -> ModConfig.agonyDurationTicks,
                                        v -> ModConfig.agonyDurationTicks = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(200, 2400)
                                        .step(100))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.agonyHitsBeforeDeath"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.agonyHitsBeforeDeath.desc")))
                                .binding(3,
                                        () -> ModConfig.agonyHitsBeforeDeath,
                                        v -> ModConfig.agonyHitsBeforeDeath = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(1, 10)
                                        .step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.immersivecompanions.reviveDurationTicks"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.immersivecompanions.reviveDurationTicks.desc")))
                                .binding(100,
                                        () -> ModConfig.reviveDurationTicks,
                                        v -> ModConfig.reviveDurationTicks = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(20, 400)
                                        .step(10))
                                .build())
                        .build())
                .save(ModConfig::save)
                .build()
                .generateScreen(parent);
    }
}
