package com.linbit.linstor.backupshipping;

import com.linbit.extproc.ExtCmdFactory;
import com.linbit.linstor.annotation.SystemContext;
import com.linbit.linstor.api.interfaces.serializer.CtrlStltSerializer;
import com.linbit.linstor.core.ControllerPeerConnector;
import com.linbit.linstor.core.CoreModule.RemoteMap;
import com.linbit.linstor.core.StltConfigAccessor;
import com.linbit.linstor.core.StltConnTracker;
import com.linbit.linstor.core.StltSecurityObjects;
import com.linbit.linstor.core.objects.Remote;
import com.linbit.linstor.core.objects.Remote.RemoteType;
import com.linbit.linstor.core.objects.Snapshot;
import com.linbit.linstor.core.objects.StltRemote;
import com.linbit.linstor.logging.ErrorReporter;
import com.linbit.linstor.propscon.InvalidKeyException;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.linstor.storage.data.provider.AbsStorageVlmData;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.function.Consumer;

@Singleton
public class BackupShippingL2LService extends AbsBackupShippingService
{
    public static final String SERVICE_INFO = "BackupShippingL2LService";

    private static final String CMD_FORMAT_RECEIVING =
        "trap 'kill -HUP 0' SIGTERM; " +
        "set -o pipefail; " +
        "(" +
            "socat TCP-LISTEN:%s STDOUT | " +
            "zstd -d | " +
            // "pv -s 100m -bnr -i 0.1 | " +
            "%s ;" +
        ")& wait $!";
    private static final String CMD_FORMAT_SENDING =
        "trap 'kill -HUP 0' SIGTERM; " +
        "(" +
            "%s | " +
            // "pv -s 100m -bnr -i 0.1 | " +
            "zstd | " +
            "socat STDIN TCP:%s:%s ;" +
        ")&\\wait $!";


    @Inject
    public BackupShippingL2LService(
        ErrorReporter errorReporterRef,
        ExtCmdFactory extCmdFactoryRef,
        ControllerPeerConnector controllerPeerConnectorRef,
        CtrlStltSerializer interComSerializerRef,
        @SystemContext AccessContext accCtxRef,
        StltSecurityObjects stltSecObjRef,
        StltConfigAccessor stltConfigAccessorRef,
        StltConnTracker stltConnTrackerRef,
        RemoteMap remoteMapRef
    )
    {
        super(
            errorReporterRef,
            SERVICE_INFO,
            RemoteType.SATELLTE,
            extCmdFactoryRef,
            controllerPeerConnectorRef,
            interComSerializerRef,
            accCtxRef,
            stltSecObjRef,
            stltConfigAccessorRef,
            stltConnTrackerRef,
            remoteMapRef
        );
    }

    @Override
    protected String getCommandReceiving(String cmdRef, Remote remoteRef) throws AccessDeniedException
    {
        return String.format(
            CMD_FORMAT_RECEIVING,
            ((StltRemote) remoteRef).getPort(accCtx),
            cmdRef
        );
    }

    @Override
    protected String getCommandSending(String cmdRef, Remote remoteRef) throws AccessDeniedException
    {
        StltRemote stltRemote = (StltRemote) remoteRef;
        return String.format(
            CMD_FORMAT_SENDING,
            cmdRef,
            stltRemote.getIp(accCtx),
            stltRemote.getPort(accCtx)
        );
    }

    @Override
    protected BackupShippingDaemon createDaemon(
        AbsStorageVlmData<Snapshot> snapVlmDataRef,
        String shippingDescrRef,
        String[] fullCommandRef,
        String backupNameRef,
        Remote remoteRef,
        boolean restoreRef,
        Consumer<Boolean> postActionRef
    )
    {
        return new BackupShippingL2LDaemon(
            errorReporter,
            threadGroup,
            backupNameRef,
            fullCommandRef,
            postActionRef
        );
    }

    @Override
    protected String getBackupNameForRestore(AbsStorageVlmData<Snapshot> snapVlmDataRef)
        throws InvalidKeyException, AccessDeniedException
    {
        return snapVlmDataRef.getIdentifier();
    }

    @Override
    protected void preCtrlNotifyBackupShipped(
        boolean successRef,
        boolean restoringRef,
        Snapshot snapRef,
        ShippingInfo shippingInfoRef
    )
    {
        // ignore
    }
}