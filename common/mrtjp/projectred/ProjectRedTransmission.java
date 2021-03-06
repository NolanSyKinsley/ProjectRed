package mrtjp.projectred;

import mrtjp.projectred.api.ProjectRedAPI;
import mrtjp.projectred.core.IProxy;
import mrtjp.projectred.transmission.APIImpl_Transmission;
import mrtjp.projectred.transmission.ItemPartFramedWire;
import mrtjp.projectred.transmission.ItemPartWire;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "ProjRed|Transmission", useMetadata = true)
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class ProjectRedTransmission
{
    public ProjectRedTransmission()
    {
        ProjectRedAPI.transmissionAPI = new APIImpl_Transmission();
    }
    
    /** Multipart items **/
    public static ItemPartWire itemPartWire;
    public static ItemPartFramedWire itemPartFramedWire;

    @Instance("ProjRed|Transmission")
    public static ProjectRedTransmission instance;

    @SidedProxy(clientSide = "mrtjp.projectred.transmission.TransmissionClientProxy", serverSide = "mrtjp.projectred.transmission.TransmissionProxy")
    public static IProxy proxy;

    public static CreativeTabs tabTransmission = new CreativeTabs("trans") {
        @Override
        public ItemStack getIconItemStack()
        {
            return new ItemStack(ProjectRedTransmission.itemPartWire);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
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
    }
}
