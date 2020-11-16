package com.linbit.linstor.layer.storage.utils;

import com.linbit.linstor.core.identifier.SharedStorPoolName;
import com.linbit.linstor.core.objects.AbsResource;
import com.linbit.linstor.core.objects.Resource;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.linstor.storage.interfaces.categories.resource.AbsRscLayerObject;
import com.linbit.linstor.storage.interfaces.categories.resource.VlmProviderObject;
import com.linbit.linstor.storage.kinds.DeviceLayerKind;
import com.linbit.linstor.utils.layer.LayerRscUtils;

import java.util.Iterator;
import java.util.Set;

public class SharedStorageUtils
{
    public static <RSC extends AbsResource<RSC>> boolean isNeededBySharedResource(
        AccessContext accCtx,
        VlmProviderObject<RSC> localVlmData
    )
        throws AccessDeniedException
    {
        boolean neededBySharedResource = false;

        AbsRscLayerObject<RSC> localRscStorData = localVlmData.getRscLayerObject();
        SharedStorPoolName localSharedStorPoolName = localVlmData.getStorPool().getSharedStorPoolName();
        RSC localRsc = localVlmData.getVolume().getAbsResource();
        Iterator<Resource> rscIt = localRsc.getResourceDefinition().iterateResource(accCtx);

        while (rscIt.hasNext() && !neededBySharedResource)
        {
            Resource otherRsc = rscIt.next();
            if (otherRsc != localRsc)
            {
                Set<AbsRscLayerObject<Resource>> otherRscStorDataSet = LayerRscUtils.getRscDataByProvider(
                    otherRsc.getLayerData(accCtx),
                    DeviceLayerKind.STORAGE
                );
                for (AbsRscLayerObject<Resource> otherRscStorData : otherRscStorDataSet)
                {
                    if (localRscStorData.getSuffixedResourceName().equals(otherRscStorData.getSuffixedResourceName()))
                    {
                        VlmProviderObject<Resource> otherVlmData = otherRscStorData
                            .getVlmProviderObject(localVlmData.getVlmNr());
                        SharedStorPoolName otherSharedStorPoolName = otherVlmData.getStorPool().getSharedStorPoolName();

                        if (localSharedStorPoolName.equals(otherSharedStorPoolName))
                        {
                            neededBySharedResource = true;
                            break;
                        }
                    }
                }
            }
        }

        return neededBySharedResource;
    }
}