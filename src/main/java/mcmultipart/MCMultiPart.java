package mcmultipart;

import com.google.common.base.Throwables;
import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.microblock.MicroMaterial;
import mcmultipart.api.microblock.MicroblockType;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.MultipartCapabilityHelper;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.*;
import mcmultipart.block.BlockMultipartContainer;
import mcmultipart.block.TileMultipartContainer;
import mcmultipart.capability.CapabilityJoiner;
import mcmultipart.capability.CapabilityJoiner.JoinedItemHandler;
import mcmultipart.capability.CapabilityMultipartContainer;
import mcmultipart.capability.CapabilityMultipartTile;
import mcmultipart.client.MCMPClientProxy;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.network.MultipartNetworkHandler;
import mcmultipart.slot.SlotRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(MCMultiPart.MODID)
public class MCMultiPart {

    public static final String MODID = "mcmultipart", NAME = "MCMultiPart", VERSION = "%VERSION%";

    public static MCMPCommonProxy proxy;

    public static Logger log;

    public static Block multipart;

    public static ForgeRegistry<IPartSlot> slotRegistry;
    public static ForgeRegistry<MicroMaterial> microMaterialRegistry;
    public static ForgeRegistry<MicroblockType> microblockTypeRegistry;
    public static ObjectIntIdentityMap<IBlockState> stateMap;

    public static TileEntityType<TileMultipartContainer> TYPE;
    public static TileEntityType<TileMultipartContainer.Ticking> TICKING_TYPE;

    private List<IMCMPAddon> addons;

    private static List<Class> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        List<Class> classList = new ArrayList<>();
        Type type = Type.getType(annotation);
        for (ModFileScanData allScanDatum : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData allScanDatumAnnotation : allScanDatum.getAnnotations()) {
                if (Objects.equals(allScanDatumAnnotation.getAnnotationType(), type)) {
                    try {
                        classList.add(Class.forName(allScanDatumAnnotation.getMemberName()));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return classList;
    }
    
    public MCMultiPart() {
        MinecraftForge.EVENT_BUS.register(this);

        addons=getAnnotatedClasses(MCMPAddon.class).stream().map(aClass -> {
            try {
                return aClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).map(IMCMPAddon.class::cast).collect(Collectors.toList());
        proxy = DistExecutor.runForDist(() -> MCMPClientProxy::new, () -> MCMPCommonProxy::new);

        try {
            initAPI();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegistrySetup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Block.class,this::onBlockRegistryInit);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(IPartSlot.class, this::onSlotRegistryInit);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(TileEntityType.class, this::onTileRegistry);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(event -> {
            if(event instanceof FMLCommonSetupEvent) {
                stateMap = GameData.getBlockStateIDMap();

                CapabilityMultipartContainer.register();
                CapabilityMultipartTile.register();

                MultipartCapabilityHelper.registerCapabilityJoiner(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, JoinedItemHandler::join);

                MultipartNetworkHandler.init();

                MinecraftForge.EVENT_BUS.register(proxy);
                proxy.preInit();

                addons.forEach(a -> a.registerParts(MultipartRegistry.INSTANCE));

                MultipartRegistry.INSTANCE.computeBlocks();
                SlotRegistry.INSTANCE.computeAccess();
            }
            if(event instanceof FMLLoadCompleteEvent) {
                proxy.init();
            }
        });
    }

    public void onRegistrySetup(RegistryEvent.NewRegistry event) {
        slotRegistry = (ForgeRegistry<IPartSlot>) new RegistryBuilder<IPartSlot>()//
                .setName(new ResourceLocation(MODID, "slots"))//
                .setIDRange(0, Short.MAX_VALUE)//
                .setType(IPartSlot.class)//
                .create();

        microMaterialRegistry = (ForgeRegistry<MicroMaterial>) new RegistryBuilder<MicroMaterial>()//
                .setName(new ResourceLocation(MODID, "micro_material"))//
                .setIDRange(0, Short.MAX_VALUE)//
                .setType(MicroMaterial.class)//
                .create();

        microblockTypeRegistry = (ForgeRegistry<MicroblockType>) new RegistryBuilder<MicroblockType>()//
                .setName(new ResourceLocation(MODID, "micro_type"))//
                .setIDRange(0, Short.MAX_VALUE)
                .setType(MicroblockType.class)
                .create();
    }

    public void onSlotRegistryInit(RegistryEvent.Register<IPartSlot> event) {
        Stream.of(EnumFaceSlot.VALUES).map(EnumFaceSlot::getSlot).forEach(event.getRegistry()::register);
        Stream.of(EnumEdgeSlot.VALUES).map(EnumEdgeSlot::getSlot).forEach(event.getRegistry()::register);
//        Stream.of(EnumCornerSlot.VALUES).map(EnumCornerSlot::getSlot).forEach(event.getRegistry()::register);
//        Stream.of(EnumCenterSlot.VALUES).map(EnumCenterSlot::getSlot).forEach(event.getRegistry()::register);
    }

    public void onBlockRegistryInit(RegistryEvent.Register<Block> event) {
        multipart = new BlockMultipartContainer();
        event.getRegistry().register(multipart.setRegistryName("multipart"));
    }

    public void onTileRegistry(RegistryEvent.Register<TileEntityType<?>> event) {
        TYPE=(TileEntityType<TileMultipartContainer>)TileEntityType.Builder.create(TileMultipartContainer::new).build(null).setRegistryName(MODID + ":multipart.nonticking");
        TICKING_TYPE=(TileEntityType<TileMultipartContainer.Ticking>)TileEntityType.Builder.create(TileMultipartContainer.Ticking::new).build(null).setRegistryName(MODID + ":multipart.ticking");
        event.getRegistry().registerAll(TYPE, TICKING_TYPE);
    }

    public <T> void initAPI() throws Exception {
        ObfuscationReflectionHelper.setPrivateValue(MultipartHelper.class, null, //
                (BiFunction<World, BlockPos, IMultipartContainer>) TileMultipartContainer::createTileFromWorldInfo,
                "createTileFromWorldInfo");
        ObfuscationReflectionHelper.setPrivateValue(MultipartHelper.class, null, //
                (BiFunction<World, BlockPos, IMultipartContainer>) TileMultipartContainer::createTile, "createTile");
        ObfuscationReflectionHelper.setPrivateValue(MultipartHelper.class, null, //
                (Function<Block, IMultipart>) MultipartRegistry.INSTANCE::getPart, "getPart");

        ObfuscationReflectionHelper.setPrivateValue(MultipartCapabilityHelper.class, null, //
                (BiConsumer<Capability<T>, Function<List<T>, T>>) CapabilityJoiner::registerCapabilityJoiner,
                "registerJoiner");

        MethodHandle viewSide = MethodHandles
                .lookup().unreflect(SlotRegistry.class.getMethod("viewContainer", ISlottedContainer.class,
                        Function.class, Function.class, Object.class, boolean.class, EnumFacing.class))
                .bindTo(SlotRegistry.INSTANCE);
        MethodHandle viewEdge = MethodHandles.lookup()
                .unreflect(SlotRegistry.class.getMethod("viewContainer", ISlottedContainer.class, Function.class,
                        Function.class, Object.class, boolean.class, EnumEdgeSlot.class, EnumFacing.class))
                .bindTo(SlotRegistry.INSTANCE);
        ObfuscationReflectionHelper.setPrivateValue(SlotUtil.class, null, viewSide, "viewSide");
        ObfuscationReflectionHelper.setPrivateValue(SlotUtil.class, null, viewEdge, "viewEdge");
    }

}
