package cofh.thermalexpansion.plugins.jei.dynamos;

import cofh.api.energy.IEnergyContainerItem;
import cofh.lib.inventory.ComparableItemStack;
import cofh.lib.inventory.ComparableItemStackNBT;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.dynamo.*;
import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.util.fuels.CoolantManager;
import cofh.thermalfoundation.init.TFFluids;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.UniversalBucket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class DynamoFuelCategories {

	public static void initialize(IModRegistry registry) {

		IJeiHelpers jeiHelpers = registry.getJeiHelpers();
		IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

		registry.addRecipeHandlers(new DynamoFuelHandler());

		initSteam(registry, guiHelper);
		initMagmatic(registry, guiHelper);
		initCompression(registry, guiHelper);
		initReactantFluid(registry, guiHelper);
		initReactantSolid(registry, guiHelper);
		initEnervation(registry, guiHelper);
		initNumismatic(registry, guiHelper);

		initCoolantCategory(registry, guiHelper);
	}

	protected static void initSteam(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelItem steamCategory = new DynamoFuelCategory.FuelItem(guiHelper, RecipeUidsTE.DYNAMO_STEAM, StringHelper.localize("tile.thermalexpansion.dynamo.steam.name"));
		registry.addRecipeCategories(steamCategory);
		Set<ComparableItemStack> overriddenFuelStacks = TileDynamoSteam.getOverriddenFuelStacks();
		ArrayList<DynamoFuelWrapper<ItemStack>> steamFuels = new ArrayList<>();
		for (ComparableItemStack comparableItemStack : overriddenFuelStacks) {
			ItemStack itemStack = comparableItemStack.toItemStack();
			int energyValue = TileDynamoSteam.getEnergyValue(itemStack);
			if (energyValue > 0) {
				steamFuels.add(new DynamoFuelWrapper<>(itemStack, steamCategory, energyValue));
			}
		}

		for (ItemStack itemStack : registry.getIngredientRegistry().getFuels()) {
			if (overriddenFuelStacks.contains(new ComparableItemStack(itemStack))) {
				continue;
			}
			int energyValue = TileDynamoSteam.getEnergyValue(itemStack);
			if (energyValue > 0) {
				steamFuels.add(new DynamoFuelWrapper<>(itemStack, steamCategory, energyValue));
			}
		}
		registry.addRecipes(steamFuels);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoSteam, RecipeUidsTE.DYNAMO_STEAM);
	}

	protected static void initMagmatic(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelFluid magmaticCategory = new DynamoFuelCategory.FuelFluid(guiHelper, RecipeUidsTE.DYNAMO_MAGMATIC, StringHelper.localize("tile.thermalexpansion.dynamo.magmatic.name"));
		registry.addRecipeCategories(magmaticCategory);
		addFluidFuels(registry, magmaticCategory, TileDynamoMagmatic.getMagmaticFuelFluids(), TileDynamoMagmatic::getFuelEnergy);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoMagmatic, RecipeUidsTE.DYNAMO_MAGMATIC);
	}

	protected static void initCompression(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelFluid compressionCategory = new DynamoFuelCategory.FuelFluid(guiHelper, RecipeUidsTE.DYNAMO_COMPRESSION, StringHelper.localize("tile.thermalexpansion.dynamo.compression.name"));
		registry.addRecipeCategories(compressionCategory);
		addFluidFuels(registry, compressionCategory, TileDynamoCompression.getCompressionFuelFluids(), TileDynamoCompression::getFuelEnergy);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoCompression, RecipeUidsTE.DYNAMO_COMPRESSION);
	}

	protected static void initReactantFluid(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelFluid reactantCategory = new DynamoFuelCategory.FuelFluid(guiHelper, RecipeUidsTE.DYNAMO_REACTANT_FLUID, StringHelper.localize("gui.thermalexpansion.jei.category.dynamo.reactant.fluid"));
		registry.addRecipeCategories(reactantCategory);
		addFluidFuels(registry, reactantCategory, TileDynamoReactant.getReactantFuelFluids(), TileDynamoReactant::getFuelEnergy);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoReactant, RecipeUidsTE.DYNAMO_REACTANT_FLUID);
	}

	private static void initReactantSolid(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelItem reactantCategory = new DynamoFuelCategory.FuelItem(guiHelper, RecipeUidsTE.DYNAMO_REACTANT_SOLID, StringHelper.localize("gui.thermalexpansion.jei.category.dynamo.reactant.solid"));
		registry.addRecipeCategories(reactantCategory);
		addItemFuels(registry, reactantCategory, TileDynamoReactant.getReactantsStacks(), TileDynamoReactant::getReactantEnergy);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoReactant, RecipeUidsTE.DYNAMO_REACTANT_SOLID);
	}

	private static void initEnervation(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelItem enervationCategory = new DynamoFuelCategory.FuelItem(guiHelper, RecipeUidsTE.DYNAMO_ENERVATION, StringHelper.localize("tile.thermalexpansion.dynamo.enervation.name"));
		registry.addRecipeCategories(enervationCategory);
		addItemFuels(registry, enervationCategory, TileDynamoEnervation.getSpecialStacks(), TileDynamoEnervation::getEnergyValue);
		ArrayList<Object> recipes = new ArrayList<>();

		for (Item item : Item.REGISTRY) {
			if (item instanceof IEnergyContainerItem) {
				DynamoFuelWrapper<ItemStack> dynamoFuelWrapper = null;
				try {
					HashSet<ComparableItemStack> processedStacks = new HashSet<>();

					List<ItemStack> list = new ArrayList<>();
					item.getSubItems(item, item.getCreativeTab(), list);
					for (ItemStack stack : list) {
						IEnergyContainerItem energyContainerItem = (IEnergyContainerItem) item;
						int maxEnergyStored = energyContainerItem.getMaxEnergyStored(stack);
						if (maxEnergyStored != 0) {
							energyContainerItem.receiveEnergy(stack, Integer.MAX_VALUE, false);

							if (!processedStacks.add(new ComparableItemStackNBT(stack))) {
								continue;
							}

							int energyValue = TileDynamoEnervation.getEnergyValue(stack);
							if (energyValue > 0) {
								dynamoFuelWrapper = new DynamoFuelWrapper<>(stack, enervationCategory, energyValue);
							}
						}
					}

				} catch (Exception err) {
					err.printStackTrace();
					continue;
				}
				if (dynamoFuelWrapper != null) {
					recipes.add(dynamoFuelWrapper);
				}
			}
		}
		registry.addRecipes(recipes);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoEnervation, RecipeUidsTE.DYNAMO_ENERVATION);
	}

	protected static void initNumismatic(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelItem numismaticCategory = new DynamoFuelCategory.FuelItem(guiHelper, RecipeUidsTE.DYNAMO_NUMISMATIC, StringHelper.localize("tile.thermalexpansion.dynamo.numismatic.name"));
		registry.addRecipeCategories(numismaticCategory);
		addItemFuels(registry, numismaticCategory, TileDynamoNumismatic.getFuelStacks(), TileDynamoNumismatic::getEnergyValue);
		registry.addRecipeCategoryCraftingItem(BlockDynamo.dynamoNumismatic, RecipeUidsTE.DYNAMO_NUMISMATIC);
	}

	protected static void initCoolantCategory(IModRegistry registry, IGuiHelper guiHelper) {

		DynamoFuelCategory.FuelFluid coolantCategory = new DynamoFuelCategory.FuelFluid(guiHelper, RecipeUidsTE.COOLANT, StringHelper.localize("info.thermalexpansion.coolant")) {
			@Override
			protected IDrawable createAnimatedFlame(IGuiHelper guiHelper) {

				return null;
			}
		};
		registry.addRecipeCategories(coolantCategory);
		registry.addRecipes(CoolantManager.getCoolantFluids().stream().map(fluid -> {
			FluidStack fluidStack = new FluidStack(fluid, 1000);
			return new DynamoFuelWrapper<>(fluidStack, coolantCategory, CoolantManager.getCoolantRF(fluidStack), "TC");
		}).collect(Collectors.toList()));

		registry.addRecipeCategoryCraftingItem(UniversalBucket.getFilledBucket(ForgeModContainer.getInstance().universalBucket, TFFluids.fluidCryotheum), RecipeUidsTE.COOLANT);
	}

	/* HELPERS */
	private static void addFluidFuels(IModRegistry registry, DynamoFuelCategory.FuelFluid category, Set<Fluid> fluidSet, ToIntFunction<FluidStack> getFuelEnergy) {

		registry.addRecipes(fluidSet.stream().map(fluid -> {
			FluidStack fluidStack = new FluidStack(fluid, 1000);
			return new DynamoFuelWrapper<>(fluidStack, category, getFuelEnergy.applyAsInt(fluidStack));
		}).collect(Collectors.toList()));
	}

	private static void addItemFuels(IModRegistry registry, DynamoFuelCategory.FuelItem category, Set<ComparableItemStack> stackSet, ToIntFunction<ItemStack> getFuelEnergy) {

		registry.addRecipes(stackSet.stream().map(comparableItemStack -> {
			ItemStack itemStack = comparableItemStack.toItemStack();
			return new DynamoFuelWrapper<>(itemStack, category, getFuelEnergy.applyAsInt(itemStack));
		}).collect(Collectors.toList()));
	}

}
