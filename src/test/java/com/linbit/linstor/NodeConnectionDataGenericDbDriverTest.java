package com.linbit.linstor;

import javax.inject.Inject;
import com.linbit.InvalidNameException;
import com.linbit.linstor.core.identifier.NodeName;
import com.linbit.linstor.core.objects.Node;
import com.linbit.linstor.core.objects.NodeConnection;
import com.linbit.linstor.core.objects.NodeConnectionData;
import com.linbit.linstor.core.objects.NodeConnectionDataGenericDbDriver;
import com.linbit.linstor.core.objects.NodeData;
import com.linbit.linstor.core.objects.TestFactory;
import com.linbit.linstor.security.AccessDeniedException;
import com.linbit.linstor.security.GenericDbBase;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NodeConnectionDataGenericDbDriverTest extends GenericDbBase
{
    private static final String SELECT_ALL_RES_CON_DFNS =
        " SELECT " + UUID + ", " + NODE_NAME_SRC + ", " + NODE_NAME_DST +
        " FROM " + TBL_NODE_CONNECTIONS;

    private final NodeName sourceName;
    private final NodeName targetName;

    private java.util.UUID uuid;
    private NodeData nodeSrc;
    private NodeData nodeDst;

    private NodeConnectionData nodeCon;
    @Inject private NodeConnectionDataGenericDbDriver driver;

    public NodeConnectionDataGenericDbDriverTest() throws InvalidNameException
    {
        sourceName = new NodeName("testNodeSource");
        targetName = new NodeName("testNodeTarget");
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Before
    public void setUp() throws Exception
    {
        super.setUpAndEnterScope();
        assertEquals(
            TBL_NODE_CONNECTIONS + " table's column count has changed. Update tests accordingly!",
            3,
            TBL_COL_COUNT_NODE_CONNECTIONS
        );

        uuid = randomUUID();

        nodeSrc = nodeDataFactory.create(SYS_CTX, sourceName, null, null);
        nodesMap.put(nodeSrc.getName(), nodeSrc);
        nodeDst = nodeDataFactory.create(SYS_CTX, targetName, null, null);
        nodesMap.put(nodeDst.getName(), nodeDst);

        nodeCon = TestFactory.createNodeConnectionData(
            uuid, nodeSrc, nodeDst, driver, propsContainerFactory, transObjFactory, transMgrProvider
        );
    }

    @Test
    public void testPersist() throws Exception
    {
        driver.create(nodeCon);
        commit();

        checkDbPersist(true);
    }

    @Test
    public void testPersistGetInstance() throws Exception
    {
        nodeConnectionDataFactory.create(SYS_CTX, nodeSrc, nodeDst);
        commit();

        checkDbPersist(false);
    }


    @Test
    public void testLoadAll() throws Exception
    {
        driver.create(nodeCon);

        List<NodeConnectionData> cons = driver.loadAll(nodesMap);

        assertNotNull(cons);

        assertEquals(1, cons.size());

        NodeConnection loadedConDfn = cons.get(0);
        assertNotNull(loadedConDfn);

        checkLoadedConDfn(loadedConDfn, true);
    }

    @Test
    public void testLoadGetInstance() throws Exception
    {
        driver.create(nodeCon);
        nodeSrc.setNodeConnection(SYS_CTX, nodeCon);
        nodeDst.setNodeConnection(SYS_CTX, nodeCon);

        NodeConnectionData loadedConDfn = NodeConnectionData.get(
            SYS_CTX,
            nodeSrc,
            nodeDst
        );

        checkLoadedConDfn(loadedConDfn, true);
    }

    @Test
    public void testCache() throws Exception
    {
        NodeConnectionData storedInstance = nodeConnectionDataFactory.create(
            SYS_CTX,
            nodeSrc,
            nodeDst
        );

        // no clear-cache

        assertEquals(storedInstance, NodeConnectionData.get(
            SYS_CTX,
            nodeSrc,
            nodeDst
        ));
    }

    @Test
    public void testDelete() throws Exception
    {
        driver.create(nodeCon);
        commit();

        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RES_CON_DFNS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue(resultSet.next());
        assertFalse(resultSet.next());
        resultSet.close();

        driver.delete(nodeCon);
        commit();

        resultSet = stmt.executeQuery();

        assertFalse(resultSet.next());
        resultSet.close();

        stmt.close();
    }

    @Test (expected = LinStorDataAlreadyExistsException.class)
    public void testAlreadyExists() throws Exception
    {
        driver.create(nodeCon);
        nodeSrc.setNodeConnection(SYS_CTX, nodeCon);
        nodeDst.setNodeConnection(SYS_CTX, nodeCon);

        nodeConnectionDataFactory.create(SYS_CTX, nodeSrc, nodeDst);
    }


    private void checkDbPersist(boolean checkUuid) throws SQLException
    {
        PreparedStatement stmt = getConnection().prepareStatement(SELECT_ALL_RES_CON_DFNS);
        ResultSet resultSet = stmt.executeQuery();

        assertTrue(resultSet.next());
        if (checkUuid)
        {
            assertEquals(uuid, java.util.UUID.fromString(resultSet.getString(UUID)));
        }
        assertEquals(sourceName.value, resultSet.getString(NODE_NAME_SRC));
        assertEquals(targetName.value, resultSet.getString(NODE_NAME_DST));

        assertFalse(resultSet.next());

        resultSet.close();
        stmt.close();
    }

    private void checkLoadedConDfn(NodeConnection loadedConDfn, boolean checkUuid) throws AccessDeniedException
    {
        assertNotNull(loadedConDfn);
        if (checkUuid)
        {
            assertEquals(uuid, loadedConDfn.getUuid());
        }
        Node sourceNode = loadedConDfn.getSourceNode(SYS_CTX);
        Node targetNode = loadedConDfn.getTargetNode(SYS_CTX);

        assertEquals(sourceName, sourceNode.getName());
        assertEquals(targetName, targetNode.getName());
    }
}
