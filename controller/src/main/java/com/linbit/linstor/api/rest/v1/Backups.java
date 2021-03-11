package com.linbit.linstor.api.rest.v1;

import com.linbit.ImplementationError;
import com.linbit.linstor.api.ApiCallRc;
import com.linbit.linstor.api.ApiCallRcImpl;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.rest.v1.serializer.JsonGenTypes;
import com.linbit.linstor.api.rest.v1.utils.ApiCallRcRestUtils;
import com.linbit.linstor.core.apicallhandler.controller.CtrlBackupApiCallHandler;
import com.linbit.linstor.core.apis.BackupListApi;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.utils.Pair;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.Request;
import reactor.core.publisher.Flux;

@Path("v1/backups")
@Produces(MediaType.APPLICATION_JSON)
public class Backups
{
    private final RequestHelper requestHelper;
    private final CtrlBackupApiCallHandler backupApiCallHandler;
    private final ObjectMapper objectMapper;

    @Inject
    public Backups(
        RequestHelper requestHelperRef,
        CtrlBackupApiCallHandler backupApiCallHandlerRef

    )
    {
        requestHelper = requestHelperRef;
        backupApiCallHandler = backupApiCallHandlerRef;
        objectMapper = new ObjectMapper();
    }

    @POST
    @Path("/{rscName}")
    public void createFullBackup(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @PathParam("rscName") String rscName
    )
    {
        Flux<ApiCallRc> responses;
        try
        {
            responses = backupApiCallHandler.createFullBackup(rscName)
                .subscriberContext(requestHelper.createContext(ApiConsts.API_CRT_BACKUP, request));
            requestHelper.doFlux(
                asyncResponse,
                ApiCallRcRestUtils.mapToMonoResponse(responses, Response.Status.CREATED)
            );
        }
        catch (AccessDeniedException exc)
        {
            throw new ImplementationError(exc);
        }
    }

    @DELETE
    @Path("/{rscName}")
    public void deleteBackups(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        @PathParam("rscName") String rscName,
        @DefaultValue("") @QueryParam("snapName") String snapName,
        @DefaultValue("") @QueryParam("timestamp") String timestamp
    )
    {
        if (snapName.length() != 0 && timestamp.length() != 0)
        {
            // CONFUSION!!!!!
            requestHelper
                .doFlux(
                    asyncResponse,
                    ApiCallRcRestUtils.mapToMonoResponse(
                        Flux.just(
                            ApiCallRcImpl.singleApiCallRc(
                                ApiConsts.FAIL_INVLD_REQUEST,
                                "Too many parameters given. Please use either snapName or timestamp or none of them, but not both!"
                            )
                        ), Response.Status.BAD_REQUEST
                    )
                );
        }
        else
        {
            Flux<ApiCallRc> deleteFlux = backupApiCallHandler.deleteBackup(
                rscName, snapName, timestamp, Collections.emptyList(), false
            ).subscriberContext(requestHelper.createContext(ApiConsts.API_DEL_BACKUP, request));
            requestHelper.doFlux(
                asyncResponse,
                ApiCallRcRestUtils.mapToMonoResponse(
                    deleteFlux,
                    Response.Status.OK
                )
            );
        }
    }

    @DELETE
    public void deleteBackup(
        @Context Request request,
        @Suspended final AsyncResponse asyncResponse,
        String jsonData
    )
    {
        JsonGenTypes.BackupDelete data;
        try
        {
            data = objectMapper.readValue(jsonData, JsonGenTypes.BackupDelete.class);
            requestHelper.doFlux(
                asyncResponse,
                ApiCallRcRestUtils.mapToMonoResponse(
                    backupApiCallHandler.deleteBackup("", "", "", data.s3keys, data.external)
                        .subscriberContext(requestHelper.createContext(ApiConsts.API_DEL_BACKUP, request)),
                    Response.Status.OK
                )
            );
        }
        catch (JsonProcessingException exc)
        {
            ApiCallRcRestUtils.handleJsonParseException(exc, asyncResponse);
        }
    }

    @GET
    public Response listBackups(
        @Context Request request,
        @DefaultValue("") @QueryParam("rscName") String rscName,
        @DefaultValue("") @QueryParam("bucketName") String bucketName
    )
    {
        return requestHelper.doInScope(
            ApiConsts.API_LST_BACKUPS, request,
            () ->
            {
                Pair<Collection<BackupListApi>, Set<String>> backups = backupApiCallHandler
                    .listBackups(rscName, bucketName);
                JsonGenTypes.Backup backupList = new JsonGenTypes.Backup();
                List<JsonGenTypes.BackupList> jsonBackups = fillJson(backups.objA);
                backupList.linstor = jsonBackups;
                backupList.other = new JsonGenTypes.BackupOther();
                backupList.other.files = new ArrayList<>(backups.objB);
                return Response.status(Response.Status.OK)
                    .entity(objectMapper.writeValueAsString(backupList))
                    .build();
            },
            false
        );
    }

    private List<JsonGenTypes.BackupList> fillJson(Collection<BackupListApi> backups)
    {
        List<JsonGenTypes.BackupList> jsonBackups = new ArrayList<>();
        for (BackupListApi backup : backups)
        {
            JsonGenTypes.BackupList jsonBackup = new JsonGenTypes.BackupList();
            jsonBackup.snap_key = backup.getSnapKey();
            jsonBackup.meta_name = backup.getMetaName();
            jsonBackup.finished_time = backup.getFinishedTime();
            jsonBackup.finished_timestamp = backup.getFinishedTimestamp();
            jsonBackup.node = backup.getNode();
            jsonBackup.shipping = backup.isShipping();
            jsonBackup.success = backup.successful();
            jsonBackup.vlms = backup.getVlms();
            jsonBackup.inc = backup.getInc() == null ? null : fillJson(backup.getInc());

            jsonBackups.add(jsonBackup);
        }
        return jsonBackups;
    }
}
