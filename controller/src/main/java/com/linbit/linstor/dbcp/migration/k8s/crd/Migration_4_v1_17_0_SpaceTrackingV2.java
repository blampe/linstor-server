package com.linbit.linstor.dbcp.migration.k8s.crd;

import com.linbit.linstor.ControllerK8sCrdDatabase;
import com.linbit.linstor.dbdrivers.GeneratedDatabaseTables;
import com.linbit.linstor.dbdrivers.k8s.crd.GenCrdV1_17_0;
import com.linbit.linstor.dbdrivers.k8s.crd.LinstorCrd;
import com.linbit.linstor.dbdrivers.k8s.crd.LinstorSpec;

import java.util.HashMap;

@K8sCrdMigration(
    description = "Migrate to SpaceTrackingV2",
    version = 4
)
public class Migration_4_v1_17_0_SpaceTrackingV2 extends BaseK8sCrdMigration
{
    public Migration_4_v1_17_0_SpaceTrackingV2()
    {
        super(
            GenCrdV1_17_0.createTxMgrContext(),
            GenCrdV1_17_0.createTxMgrContext(),
            GenCrdV1_17_0.createSchemaUpdateContext()
        );
    }

    @Override
    public MigrationResult migrateImpl(ControllerK8sCrdDatabase k8sDbRef) throws Exception
    {
        HashMap<String, LinstorCrd<LinstorSpec>> crdMap = txFrom.getCrd(GeneratedDatabaseTables.SPACE_HISTORY);
        for (LinstorCrd<LinstorSpec> value : crdMap.values())
        {
            txTo.delete(GeneratedDatabaseTables.SPACE_HISTORY, value);
        }

        crdMap.clear();

        crdMap = txTo.getCrd(GeneratedDatabaseTables.SATELLITES_CAPACITY);
        for (LinstorCrd<LinstorSpec> value : crdMap.values())
        {
            txTo.delete(GeneratedDatabaseTables.SATELLITES_CAPACITY, value);
        }

        MigrationResult result = new MigrationResult();
        return result;
    }
}