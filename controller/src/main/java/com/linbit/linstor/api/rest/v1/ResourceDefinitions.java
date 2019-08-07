package com.linbit.linstor.api.rest.v1;

import com.linbit.linstor.api.ApiCallRc;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.rest.v1.serializer.Json;
import com.linbit.linstor.api.rest.v1.serializer.JsonGenTypes;
import com.linbit.linstor.core.apicallhandler.controller.CtrlApiCallHandler;
import com.linbit.linstor.core.apicallhandler.controller.CtrlRscDfnDeleteApiCallHandler;
import com.linbit.linstor.core.objects.ResourceDefinition;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.Request;
import reactor.core.publisher.Flux;

@Path("resource-definitions")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceDefinitions
{
    private final RequestHelper requestHelper;
    private final CtrlApiCallHandler ctrlApiCallHandler;
    private final CtrlRscDfnDeleteApiCallHandler ctrlRscDfnDeleteApiCallHandler;
    private final ObjectMapper objectMapper;

    @Inject
    ResourceDefinitions(
        RequestHelper requestHelperRef,
        CtrlApiCallHandler ctrlApiCallHandlerRef,
        CtrlRscDfnDeleteApiCallHandler ctrlRscDfnDeleteApiCallHandlerRef
    )
    {
        requestHelper = requestHelperRef;
        ctrlApiCallHandler = ctrlApiCallHandlerRef;
        ctrlRscDfnDeleteApiCallHandler = ctrlRscDfnDeleteApiCallHandlerRef;
        objectMapper = new ObjectMapper();
    }

    @GET
    public Response listResourceDefinitions(
        @Context Request request,
        @DefaultValue("0") @QueryParam("limit") int limit,
        @DefaultValue("0") @QueryParam("offset") int offset
    )
    {
        return listResourceDefinitions(request, null, limit, offset);
    }

    @GET
    @Path("{rscName}")
    public Response listResourceDefinitions(
        @Context Request request,
        @PathParam("rscName") String rscName,
        @DefaultValue("0") @QueryParam("limit") int limit,
        @DefaultValue("0") @QueryParam("offset") int offset
    )
    {
        return requestHelper.doInScope(requestHelper.createContext(ApiConsts.API_LST_RSC_DFN, request), () ->
        {
            Stream<ResourceDefinition.RscDfnApi> rscDfnStream =
                ctrlApiCallHandler.listResourceDefinition().stream()
                    .filter(rscDfnApi -> rscName == null || rscDfnApi.getResourceName().equalsIgnoreCase(rscName));

            if (limit > 0)
            {
                rscDfnStream = rscDfnStream.skip(offset).limit(limit);
            }

            final List<JsonGenTypes.ResourceDefinition> rscDfns = rscDfnStream.map(Json::apiToResourceDefinition)
                .collect(Collectors.toList());

            return RequestHelper.queryRequestResponse(
                objectMapper, ApiConsts.FAIL_NOT_FOUND_RSC_DFN, "Resource definition", rscName, rscDfns
            );
        }, false);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResourceDefinition(@Context Request request, String jsonData)
    {
        return requestHelper.doInScope(requestHelper.createContext(ApiConsts.API_CRT_RSC_DFN, request), () ->
        {
            JsonGenTypes.ResourceDefinitionCreate rscDfnCreate = objectMapper.readValue(
                jsonData,
                JsonGenTypes.ResourceDefinitionCreate.class
            );
//            final List<VolumeDefinitionData> vlmDfns =
//                rscDfnCreate.resource_definition.volume_definitions != null ?
//                    rscDfnCreate.resource_definition.volume_definitions : new ArrayList<>();

            List<JsonGenTypes.ResourceDefinitionLayer> layerDataList = rscDfnCreate.resource_definition.layer_data;
            // currently we ignore the possible payload, only extract the layer-stack

            byte[] externalNameBytes = rscDfnCreate.resource_definition.external_name != null ?
                rscDfnCreate.resource_definition.external_name.getBytes(StandardCharsets.UTF_8) : null;


            ApiCallRc apiCallRc = ctrlApiCallHandler.createResourceDefinition(
                rscDfnCreate.resource_definition.name,
                externalNameBytes,
                rscDfnCreate.drbd_port,
                rscDfnCreate.drbd_secret,
                rscDfnCreate.drbd_transport_type,
                rscDfnCreate.resource_definition.props,
                new ArrayList<>(), // do not allow volume definition creations
                layerDataList.stream().map(rscDfnData -> rscDfnData.type).collect(Collectors.toList()),
                rscDfnCreate.drbd_peer_slots == null ? null : rscDfnCreate.drbd_peer_slots.shortValue(),
                rscDfnCreate.resource_definition.resource_group_name
            );
            return ApiCallRcConverter.toResponse(apiCallRc, Response.Status.CREATED);
        }, true);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{rscName}")
    public Response modifyResourceDefinition(
        @Context Request request,
        @PathParam("rscName") String rscName,
        String jsonData
    )
    {
        return requestHelper.doInScope(requestHelper.createContext(ApiConsts.API_MOD_RSC_DFN, request), () ->
        {
            JsonGenTypes.ResourceDefinitionModify modifyData =
                objectMapper.readValue(jsonData, JsonGenTypes.ResourceDefinitionModify.class);
            ApiCallRc apiCallRc = ctrlApiCallHandler.modifyRscDfn(
                null,
                rscName,
                modifyData.drbd_port,
                modifyData.override_props,
                new HashSet<>(modifyData.delete_props),
                new HashSet<>(modifyData.delete_namespaces),
                modifyData.layer_stack,
                modifyData.drbd_peer_slots == null ? null : modifyData.drbd_peer_slots.shortValue()
            );
            return ApiCallRcConverter.toResponse(apiCallRc, Response.Status.OK);
        }, true);
    }

    @DELETE
    @Path("{rscName}")
    public void deleteResourceDefinition(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @PathParam("rscName") String rscName)
    {
        Flux<ApiCallRc> flux = ctrlRscDfnDeleteApiCallHandler.deleteResourceDefinition(rscName)
            .subscriberContext(requestHelper.createContext(ApiConsts.API_DEL_RSC_DFN, request));

        requestHelper.doFlux(asyncResponse, ApiCallRcConverter.mapToMonoResponse(flux));
    }
}
