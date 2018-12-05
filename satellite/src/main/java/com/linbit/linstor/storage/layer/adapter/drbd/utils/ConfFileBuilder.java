package com.linbit.linstor.storage.layer.adapter.drbd.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.linbit.ImplementationError;
import com.linbit.InvalidNameException;
import com.linbit.linstor.LinStorException;
import com.linbit.linstor.LinStorRuntimeException;
import com.linbit.linstor.LsIpAddress;
import com.linbit.linstor.NetInterface;
import com.linbit.linstor.NetInterfaceName;
import com.linbit.linstor.Node;
import com.linbit.linstor.PriorityProps;
import com.linbit.linstor.Resource;
import com.linbit.linstor.ResourceConnection;
import com.linbit.linstor.ResourceDefinition;
import com.linbit.linstor.TcpPortNumber;
import com.linbit.linstor.Volume;
import com.linbit.linstor.VolumeDefinition;
import com.linbit.linstor.Resource.RscFlags;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.prop.LinStorObject;
import com.linbit.linstor.api.prop.WhitelistProps;
import com.linbit.linstor.core.LinStor;
import com.linbit.linstor.logging.ErrorReporter;
import com.linbit.linstor.propscon.InvalidKeyException;
import com.linbit.linstor.propscon.Props;
import com.linbit.linstor.security.AccessContext;
import com.linbit.linstor.security.AccessDeniedException;
import org.slf4j.event.Level;

public class ConfFileBuilder
{
    private static final ResourceNameComparator RESOURCE_NAME_COMPARATOR = new ResourceNameComparator();

    private final ErrorReporter errorReporter;
    private final AccessContext accCtx;
    private final Resource localRsc;
    private final Collection<Resource> remoteResources;
    private final WhitelistProps whitelistProps;

    private StringBuilder stringBuilder;
    private int indentDepth;

    public ConfFileBuilder(
        final ErrorReporter errorReporterRef,
        final AccessContext accCtxRef,
        final Resource localRscRef,
        final Collection<Resource> remoteResourcesRef,
        final WhitelistProps whitelistPropsRef
    )
    {
        errorReporter = errorReporterRef;
        accCtx = accCtxRef;
        localRsc = localRscRef;
        remoteResources = remoteResourcesRef;
        whitelistProps = whitelistPropsRef;

        stringBuilder = new StringBuilder();
        indentDepth = 0;
    }

    // Constructor used for the common linstor conf
    public ConfFileBuilder(
        final ErrorReporter errorReporterRef,
        final WhitelistProps whitelistPropsRef
    )
    {
        errorReporter = errorReporterRef;
        accCtx = null;
        localRsc = null;
        remoteResources = null;
        whitelistProps = whitelistPropsRef;

        stringBuilder = new StringBuilder();
        indentDepth = 0;
    }

    private String header()
    {
        return String.format("# This file was generated by linstor(%s), do not edit manually.",
            LinStor.VERSION_INFO_PROVIDER.getVersion());
    }

    public String build()
        throws AccessDeniedException
    {
        Set<Resource> peerRscSet = new TreeSet<>(RESOURCE_NAME_COMPARATOR);
        peerRscSet.addAll(remoteResources); // node-alphabetically sorted

        final ResourceDefinition rscDfn = localRsc.getDefinition();

        appendLine(header());
        appendLine("");
        appendLine("resource \"%s\"", localRsc.getDefinition().getName().displayValue);
        try (Section resourceSection = new Section())
        {
            // include linstor common
            appendLine("template-file \"linstor_common.conf\";");

            if (rscDfn.getProps(accCtx).getNamespace(ApiConsts.NAMESPC_DRBD_RESOURCE_OPTIONS).isPresent())
            {
                appendLine("");
                appendLine("options");
                try (Section optionsSection = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        rscDfn.getProps(accCtx),
                        ApiConsts.NAMESPC_DRBD_RESOURCE_OPTIONS
                    );
                }
            }

            appendLine("");
            appendLine("net");
            try (Section netSection = new Section())
            {
                // TODO: make configurable
                appendLine("cram-hmac-alg     %s;", "sha1");
                // TODO: make configurable
                appendLine("shared-secret     \"%s\";", localRsc.getDefinition().getSecret(accCtx));

                appendDrbdOptions(
                    LinStorObject.CONTROLLER,
                    rscDfn.getProps(accCtx),
                    ApiConsts.NAMESPC_DRBD_NET_OPTIONS
                );
            }

            if (rscDfn.getProps(accCtx).getNamespace(ApiConsts.NAMESPC_DRBD_DISK_OPTIONS).isPresent())
            {
                appendLine("");
                appendLine("disk");
                try (Section ignore = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        rscDfn.getProps(accCtx),
                        ApiConsts.NAMESPC_DRBD_DISK_OPTIONS
                    );
                }
            }

            int port = localRsc.getDefinition().getPort(accCtx).value;
            // Create local network configuration
            {
                appendLine("");
                appendLine("on %s", localRsc.getAssignedNode().getName().displayValue);
                try (Section onSection = new Section())
                {
                    Iterator<Volume> vlmIterator = localRsc.iterateVolumes();
                    while (vlmIterator.hasNext())
                    {
                        appendVlmIfPresent(vlmIterator.next(), accCtx, false);
                    }
                    appendLine("node-id    %d;", localRsc.getNodeId().value);
                }
            }

            for (final Resource peerRsc : peerRscSet)
            {
                if (peerRsc.getStateFlags().isUnset(accCtx, RscFlags.DELETE))
                {
                    appendLine("");
                    appendLine("on %s", peerRsc.getAssignedNode().getName().displayValue);
                    try (Section onSection = new Section())
                    {
                        Iterator<Volume> peerVlms = peerRsc.iterateVolumes();
                        while (peerVlms.hasNext())
                        {
                            appendVlmIfPresent(peerVlms.next(), accCtx, true);
                        }

                        appendLine("node-id    %d;", peerRsc.getNodeId().value);

                        // TODO: implement "multi-connection / path magic" (nodeMeshes + singleConnections vars)
                        // sb.append(peerResource.co)
                    }
                }
            }

            // first generate all with local first
            for (final Resource peerRsc : peerRscSet)
            {
                // don't create a connection entry if the resource has the deleted flag
                // or if it is a connection between two diskless nodes
                if (peerRsc.getStateFlags().isUnset(accCtx, RscFlags.DELETE) &&
                        !(peerRsc.disklessForPeers(accCtx) &&
                            localRsc.getStateFlags().isSet(accCtx, RscFlags.DISKLESS)))
                {
                    appendLine("");
                    appendLine("connection");
                    try (Section connectionSection = new Section())
                    {
                        ResourceConnection rscConn = localRsc.getResourceConnection(accCtx, peerRsc);

                        appendConnectionHost(port, localRsc, rscConn);
                        appendConnectionHost(port, peerRsc, rscConn);

                        if (rscConn != null)
                        {
                            Props rscConnProps = rscConn.getProps(accCtx);
                            if (rscConnProps.getNamespace(ApiConsts.NAMESPC_DRBD_NET_OPTIONS).isPresent())
                            {
                                appendLine("");
                                appendLine("net");
                                try (Section ignore = new Section())
                                {
                                    appendDrbdOptions(
                                        LinStorObject.CONTROLLER,
                                        rscConnProps,
                                        ApiConsts.NAMESPC_DRBD_NET_OPTIONS
                                    );
                                }
                            }

                            if (rscConnProps.getNamespace(ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS).isPresent())
                            {
                                appendLine("");
                                appendLine("disk");
                                try (Section ignore = new Section())
                                {
                                    appendConflictingDrbdOptions(
                                        LinStorObject.CONTROLLER,
                                        "resource-definition",
                                        rscDfn.getProps(accCtx),
                                        rscConnProps,
                                        ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS
                                    );
                                }
                            }
                        }
                        else
                        {
                            if (rscDfn.getProps(accCtx)
                                .getNamespace(ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS).isPresent()
                                )
                            {
                                appendLine("");
                                appendLine("disk");
                                try (Section ignore = new Section())
                                {
                                    appendDrbdOptions(
                                        LinStorObject.CONTROLLER,
                                        rscDfn.getProps(accCtx),
                                        ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS
                                    );
                                }
                            }
                        }
                    }
                }
            }

            Optional<String> compressionTypeProp = rscDfn.getProps(accCtx)
                .getNamespace(ApiConsts.NAMESPC_DRBD_PROXY)
                .map(Props::map)
                .map(map -> map.get(ApiConsts.KEY_DRBD_PROXY_COMPRESSION_TYPE));

            if (rscDfn.getProps(accCtx).getNamespace(ApiConsts.NAMESPC_DRBD_PROXY_OPTIONS).isPresent() ||
                compressionTypeProp.isPresent())
            {
                appendLine("");
                appendLine("proxy");
                try (Section ignore = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.DRBD_PROXY,
                        rscDfn.getProps(accCtx),
                        ApiConsts.NAMESPC_DRBD_PROXY_OPTIONS
                    );

                    if (compressionTypeProp.isPresent())
                    {
                        appendCompressionPlugin(rscDfn, compressionTypeProp.get());
                    }
                }
            }
        }

        return stringBuilder.toString();
    }

    private void appendConnectionHost(int rscDfnPort, Resource rsc, ResourceConnection rscConn)
        throws AccessDeniedException
    {
        TcpPortNumber rscConnPort = rscConn == null ? null : rscConn.getPort(accCtx);
        int port = rscConnPort == null ? rscDfnPort : rscConnPort.value;

        NetInterface netIf = getPreferredNetIf(rsc);
        LsIpAddress addr = netIf.getAddress(accCtx);
        String addrText = addr.getAddress();

        String outsideAddress;
        if (addr.getAddressType() == LsIpAddress.AddrType.IPv6)
        {
            outsideAddress = String.format("ipv6 [%s]:%d", addrText, port);
        }
        else
        {
            outsideAddress = String.format("ipv4 %s:%d", addrText, port);
        }

        String hostName = rsc.getAssignedNode().getName().displayValue;

        if (rscConn != null && rscConn.getStateFlags().isSet(accCtx, ResourceConnection.RscConnFlags.LOCAL_DRBD_PROXY))
        {
            appendLine("host %s address 127.0.0.1:%d via proxy on %s", hostName, port, hostName);
            try (Section ignore = new Section())
            {
                appendLine("inside 127.0.0.2:%d;", port);
                appendLine("outside %s;", outsideAddress);
            }
        }
        else
        {
            appendLine("host %s address %s;", hostName, outsideAddress);
        }
    }

    public String buildCommonConf(final Props satelliteProps)
    {
        appendLine(header());
        appendLine("");
        appendLine("common");
        try (Section commonSection = new Section())
        {
            if (satelliteProps.getNamespace(ApiConsts.NAMESPC_DRBD_DISK_OPTIONS).isPresent() ||
                satelliteProps.getNamespace(ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS).isPresent())
            {
                appendLine("disk");
                try (Section ignore = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        satelliteProps,
                        ApiConsts.NAMESPC_DRBD_DISK_OPTIONS
                    );

                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        satelliteProps,
                        ApiConsts.NAMESPC_DRBD_PEER_DEVICE_OPTIONS
                    );
                }
            }

            if (satelliteProps.getNamespace(ApiConsts.NAMESPC_DRBD_NET_OPTIONS).isPresent())
            {
                appendLine("net");
                try (Section ignore = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        satelliteProps,
                        ApiConsts.NAMESPC_DRBD_NET_OPTIONS
                    );
                }
            }

            if (satelliteProps.getNamespace(ApiConsts.NAMESPC_DRBD_RESOURCE_OPTIONS).isPresent())
            {
                appendLine("options");
                try (Section ignore = new Section())
                {
                    appendDrbdOptions(
                        LinStorObject.CONTROLLER,
                        satelliteProps,
                        ApiConsts.NAMESPC_DRBD_RESOURCE_OPTIONS
                    );
                }
            }
        }

        return stringBuilder.toString();
    }

    private void appendCompressionPlugin(ResourceDefinition rscDfn, String compressionType)
        throws AccessDeniedException
    {
        appendLine("plugin");
        try (Section pluginSection = new Section())
        {
            String namespace = ApiConsts.NAMESPC_DRBD_PROXY_COMPRESSION_OPTIONS;

            List<String> compressionPluginTerms = new ArrayList<>();
            compressionPluginTerms.add(compressionType);

            Map<String, String> drbdProps = rscDfn.getProps(accCtx)
                .getNamespace(namespace)
                .map(Props::map).orElse(new HashMap<>());

            for (Map.Entry<String, String> entry : drbdProps.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();
                if (checkValidDrbdOption(LinStorObject.drbdProxyCompressionObject(compressionType), key, value))
                {
                    compressionPluginTerms.add(key.substring(namespace.length() + 1));
                    compressionPluginTerms.add(value);
                }
            }

            appendLine("%s;", String.join(" ", compressionPluginTerms));
        }
    }

    private boolean checkValidDrbdOption(
        final LinStorObject lsObj,
        final String key,
        final String value
    )
    {
        boolean ret = true;
        if (!whitelistProps.isAllowed(lsObj, key, value, true))
        {
            ret = false;
            errorReporter.reportProblem(
                Level.WARN,
                new LinStorException(
                    "Ignoring property '" + key + "' with value '" + value + "' as it is not whitelisted."
                ),
                null,
                null,
                "The whitelist was generated from 'drbdsetup xml-help {resource,peer-device,net,disk}-options'" +
                    " when the satellite started."
            );
        }

        return ret;
    }

    private void appendConflictingDrbdOptions(
        final LinStorObject lsObj,
        final String parentName,
        final Props propsParent,
        final Props props,
        final String namespace
    )
    {
        Map<String, String> mapParent = propsParent.getNamespace(namespace)
            .map(Props::map).orElse(new HashMap<>());

        Map<String, String> mapProps = props.getNamespace(namespace)
            .map(Props::map).orElse(new HashMap<>());

        Set<String> writtenProps = new TreeSet<>();

        for (Map.Entry<String, String> entry : mapParent.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            final String configKey = key.substring(namespace.length() + 1);
            if (checkValidDrbdOption(lsObj, key, value))
            {
                final String absKey = Props.PATH_SEPARATOR + key; // key needs to be absolute
                if (mapProps.containsKey(absKey))
                {
                    appendCommentLine("%s %s; # set on %s",
                        configKey,
                        value,
                        parentName
                    );
                    appendLine("%s %s;",
                        configKey,
                        mapProps.get(absKey)
                    );
                }
                else
                {
                    appendLine("%s %s;",
                        configKey,
                        value
                    );
                }
                writtenProps.add(key);
            }
        }

        for (Map.Entry<String, String> entry : mapProps.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            final String configKey = key.substring(namespace.length() + 1);
            if (!writtenProps.contains(key) && checkValidDrbdOption(lsObj, key, value))
            {
                appendLine("%s %s;",
                    configKey,
                    value
                );
            }
        }
    }

    private void appendDrbdOptions(
        final LinStorObject lsObj,
        final Props props,
        final String namespace
    )
    {
        Map<String, String> drbdProps = props.getNamespace(namespace)
            .map(Props::map).orElse(new HashMap<>());

        for (Map.Entry<String, String> entry : drbdProps.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            if (checkValidDrbdOption(lsObj, key, value))
            {
                appendLine("%s %s;",
                    key.substring(namespace.length() + 1),
                    value
                );
            }
        }
    }

    private NetInterface getPreferredNetIf(Resource rsc)
    {
        NetInterface preferredNetIf = null;
        try
        {
            Volume firstVlm = rsc.iterateVolumes().next();

            PriorityProps prioProps;
            prioProps = new PriorityProps(
                firstVlm.getStorPool(accCtx).getProps(accCtx),
                firstVlm.getProps(accCtx),
                rsc.getProps(accCtx),
                rsc.getAssignedNode().getProps(accCtx)
            );

            String prefNic = prioProps.getProp(ApiConsts.KEY_STOR_POOL_PREF_NIC);

            if (prefNic != null)
            {
                preferredNetIf = rsc.getAssignedNode().getNetInterface(
                    accCtx,
                    new NetInterfaceName(prefNic) // TODO: validate on controller
                );

                if (preferredNetIf == null)
                {
                    errorReporter.logWarning(
                        String.format("Preferred network interface '%s' not found, fallback to default", prefNic)
                    );
                }
            }

            // fallback if preferred couldn't be found
            if (preferredNetIf == null)
            {
                Node assgNode = rsc.getAssignedNode();
                // Try to find the 'default' network interface
                preferredNetIf = assgNode.getNetInterface(accCtx, NetInterfaceName.DEFAULT_NET_INTERFACE_NAME);
                // If there is not even a 'default', use the first one that is found in the node's
                // list of network interfaces
                if (preferredNetIf == null)
                {
                    preferredNetIf = assgNode.streamNetInterfaces(accCtx).findFirst().orElse(null);
                }
            }
        }
        catch (AccessDeniedException | InvalidKeyException | InvalidNameException implError)
        {
            throw new ImplementationError(implError);
        }

        return preferredNetIf;
    }

    private void appendVlmIfPresent(Volume vlm, AccessContext localAccCtx, boolean isPeerRsc)
        throws AccessDeniedException
    {
        if (vlm.getFlags().isUnset(localAccCtx, Volume.VlmFlags.DELETE))
        {
            final String disk;
            if (vlm.getBackingDiskPath(localAccCtx) == null ||
                (isPeerRsc && vlm.getResource().disklessForPeers(localAccCtx)) ||
                (!isPeerRsc && vlm.getResource().getStateFlags().isSet(localAccCtx, RscFlags.DISKLESS)))
            {
                disk = "none";
            }
            else
            {
                if (vlm.getResource().equals(localRsc))
                {
                    String backingDiskPath = vlm.getBackingDiskPath(localAccCtx);
                    if (backingDiskPath.trim().equals(""))
                    {
                        throw new LinStorRuntimeException(
                            "Local volume does an empty block device. This might be result of an other error.",
                            "The storage driver returned an empty string instead of the path of the backing device",
                            "This is either an implementation error or just a side effect of an other " +
                                "recently occured error. Please check the error logs and try to solve the other " +
                                "other errors first",
                            null,
                            vlm.toString()
                        );
                    }
                    disk = backingDiskPath;
                }
                else
                {
                    // Do not use the backing disk path from the peer resource because it may be 'none' when
                    // the peer resource is converting from diskless, but the path here should not be 'none'
                    disk = "/dev/drbd/this/is/not/used";
                }
            }
            final String metaDisk;
            if (vlm.getMetaDiskPath(localAccCtx) == null)
            {
                metaDisk = "internal";
            }
            else
            {
                String tmpMeta = vlm.getMetaDiskPath(localAccCtx);
                if (tmpMeta.trim().equals(""))
                {
                    metaDisk = "internal";
                }
                else
                {
                    metaDisk = vlm.getMetaDiskPath(localAccCtx);
                }
            }

            final VolumeDefinition vlmDfn = vlm.getVolumeDefinition();
            appendLine("volume %s", vlmDfn.getVolumeNumber().value);
            try (Section volumeSection = new Section())
            {
                appendLine("disk        %s;", disk);

                if (vlmDfn.getProps(accCtx).getNamespace(ApiConsts.NAMESPC_DRBD_DISK_OPTIONS).isPresent())
                {
                    appendLine("disk");
                    try (Section ignore = new Section())
                    {
                        appendDrbdOptions(
                            LinStorObject.CONTROLLER,
                            vlmDfn.getProps(accCtx),
                            ApiConsts.NAMESPC_DRBD_DISK_OPTIONS
                        );
                    }
                }

                appendLine("meta-disk   %s;", metaDisk);
                appendLine("device      minor %d;",
                    vlm.getVolumeDefinition().getMinorNr(localAccCtx).value
                // TODO: impl and ask storPool for device
                );
                // TODO: add "disk { ... }" section
            }
        }
    }

    private void appendIndent()
    {
        for (int idx = 0; idx < indentDepth; idx++)
        {
            stringBuilder.append("    ");
        }
    }

    private void append(String format, Object... args)
    {
        stringBuilder.append(String.format(format, args));
    }

    private void appendLine(String format, Object... args)
    {
        appendIndent();
        append(format, args);
        stringBuilder.append("\n");
    }

    private void appendCommentLine(String format, Object... args)
    {
        stringBuilder.append("#");
        appendLine(format, args);
    }

    private static class ResourceNameComparator implements Comparator<Resource>
    {
        @Override
        public int compare(Resource o1, Resource o2)
        {
            return o1.getAssignedNode().getName().compareTo(o2.getAssignedNode().getName());
        }
    }

    /**
     * Allows a section to be expressed using try-with-resources so that it is automatically closed.
     * <p>
     * Non-static to allow access to the indentDepth.
     */
    private class Section implements AutoCloseable
    {
        Section()
        {
            appendLine("{");
            indentDepth++;
        }

        @Override
        public void close()
        {
            indentDepth--;
            appendLine("}");
        }
    }
}
