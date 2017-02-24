package cofh.thermalexpansion.render;

import codechicken.lib.model.blockbakery.ILayeredBlockBakery;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.buffer.BakingVertexBuffer;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.lib.render.RenderHelper;
import cofh.thermalexpansion.block.storage.ItemBlockTank;
import cofh.thermalexpansion.block.storage.TileTank;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.init.TETextures;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class RenderTank implements ILayeredBlockBakery {

	public static final RenderTank INSTANCE = new RenderTank();

	public static final int RENDER_LEVELS = 128;

	private static CCModel[] modelFluid = new CCModel[RENDER_LEVELS];
	private static CCModel modelFrame = CCModel.quadModel(48);

	static {
		generateModels();
		generateFluidModels();
	}

	private static void generateModels() {

		Cuboid6 box = new Cuboid6(0.125, 0, 0.125, 0.875, 1, 0.875);
		double inset = 0.0625;
		modelFrame = CCModel.quadModel(48).generateBlock(0, box);
		CCModel.generateBackface(modelFrame, 0, modelFrame, 24, 24);
		modelFrame.computeNormals();

		for (int i = 24; i < 48; i++) {
			modelFrame.verts[i].vec.add(modelFrame.normals()[i].copy().multiply(inset));
		}
		modelFrame.shrinkUVs(RenderHelper.RENDER_OFFSET);
	}

	private static void generateFluidModels() {

		double minXZ = 0.1875 - RenderHelper.RENDER_OFFSET;
		double maxXZ = 0.8125 + RenderHelper.RENDER_OFFSET;
		double minY = 0.0625 - RenderHelper.RENDER_OFFSET;
		double maxY = 1 - minY;
		double increment = (maxY - minY) / RENDER_LEVELS;

		for (int i = 1; i < RENDER_LEVELS + 1; i++) {
			double yLevel = minY + increment * i;
			modelFluid[i - 1] = CCModel.quadModel(24).generateBlock(0, minXZ, minY, minXZ, maxXZ, yLevel, maxXZ).computeNormals();
		}
	}

	/* RENDER */
	protected void renderFrame(CCRenderState ccrs, int level, int mode) {

		modelFrame.render(ccrs, 0, 4, new IconTransformation(TETextures.TANK_BOTTOM[mode][level])); // Bottom
		modelFrame.render(ccrs, 24, 28, new IconTransformation(TETextures.TANK_TOP[mode][level])); // Bottom inside
		modelFrame.render(ccrs, 4, 8, new IconTransformation(TETextures.TANK_TOP[mode][level])); // Top
		modelFrame.render(ccrs, 28, 32, new IconTransformation(TETextures.TANK_BOTTOM[mode][level])); // Top Inside

		for (int i = 8; i < 24; i += 4) {
			modelFrame.render(ccrs, i, i + 4, new IconTransformation(TETextures.TANK_SIDE[mode][level])); // Sides
		}
		for (int i = 32; i < 48; i += 4) {
			modelFrame.render(ccrs, i, i + 4, new IconTransformation(TETextures.TANK_SIDE[mode][level])); // Edges
		}
	}

	protected void renderFluid(CCRenderState ccrs, int metadata, FluidStack stack) {

		if (stack == null || stack.amount <= 0) {
			return;
		}
		Fluid fluid = stack.getFluid();

		ccrs.setFluidColour(stack);
		TextureAtlasSprite fluidTex = RenderHelper.getFluidTexture(stack);
		int fluidLevel = RENDER_LEVELS - 1;

		if (fluid.isGaseous(stack)) {
			ccrs.alphaOverride = 32 + 192 * stack.amount / TileTank.CAPACITY[metadata];
		} else {
			fluidLevel = (int) Math.min(RENDER_LEVELS - 1, (long) stack.amount * RENDER_LEVELS / TileTank.CAPACITY[metadata]);
		}
		modelFluid[fluidLevel].render(ccrs, new IconTransformation(fluidTex));
	}

	/* ICustomBlockBakery */
	@Override
	public IExtendedBlockState handleState(IExtendedBlockState state, TileEntity tileEntity) {

		TileTank tank = ((TileTank) tileEntity);

		// state = state.withProperty(TEProps.CREATIVE, false);// TODO
		state = state.withProperty(TEProps.LEVEL, tank.getLevel());
		state = state.withProperty(TEProps.ACTIVE, tank.enableAutoOutput);
		state = state.withProperty(TEProps.FLUID, tank.getTankFluid());

		return state;
	}

	@Override
	public List<BakedQuad> bakeItemQuads(EnumFacing face, ItemStack stack) {

		if (face == null) {
			BakingVertexBuffer buffer = BakingVertexBuffer.create();
			buffer.begin(7, DefaultVertexFormats.ITEM);
			CCRenderState ccrs = CCRenderState.instance();
			ccrs.reset();
			ccrs.bind(buffer);

			int level = ItemBlockTank.getLevel(stack);
			FluidStack fluid = null;
			if (stack.getTagCompound() != null) {
				fluid = FluidStack.loadFluidStackFromNBT(stack.getTagCompound().getCompoundTag("Fluid"));
			}
			renderFrame(ccrs, level, 0);
			renderFluid(ccrs, level, fluid);

			buffer.finishDrawing();
			return buffer.bake();
		}
		return new ArrayList<BakedQuad>();
	}

	/* ILayeredBlockBakery */
	@Override
	public List<BakedQuad> bakeLayerFace(EnumFacing face, BlockRenderLayer layer, IExtendedBlockState state) {

		if (face == null) {
			// boolean creative = state.getValue(TEProps.CREATIVE);
			int level = state.getValue(TEProps.LEVEL);
			int mode = state.getValue(TEProps.ACTIVE) ? 1 : 0;
			FluidStack fluidStack = state.getValue(TEProps.FLUID);

			BakingVertexBuffer buffer = BakingVertexBuffer.create();
			buffer.begin(7, DefaultVertexFormats.ITEM);
			CCRenderState ccrs = CCRenderState.instance();
			ccrs.reset();
			ccrs.bind(buffer);

			if (layer == BlockRenderLayer.CUTOUT) {
				renderFrame(ccrs, level, mode);
			} else {
				renderFluid(ccrs, level, fluidStack);
			}
			buffer.finishDrawing();
			return buffer.bake();
		}
		return new ArrayList<BakedQuad>();
	}

}
