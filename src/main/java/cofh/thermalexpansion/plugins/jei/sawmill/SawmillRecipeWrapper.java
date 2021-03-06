package cofh.thermalexpansion.plugins.jei.sawmill;

import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.machine.TilePulverizer;
import cofh.thermalexpansion.block.machine.TileSawmill;
import cofh.thermalexpansion.plugins.jei.Drawables;
import cofh.thermalexpansion.plugins.jei.JEIPluginTE;
import cofh.thermalexpansion.plugins.jei.RecipeUidsTE;
import cofh.thermalexpansion.util.crafting.SawmillManager.ComparableItemStackSawmill;
import cofh.thermalexpansion.util.crafting.SawmillManager.RecipeSawmill;
import cofh.thermalexpansion.util.crafting.TapperManager;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableAnimated.StartDirection;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SawmillRecipeWrapper extends BlankRecipeWrapper {

	/* Recipe */
	final List<List<ItemStack>> inputs;
	final List<ItemStack> outputs;
	final List<FluidStack> outputFluids;

	final int energy;
	final int chance;
	final String uId;

	/* Animation */
	final IDrawableAnimated fluid;
	final IDrawableAnimated progress;
	final IDrawableAnimated speed;
	final IDrawableAnimated energyMeter;

	public SawmillRecipeWrapper(IGuiHelper guiHelper, RecipeSawmill recipe) {

		this(guiHelper, recipe, RecipeUidsTE.SAWMILL);
	}

	public SawmillRecipeWrapper(IGuiHelper guiHelper, RecipeSawmill recipe, String uIdIn) {

		uId = uIdIn;

		List<ItemStack> recipeInputs = new ArrayList<>();
		List<ItemStack> recipeOutputs = new ArrayList<>();
		List<FluidStack> recipeOutputFluids = new ArrayList<>();

		if (ComparableItemStackSawmill.getOreID(recipe.getInput()) != -1) {
			for (ItemStack ore : OreDictionary.getOres(ItemHelper.getOreName(recipe.getInput()))) {
				recipeInputs.add(ItemHelper.cloneStack(ore, recipe.getInput().stackSize));
			}
		} else {
			recipeInputs.add(recipe.getInput());
		}
		recipeOutputs.add(recipe.getPrimaryOutput());

		if (recipe.getSecondaryOutput() != null) {
			recipeOutputs.add(recipe.getSecondaryOutput());
		}
		if (uId.equals(RecipeUidsTE.SAWMILL_TAPPER)) {
			recipeOutputFluids.add(TapperManager.getFluid(recipe.getInput()));
			outputFluids = recipeOutputFluids;
			energy = recipe.getEnergy() * 3 / 2;
		} else {
			outputFluids = Collections.emptyList();
			energy = recipe.getEnergy();
		}
		inputs = Collections.singletonList(recipeInputs);
		outputs = recipeOutputs;

		chance = recipe.getSecondaryOutputChance();

		IDrawableStatic fluidDrawable = Drawables.getDrawables(guiHelper).getProgress(1);
		IDrawableStatic progressDrawable = Drawables.getDrawables(guiHelper).getProgressFill(uId.equals(RecipeUidsTE.SAWMILL_TAPPER) ? 1 : 0);
		IDrawableStatic speedDrawable = Drawables.getDrawables(guiHelper).getSpeedFill(3);
		IDrawableStatic energyDrawable = Drawables.getDrawables(guiHelper).getEnergyFill();

		fluid = guiHelper.createAnimatedDrawable(fluidDrawable, energy / TileSawmill.basePower, StartDirection.LEFT, true);
		progress = guiHelper.createAnimatedDrawable(progressDrawable, energy / TilePulverizer.basePower, StartDirection.LEFT, false);
		speed = guiHelper.createAnimatedDrawable(speedDrawable, 1000, StartDirection.TOP, true);
		energyMeter = guiHelper.createAnimatedDrawable(energyDrawable, 1000, StartDirection.TOP, true);
	}

	public String getUid() {

		return uId;
	}

	@Override
	public void getIngredients(IIngredients ingredients) {

		ingredients.setInputLists(ItemStack.class, inputs);
		ingredients.setOutputs(ItemStack.class, outputs);
		ingredients.setOutputs(FluidStack.class, outputFluids);
	}

	@Override
	public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {

		if (uId.equals(RecipeUidsTE.SAWMILL_TAPPER)) {
			JEIPluginTE.drawFluid(69, 23, outputFluids.get(0), 24, 16);
			fluid.draw(minecraft, 69, 23);
		}
		progress.draw(minecraft, 69, 23);
		speed.draw(minecraft, 43, 33);
		energyMeter.draw(minecraft, 2, 8);
	}

	@Nullable
	public List<String> getTooltipStrings(int mouseX, int mouseY) {

		List<String> tooltip = new ArrayList<>();

		if (mouseX > 2 && mouseX < 15 && mouseY > 8 && mouseY < 49) {
			tooltip.add(StringHelper.localize("info.cofh.energy") + ": " + energy + " RF");
		}
		return tooltip;
	}

}
