package com.sigmundgranaas.forgerofabric;

import com.google.common.base.Stopwatch;
import com.sigmundgranaas.forgero.Forgero;
import com.sigmundgranaas.forgero.ForgeroStateRegistry;
import com.sigmundgranaas.forgero.minecraft.common.item.DynamicItems;
import com.sigmundgranaas.forgero.minecraft.common.property.handler.PatternBreaking;
import com.sigmundgranaas.forgero.minecraft.common.property.handler.TaggedPatternBreaking;
import com.sigmundgranaas.forgero.property.AttributeType;
import com.sigmundgranaas.forgero.property.active.ActivePropertyRegistry;
import com.sigmundgranaas.forgero.property.active.VeinBreaking;
import com.sigmundgranaas.forgero.resource.PipelineBuilder;
import com.sigmundgranaas.forgero.settings.ForgeroSettings;
import com.sigmundgranaas.forgero.state.State;
import com.sigmundgranaas.forgero.type.Type;
import com.sigmundgranaas.forgerofabric.command.CommandRegistry;
import com.sigmundgranaas.forgerofabric.item.StateToItemConverter;
import com.sigmundgranaas.forgerofabric.loot.TreasureInjector;
import com.sigmundgranaas.forgerofabric.loot.function.GemLevelFunction;
import com.sigmundgranaas.forgerofabric.registry.RecipeRegistry;
import com.sigmundgranaas.forgerofabric.registry.RegistryHandler;
import com.sigmundgranaas.forgerofabric.resources.ARRPGenerator;
import com.sigmundgranaas.forgerofabric.resources.FabricPackFinder;
import com.sigmundgranaas.forgerofabric.resources.dynamic.PartToSchematicGenerator;
import com.sigmundgranaas.forgerofabric.resources.dynamic.RepairKitResourceGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.sigmundgranaas.forgero.identifier.Common.ELEMENT_SEPARATOR;
import static com.sigmundgranaas.forgero.minecraft.common.block.assemblystation.AssemblyStationBlock.*;
import static com.sigmundgranaas.forgero.minecraft.common.block.assemblystation.AssemblyStationScreenHandler.ASSEMBLY_STATION_SCREEN_HANDLER;


public class ForgeroInitializer implements ModInitializer {
    public static final String MOD_NAMESPACE = "forgero";
    public static final Logger LOGGER = LogManager.getLogger(ForgeroInitializer.MOD_NAMESPACE);

    @Override
    public void onInitialize() {
        ActivePropertyRegistry.register(new ActivePropertyRegistry.PropertyEntry(PatternBreaking.predicate, PatternBreaking.factory));
        ActivePropertyRegistry.register(new ActivePropertyRegistry.PropertyEntry(TaggedPatternBreaking.predicate, TaggedPatternBreaking.factory));
        ActivePropertyRegistry.register(new ActivePropertyRegistry.PropertyEntry(VeinBreaking.predicate, VeinBreaking.factory));

        var availableDependencies = FabricLoader.getInstance().getAllMods().stream().map(ModContainer::getMetadata).map(ModMetadata::getId).collect(Collectors.toSet());

        dataReloader(availableDependencies);
        Stopwatch timer = Stopwatch.createStarted();
        PipelineBuilder
                .builder()
                .register(ForgeroSettings.SETTINGS)
                .register(availableDependencies)
                .register(FabricPackFinder.supplier())
                .state(ForgeroStateRegistry.stateListener())
                .state(ForgeroStateRegistry.compositeListener())
                .createStates(ForgeroStateRegistry.createStateListener())
                .inflated(ForgeroStateRegistry.constructListener())
                .inflated(ForgeroStateRegistry.containerListener())
                .recipes(ForgeroStateRegistry.recipeListener())
                .build()
                .execute();
        Forgero.LOGGER.info("Total load time: " + timer.stop());
        var handler = RegistryHandler.HANDLER;
        handler.accept(this::registerBlocks);
        handler.accept(this::registerAARPRecipes);
        handler.accept(this::registerStates);
        handler.accept(this::registerLootFunctions);
        handler.accept(this::registerRecipes);
        handler.accept(DynamicItems::registerDynamicItems);
        handler.accept(this::registerTreasure);
        handler.accept(this::registerCommands);
        handler.run();
    }

    private void registerBlocks() {
        Registry.register(Registry.BLOCK, ASSEMBLY_STATION, ASSEMBLY_STATION_BLOCK);
        Registry.register(Registry.ITEM, ASSEMBLY_STATION, ASSEMBLY_STATION_ITEM);
        Registry.register(Registry.SCREEN_HANDLER, ASSEMBLY_STATION, ASSEMBLY_STATION_SCREEN_HANDLER);
    }

    private void registerAARPRecipes() {
        ARRPGenerator.register(new RepairKitResourceGenerator(ForgeroSettings.SETTINGS));
        ARRPGenerator.register(PartToSchematicGenerator::new);
        ARRPGenerator.generate();
    }

    private void dataReloader(Set<String> dependencies) {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                PipelineBuilder
                        .builder()
                        .register(ForgeroSettings.SETTINGS)
                        .register(dependencies)
                        .register(FabricPackFinder.supplier())
                        .state(ForgeroStateRegistry.stateListener())
                        .build()
                        .execute();
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier(ForgeroInitializer.MOD_NAMESPACE, "data");
            }
        });
    }

    private void registerLootFunctions() {
        Registry.register(Registry.LOOT_FUNCTION_TYPE, new Identifier("forgero:gem_level_function"), new LootFunctionType(new GemLevelFunction.Serializer()));
    }

    private void registerTreasure() {
        new TreasureInjector().registerLoot();
    }

    private void registerCommands() {
        new CommandRegistry().registerCommand();
    }

    private void registerStates() {
        var sortingMap = new HashMap<String, Integer>();

        ForgeroStateRegistry.STATES.all().stream().map(Supplier::get)
                .filter(state -> !state.test(Type.WEAPON) && !state.test(Type.TOOL)).forEach(state -> sortingMap.compute(materialName(state), (key, value) -> value == null || rarity(state) > value ? rarity(state) : value));

        ForgeroStateRegistry.CREATE_STATES.stream()
                .filter(state -> !Registry.ITEM.containsId(new Identifier(ForgeroStateRegistry.STATE_TO_CONTAINER.get(state.get().identifier()))))
                .filter(state -> !Registry.ITEM.containsId(new Identifier(state.get().identifier())))
                .sorted((element1, element2) -> compareStates(element1.get(), element2.get(), sortingMap))
                .forEach(state -> {
                    try {
                        var converter = StateToItemConverter.of(state);
                        Identifier identifier = converter.id();
                        var item = converter.convert();
                        Registry.register(Registry.ITEM, identifier, item);
                    } catch (InvalidIdentifierException e) {
                        LOGGER.error("invalid identifier: {}", state.get().identifier());
                        LOGGER.error(e);
                    }
                });
    }


    private int getOrderingFromState(Map<String, Integer> map, State state) {
        var name = materialName(state);
        int rarity = (int) state.stream().applyAttribute(AttributeType.RARITY);
        return map.getOrDefault(name, rarity);
    }

    private String materialName(State state) {
        var elements = state.name().split(ELEMENT_SEPARATOR);
        if (elements.length > 1) {
            return elements[0];
        } else {
            return state.name();
        }
    }

    private int compareStates(State element1, State element2, Map<String, Integer> map) {
        int elementOrdering = getOrderingFromState(map, element1) - getOrderingFromState(map, element2);
        int nameOrdering = materialName(element1).compareTo(materialName(element2));

        if (elementOrdering != 0) {
            return elementOrdering;
        } else if (nameOrdering != 0) {
            return nameOrdering;
        } else {
            return rarity(element1) - rarity(element2);
        }
    }

    private int rarity(State state) {
        return (int) state.stream().applyAttribute(AttributeType.RARITY);
    }

    private void registerRecipes() {
        RecipeRegistry.INSTANCE.registerRecipeSerializers();
    }


}
