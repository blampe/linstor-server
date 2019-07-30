package com.linbit.linstor.storage.data.adapter.luks;

import com.linbit.linstor.api.interfaces.RscLayerDataApi;
import com.linbit.linstor.api.pojo.LuksRscPojo;
import com.linbit.linstor.api.pojo.LuksRscPojo.LuksVlmPojo;
import com.linbit.linstor.core.identifier.VolumeNumber;
import com.linbit.linstor.core.objects.Resource;
import com.linbit.linstor.dbdrivers.DatabaseException;
import com.linbit.linstor.dbdrivers.interfaces.LuksLayerDatabaseDriver;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.linstor.storage.AbsRscData;
import com.linbit.linstor.storage.interfaces.categories.resource.RscDfnLayerObject;
import com.linbit.linstor.storage.interfaces.categories.resource.RscLayerObject;
import com.linbit.linstor.storage.interfaces.layers.luks.LuksRscObject;
import com.linbit.linstor.storage.kinds.DeviceLayerKind;
import com.linbit.linstor.transaction.TransactionMgr;
import com.linbit.linstor.transaction.TransactionObjectFactory;

import javax.annotation.Nullable;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuksRscData extends AbsRscData<LuksVlmData> implements LuksRscObject
{
    private final LuksLayerDatabaseDriver luksDbDriver;

    public LuksRscData(
        int rscLayerIdRef,
        Resource rscRef,
        String rscNameSuffixRef,
        @Nullable RscLayerObject parentRef,
        Set<RscLayerObject> childrenRef,
        Map<VolumeNumber, LuksVlmData> vlmLayerObjectsRef,
        LuksLayerDatabaseDriver dbDriverRef,
        TransactionObjectFactory transObjFactory,
        Provider<TransactionMgr> transMgrProvider
    )
    {
        super(
            rscLayerIdRef,
            rscRef,
            parentRef,
            childrenRef,
            rscNameSuffixRef,
            dbDriverRef.getIdDriver(),
            vlmLayerObjectsRef,
            transObjFactory,
            transMgrProvider
        );
        luksDbDriver = dbDriverRef;
    }

    @Override
    public DeviceLayerKind getLayerKind()
    {
        return DeviceLayerKind.LUKS;
    }

    @Override
    public @Nullable RscDfnLayerObject getRscDfnLayerObject()
    {
        return null;
    }

    @Override
    protected void deleteVlmFromDatabase(LuksVlmData vlmRef) throws DatabaseException
    {
        luksDbDriver.delete(vlmRef);
    }

    @Override
    protected void deleteRscFromDatabase() throws DatabaseException
    {
        luksDbDriver.delete(this);
    }

    @Override
    public RscLayerDataApi asPojo(AccessContext accCtxRef) throws AccessDeniedException
    {
        List<LuksVlmPojo> vlmPojos = new ArrayList<>();
        for (LuksVlmData luksVlmData : vlmMap.values())
        {
            vlmPojos.add(luksVlmData.asPojo(accCtxRef));
        }
        return new LuksRscPojo(
            rscLayerId,
            getChildrenPojos(accCtxRef),
            rscSuffix,
            vlmPojos
        );
    }
}
