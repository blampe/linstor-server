package com.linbit.linstor.core.apicallhandler.controller;

import com.linbit.ImplementationError;
import com.linbit.linstor.LinStorDataAlreadyExistsException;
import com.linbit.linstor.LinStorException;
import com.linbit.linstor.LinstorParsingUtils;
import com.linbit.linstor.PriorityProps;
import com.linbit.linstor.Resource;
import com.linbit.linstor.StorPool;
import com.linbit.linstor.VolumeData;
import com.linbit.linstor.VolumeDataFactory;
import com.linbit.linstor.VolumeDefinition;
import com.linbit.linstor.annotation.ApiContext;
import com.linbit.linstor.annotation.PeerContext;
import com.linbit.linstor.api.ApiCallRc;
import com.linbit.linstor.api.ApiCallRcImpl;
import com.linbit.linstor.api.ApiCallRcWith;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.core.ConfigModule;
import com.linbit.linstor.core.apicallhandler.response.ApiAccessDeniedException;
import com.linbit.linstor.core.apicallhandler.response.ApiRcException;
import com.linbit.linstor.core.apicallhandler.response.ApiSQLException;
import com.linbit.linstor.propscon.InvalidKeyException;
import com.linbit.linstor.propscon.Props;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.sql.SQLException;

import static com.linbit.linstor.api.ApiConsts.FAIL_INVLD_STOR_POOL_NAME;
import static com.linbit.linstor.api.ApiConsts.FAIL_NOT_FOUND_DFLT_STOR_POOL;
import static com.linbit.linstor.api.ApiConsts.KEY_STOR_POOL_NAME;
import static com.linbit.linstor.api.ApiConsts.MASK_STOR_POOL;
import static com.linbit.linstor.api.ApiConsts.MASK_WARN;
import static com.linbit.linstor.core.apicallhandler.controller.CtrlVlmApiCallHandler.getVlmDescriptionInline;

@Singleton
class CtrlVlmCrtApiHelper
{
    private final AccessContext apiCtx;
    private final CtrlPropsHelper ctrlPropsHelper;
    private final VolumeDataFactory volumeDataFactory;
    private final Provider<AccessContext> peerAccCtx;
    private final String defaultStorPoolName;

    @Inject
    CtrlVlmCrtApiHelper(
        @ApiContext AccessContext apiCtxRef,
        CtrlPropsHelper ctrlPropsHelperRef,
        VolumeDataFactory volumeDataFactoryRef,
        @PeerContext Provider<AccessContext> peerAccCtxRef,
        @Named(ConfigModule.CONFIG_STOR_POOL_NAME) String defaultStorPoolNameRef
    )
    {
        apiCtx = apiCtxRef;
        ctrlPropsHelper = ctrlPropsHelperRef;
        volumeDataFactory = volumeDataFactoryRef;
        peerAccCtx = peerAccCtxRef;
        defaultStorPoolName = defaultStorPoolNameRef;
    }

    public ApiCallRcWith<VolumeData> createVolumeResolvingStorPool(
        Resource rsc,
        VolumeDefinition vlmDfn
    )
    {
        return createVolumeResolvingStorPool(rsc, vlmDfn, null, null);
    }

    public ApiCallRcWith<VolumeData> createVolumeResolvingStorPool(
        Resource rsc,
        VolumeDefinition vlmDfn,
        String blockDevice,
        String metaDisk
    )
    {
        ApiCallRcImpl apiCallRc = new ApiCallRcImpl();
        return new ApiCallRcWith<>(
            apiCallRc,
            createVolume(
                rsc,
                vlmDfn,
                resolveStorPool(rsc, vlmDfn, isDiskless(rsc)).extractApiCallRc(apiCallRc),
                blockDevice,
                metaDisk
            )
        );
    }

    public VolumeData createVolume(
        Resource rsc,
        VolumeDefinition vlmDfn,
        StorPool storPool,
        String blockDevice,
        String metaDisk
    )
    {
        VolumeData vlm;
        try
        {
            vlm = volumeDataFactory.create(
                peerAccCtx.get(),
                rsc,
                vlmDfn,
                storPool,
                blockDevice,
                metaDisk,
                null // flags
            );
        }
        catch (AccessDeniedException accDeniedExc)
        {
            throw new ApiAccessDeniedException(
                accDeniedExc,
                "register " + getVlmDescriptionInline(rsc, vlmDfn),
                ApiConsts.FAIL_ACC_DENIED_VLM
            );
        }
        catch (LinStorDataAlreadyExistsException dataAlreadyExistsExc)
        {
            throw new ApiRcException(ApiCallRcImpl.simpleEntry(
                ApiConsts.FAIL_EXISTS_VLM,
                "The " + getVlmDescriptionInline(rsc, vlmDfn) + " already exists"
            ), dataAlreadyExistsExc);
        }
        catch (SQLException sqlExc)
        {
            throw new ApiSQLException(sqlExc);
        }
        return vlm;
    }

    /**
     * Resolves the correct storage pool and also handles error/warnings in diskless modes.
     */
    public ApiCallRcWith<StorPool> resolveStorPool(
        Resource rsc,
        VolumeDefinition vlmDfn,
        boolean isRscDiskless
    )
    {
        ApiCallRcImpl responses = new ApiCallRcImpl();

        StorPool storPool;
        try
        {
            Props rscProps = ctrlPropsHelper.getProps(rsc);
            Props vlmDfnProps = ctrlPropsHelper.getProps(vlmDfn);
            Props rscDfnProps = ctrlPropsHelper.getProps(rsc.getDefinition());
            Props nodeProps = ctrlPropsHelper.getProps(rsc.getAssignedNode());

            PriorityProps vlmPrioProps = new PriorityProps(
                rscProps, vlmDfnProps, rscDfnProps, nodeProps
            );

            String storPoolNameStr = vlmPrioProps.getProp(KEY_STOR_POOL_NAME);
            if (isRscDiskless)
            {
                if (storPoolNameStr == null || "".equals(storPoolNameStr))
                {
                    // If the resource was marked as diskless then there should be a resource property identifying the
                    // diskless pool.
                    storPool = null;
                }
                else
                {
                    storPool = rsc.getAssignedNode().getStorPool(
                        apiCtx,
                        LinstorParsingUtils.asStorPoolName(storPoolNameStr)
                    );
                }

                checkBackingDiskWithDiskless(rsc, storPool);
            }
            else
            {
                if (storPoolNameStr == null || "".equals(storPoolNameStr))
                {
                    storPoolNameStr = defaultStorPoolName;
                }
                storPool = rsc.getAssignedNode().getStorPool(
                    apiCtx,
                    LinstorParsingUtils.asStorPoolName(storPoolNameStr)
                );

                responses.addEntries(warnAndFlagDiskless(rsc, storPool));
            }

            checkStorPoolLoaded(rsc, storPool, storPoolNameStr, vlmDfn);
        }
        catch (InvalidKeyException | AccessDeniedException exc)
        {
            throw new ImplementationError(exc);
        }

        return new ApiCallRcWith<>(responses, storPool);
    }

    public boolean isDiskless(Resource rsc)
    {
        boolean isDiskless;
        try
        {
            isDiskless = rsc.getStateFlags().isSet(apiCtx, Resource.RscFlags.DISKLESS);
        }
        catch (AccessDeniedException implError)
        {
            throw new ImplementationError(implError);
        }
        return isDiskless;
    }

    private void checkStorPoolLoaded(
        final Resource rsc,
        StorPool storPool,
        String storPoolNameStr,
        final VolumeDefinition vlmDfn
    )
    {
        if (storPool == null)
        {
            throw new ApiRcException(ApiCallRcImpl
                .entryBuilder(FAIL_NOT_FOUND_DFLT_STOR_POOL, "The storage pool '" + storPoolNameStr + "' " +
                    "for resource '" + rsc.getDefinition().getName().displayValue + "' " +
                    "for volume number '" + vlmDfn.getVolumeNumber().value + "' " +
                    "is not deployed on node '" + rsc.getAssignedNode().getName().displayValue + "'.")
                .setDetails("The resource which should be deployed had at least one volume definition " +
                    "(volume number '" + vlmDfn.getVolumeNumber().value + "') which LinStor " +
                    "tried to automatically create. " +
                    "The storage pool name for this new volume was looked for in order in " +
                    "the properties of the resource, volume definition, resource definition and node, " +
                    "and finally in a system wide default storage pool name defined by " +
                    "the LinStor controller.")
                .build(),
                new LinStorException("Dependency not found")
            );
        }
    }

    private void checkBackingDiskWithDiskless(final Resource rsc, final StorPool storPool)
    {
        if (storPool != null && storPool.getDriverKind().hasBackingStorage())
        {
            throw new ApiRcException(ApiCallRcImpl
                .entryBuilder(FAIL_INVLD_STOR_POOL_NAME, "Storage pool with backing disk not allowed with diskless resource.")
                .setCause(String.format("Resource '%s' flagged as diskless, but a storage pool '%s' " +
                        "with backing disk was specified.",
                    rsc.getDefinition().getName().displayValue,
                    storPool.getName().displayValue))
                .setCorrection("Use a storage pool with a diskless driver or remove the diskless flag.")
                .build(),
                new LinStorException("Incorrect storage pool used.")
            );
        }
    }

    private ApiCallRc warnAndFlagDiskless(Resource rsc, final StorPool storPool)
    {
        ApiCallRcImpl responses = new ApiCallRcImpl();

        if (storPool != null && !storPool.getDriverKind().hasBackingStorage())
        {
            responses.addEntry(ApiCallRcImpl
                .entryBuilder(
                    MASK_WARN | MASK_STOR_POOL,
                    "Resource will be automatically flagged diskless."
                )
                .setCause(String.format("Used storage pool '%s' is diskless, " +
                    "but resource was not flagged diskless", storPool.getName().displayValue))
                .build()
            );
            try
            {
                rsc.getStateFlags().enableFlags(apiCtx, Resource.RscFlags.DISKLESS);
            }
            catch (AccessDeniedException exc)
            {
                throw new ImplementationError(exc);
            }
            catch (SQLException exc)
            {
                throw new ApiSQLException(exc);
            }
        }

        return responses;
    }
}
