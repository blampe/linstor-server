package com.linbit.linstor.api.rest.v1;

import com.linbit.linstor.api.ApiCallRc;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.rest.v1.serializer.Json;
import com.linbit.linstor.api.rest.v1.serializer.JsonGenTypes;
import com.linbit.linstor.api.rest.v1.utils.ApiCallRcRestUtils;
import com.linbit.linstor.core.apicallhandler.controller.CtrlRemoteApiCallHandler;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.Request;
import reactor.core.publisher.Flux;

@Path("v1/remotes")
@Produces(MediaType.APPLICATION_JSON)
public class Remotes
{
    private final RequestHelper requestHelper;
    private final ObjectMapper objectMapper;

    private final CtrlRemoteApiCallHandler remoteHandler;

    @Inject
    Remotes(
        RequestHelper requestHelperRef,
        CtrlRemoteApiCallHandler remoteHandlerRef
    )
    {
        requestHelper = requestHelperRef;
        remoteHandler = remoteHandlerRef;
        objectMapper = new ObjectMapper();
    }

    @GET
    public Response getRemotes(@Context Request request)
    {
        return requestHelper.doInScope(
            requestHelper.createContext(ApiConsts.API_LST_REMOTE, request),
            () ->
            {
                JsonGenTypes.RemoteList remoteList = new JsonGenTypes.RemoteList();
                remoteList.s3_remotes = remoteHandler.listS3().stream()
                    .map(Json::apiToS3Remote)
                    .collect(Collectors.toList());
                remoteList.linstor_remotes = remoteHandler.listLinstor().stream()
                    .map(Json::apiToLinstorRemote)
                    .collect(Collectors.toList());

                return Response
                    .status(Response.Status.OK)
                    .entity(objectMapper.writeValueAsString(remoteList))
                    .build();
            },
            false
        );
    }

    @GET
    @Path("s3")
    public Response getS3Remotes(@Context Request request)
    {
        return requestHelper.doInScope(
            requestHelper.createContext(ApiConsts.API_LST_REMOTE, request),
            () ->
            {
                List<JsonGenTypes.S3Remote> remoteList = remoteHandler.listS3().stream()
                    .map(Json::apiToS3Remote)
                    .collect(Collectors.toList());

                return Response
                    .status(Response.Status.OK)
                    .entity(objectMapper.writeValueAsString(remoteList))
                    .build();
            },
            false
        );
    }

    @POST
    @Path("s3")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createS3Remote(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        String jsonData
    )
    {
        try
        {
            JsonGenTypes.S3Remote remoteJson = objectMapper.readValue(jsonData, JsonGenTypes.S3Remote.class);
            Flux<ApiCallRc> flux = remoteHandler.createS3(
                remoteJson.remote_name,
                remoteJson.endpoint,
                remoteJson.bucket,
                remoteJson.region,
                remoteJson.access_key,
                remoteJson.secret_key
            ).subscriberContext(
                requestHelper.createContext(ApiConsts.API_SET_REMOTE, request)
            );

            requestHelper.doFlux(asyncResponse, ApiCallRcRestUtils.mapToMonoResponse(flux, Response.Status.OK));
        }
        catch (IOException ioExc)
        {
            ApiCallRcRestUtils.handleJsonParseException(ioExc, asyncResponse);
        }
    }

    @PUT
    @Path("s3/{remoteName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void changeS3Remote(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @PathParam("remoteName") String remoteName,
        String jsonData
    )
    {
        try
        {
            JsonGenTypes.S3Remote remoteJson = objectMapper.readValue(jsonData, JsonGenTypes.S3Remote.class);
            Flux<ApiCallRc> flux = remoteHandler.changeS3(
                remoteName,
                remoteJson.endpoint,
                remoteJson.bucket,
                remoteJson.region,
                remoteJson.access_key,
                remoteJson.secret_key
            ).subscriberContext(
                requestHelper.createContext(ApiConsts.API_SET_REMOTE, request)
            );

            requestHelper.doFlux(asyncResponse, ApiCallRcRestUtils.mapToMonoResponse(flux, Response.Status.OK));
        }
        catch (IOException ioExc)
        {
            ApiCallRcRestUtils.handleJsonParseException(ioExc, asyncResponse);
        }
    }

    @GET
    @Path("linstor")
    public Response getLinstorRemotes(@Context Request request)
    {
        return requestHelper.doInScope(
            requestHelper.createContext(ApiConsts.API_LST_REMOTE, request),
            () ->
            {
                List<JsonGenTypes.LinstorRemote> remoteList = remoteHandler.listLinstor().stream()
                    .map(Json::apiToLinstorRemote)
                    .collect(Collectors.toList());

                return Response
                    .status(Response.Status.OK)
                    .entity(objectMapper.writeValueAsString(remoteList))
                    .build();
            },
            false
        );
    }

    @POST
    @Path("linstor")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createLinstorRemote(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        String jsonData
    )
    {
        try
        {
            JsonGenTypes.LinstorRemote remoteJson = objectMapper.readValue(jsonData, JsonGenTypes.LinstorRemote.class);
            Flux<ApiCallRc> flux = remoteHandler.createLinstor(
                remoteJson.remote_name,
                remoteJson.url,
                remoteJson.passphrase
            ).subscriberContext(
                requestHelper.createContext(ApiConsts.API_SET_REMOTE, request)
            );

            requestHelper.doFlux(asyncResponse, ApiCallRcRestUtils.mapToMonoResponse(flux, Response.Status.OK));
        }
        catch (IOException ioExc)
        {
            ApiCallRcRestUtils.handleJsonParseException(ioExc, asyncResponse);
        }
    }

    @PUT
    @Path("linstor/{remoteName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void changeLinstorRemote(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @PathParam("remoteName") String remoteName,
        String jsonData
    )
    {
        try
        {
            JsonGenTypes.LinstorRemote remoteJson = objectMapper.readValue(jsonData, JsonGenTypes.LinstorRemote.class);
            Flux<ApiCallRc> flux = remoteHandler.changeLinstor(
                remoteName,
                remoteJson.url,
                remoteJson.passphrase
            ).subscriberContext(
                requestHelper.createContext(ApiConsts.API_SET_REMOTE, request)
            );

            requestHelper.doFlux(asyncResponse, ApiCallRcRestUtils.mapToMonoResponse(flux, Response.Status.OK));
        }
        catch (IOException ioExc)
        {
            ApiCallRcRestUtils.handleJsonParseException(ioExc, asyncResponse);
        }
    }

    @DELETE
    public void deleteRemote(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @QueryParam("remote_name") String remoteName
    )
    {
        Flux<ApiCallRc> flux = remoteHandler.delete(remoteName)
            .subscriberContext(requestHelper.createContext(ApiConsts.API_SET_REMOTE, request));
        requestHelper.doFlux(asyncResponse, ApiCallRcRestUtils.mapToMonoResponse(flux, Response.Status.OK));
    }
}