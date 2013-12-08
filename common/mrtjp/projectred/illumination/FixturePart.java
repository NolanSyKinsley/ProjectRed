package mrtjp.projectred.illumination;

import mrtjp.projectred.ProjectRedIllumination;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import codechicken.lib.lighting.LazyLightMatrix;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.TFacePart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FixturePart extends BaseLightPart implements TFacePart {
    static Cuboid6 bounds[] = new Cuboid6[6];
    static Cuboid6 lightBounds[] = new Cuboid6[6];
    static
    {
        for (int i = 0; i < 6; i++) {
            Transformation t = Rotation.sideRotations[i].at(Vector3.center);
            bounds[i] = new Cuboid6(2/16D, 0, 2/16D, 14/16D, 17/32D, 14/16D).apply(t);
            lightBounds[i] = new Cuboid6(5/32D, 0, 5/32D, 27/32D, 17/32D, 27/32D).apply(t).expand(-0.001);
        }
    }

    @Override
    public String getType() {
        return "pr_fixture";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Vector3 pos, LazyLightMatrix olm, int pass) {
        if (pass == 0)
            RenderFixture.instance.renderFixture(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawBreaking(RenderBlocks r) {
        RenderFixture.instance.renderBreaking(this, r.overrideBlockTexture);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(isInverted ? ProjectRedIllumination.itemPartInvFixture : ProjectRedIllumination.itemPartFixture, 1, type);
    }

    @Override
    public Cuboid6 getBounds() {
        return bounds[side];
    }

    @Override
    public int getSlotMask() {
        return 1<<side;
    }

    @Override
    public int redstoneConductionMap() {
        return 0;
    }

    @Override
    public boolean solid(int arg0) {
        return false;
    }
}
