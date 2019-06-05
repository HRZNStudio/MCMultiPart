package mcmultipart.util;

import mcmultipart.multipart.PartInfo;
import mcmultipart.multipart.PartInfo.ClientInfo;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.List;

public class PropertyClientInfo implements IUnlistedProperty<List<PartInfo.ClientInfo>> {

    @Override
    public String getName() {
        return "clientinfo";
    }

    @Override
    public boolean isValid(List<ClientInfo> value) {
        return value != null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<List<ClientInfo>> getType() {
        return (Class) List.class;
    }

    @Override
    public String valueToString(List<ClientInfo> value) {
        return value.toString();
    }

}
