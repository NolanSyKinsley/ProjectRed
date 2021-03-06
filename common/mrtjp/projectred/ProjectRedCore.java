package mrtjp.projectred;

import codechicken.lib.packet.PacketCustom.CustomTinyPacketHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import mrtjp.projectred.core.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "ProjRed|Core", useMetadata = true)
@NetworkMod(clientSideRequired = true, serverSideRequired = true, tinyPacketHandler = CustomTinyPacketHandler.class)
public class ProjectRedCore
{
    /** Items **/
    public static ItemPart itemComponent;
    public static ItemDrawPlate itemDrawPlate;
    public static ItemScrewdriver itemScrewdriver;
    public static ItemWireDebugger itemWireDebugger;
    public static ItemDataCard itemDataCard;

    @Instance("ProjRed|Core")
    public static ProjectRedCore instance;

    @SidedProxy(clientSide = "mrtjp.projectred.core.CoreClientProxy", serverSide = "mrtjp.projectred.core.CoreProxy")
    public static IProxy proxy;

    public static CreativeTabs tabCore = new CreativeTabs("core") {
        @Override
        public ItemStack getIconItemStack()
        {
            return new ItemStack(ProjectRedCore.itemScrewdriver);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configurator.initConfig(event);
        proxy.preinit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(instance);
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postinit();
        TickRegistry.registerTickHandler(new PRVersionChecker(), Side.CLIENT);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandDebug());
    }
}
