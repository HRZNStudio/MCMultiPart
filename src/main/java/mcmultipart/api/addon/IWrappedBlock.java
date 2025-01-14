package mcmultipart.api.addon;

import mcmultipart.api.item.ItemBlockMultipart.IBlockPlacementInfo;
import mcmultipart.api.item.ItemBlockMultipart.IBlockPlacementLogic;
import mcmultipart.api.item.ItemBlockMultipart.IPartPlacementLogic;

public interface IWrappedBlock {

    IWrappedBlock setBlockPlacementLogic(IBlockPlacementLogic logic);

    IWrappedBlock setPartPlacementLogic(IPartPlacementLogic logic);

    IWrappedBlock setPlacementInfo(IBlockPlacementInfo info);

}
