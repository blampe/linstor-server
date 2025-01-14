package com.linbit.linstor.layer;

import com.linbit.ImplementationError;
import com.linbit.linstor.PriorityProps;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.core.objects.StorPool;
import com.linbit.linstor.propscon.InvalidKeyException;
import com.linbit.linstor.propscon.Props;
import com.linbit.linstor.storage.interfaces.layers.drbd.DrbdRscDfnObject.TransportType;
import com.linbit.utils.Pair;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class LayerPayload
{
    public DrbdRscPayload drbdRsc;
    public DrbdRscDfnPayload drbdRscDfn;
    public DrbdVlmDfnPayload drbdVlmDfn;

    public OpenflexRscDfnPayload ofRscDfn;

    public Map<Integer, String> luksVlmPasswords;

    public Map<Pair<String, Integer>, StorageVlmPayload> storagePayload;

    public LayerPayload()
    {
        drbdRsc = new DrbdRscPayload();
        drbdRscDfn = new DrbdRscDfnPayload();
        drbdVlmDfn = new DrbdVlmDfnPayload();

        ofRscDfn = new OpenflexRscDfnPayload();

        luksVlmPasswords = new TreeMap<>();

        storagePayload = new TreeMap<>();
    }

    public class DrbdRscPayload
    {
        public Integer nodeId;
        public Short peerSlots;
        public Integer alStripes;
        public Long alStripeSize;
        public boolean needsNewNodeId = false;
        public Long rscFlags;
    }

    public class DrbdRscDfnPayload
    {
        public Integer tcpPort;
        public TransportType transportType;
        public String sharedSecret;
        public Short peerSlotsNewResource;
        public Integer alStripes;
        public Long alStripeSize;
    }

    public class DrbdVlmDfnPayload
    {
        public Integer minorNr;
    }

    public DrbdRscPayload getDrbdRsc()
    {
        return drbdRsc;
    }

    public DrbdRscDfnPayload getDrbdRscDfn()
    {
        return drbdRscDfn;
    }

    public DrbdVlmDfnPayload getDrbdVlmDfn()
    {
        return drbdVlmDfn;
    }

    public class OpenflexRscDfnPayload
    {
        public String nqn;
    }

    public class StorageVlmPayload
    {
        public StorPool storPool;

        public StorageVlmPayload(StorPool storPoolRef)
        {
            storPool = storPoolRef;
        }
    }

    public StorageVlmPayload getStorageVlmPayload(String rscNameSuffix, int vlmNr)
    {
        return storagePayload.get(new Pair<>(rscNameSuffix, vlmNr));
    }

    public LayerPayload putStorageVlmPayload(String rscNameSuffix, int vlmNr, StorPool storPool)
    {
        storagePayload.put(new Pair<>(rscNameSuffix, vlmNr), new StorageVlmPayload(storPool));
        return this;
    }

    public LayerPayload extractFrom(Props props)
    {
        return extractFrom(new PriorityProps(props));
    }

    public LayerPayload extractFrom(PriorityProps prioProps)
    {
        try
        {
            drbdRscDfn.peerSlotsNewResource = asShort(prioProps.getProp(ApiConsts.KEY_PEER_SLOTS_NEW_RESOURCE));
        }
        catch (InvalidKeyException exc)
        {
            throw new ImplementationError(exc);
        }
        return this;
    }

    private Short asShort(String str)
    {
        return parseNumber(Short::parseShort, str);
    }

    private <T> T parseNumber(Function<String, T> func, String str)
    {
        T ret = null;
        if (str != null)
        {
            ret = func.apply(str);
        }
        return ret;
    }
}
