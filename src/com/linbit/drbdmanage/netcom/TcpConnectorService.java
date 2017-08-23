package com.linbit.drbdmanage.netcom;

import com.linbit.*;
import com.linbit.drbdmanage.CoreServices;
import com.linbit.drbdmanage.TcpPortNumber;
import com.linbit.drbdmanage.netcom.TcpConnectorMessage.ReadState;
import com.linbit.drbdmanage.netcom.TcpConnectorMessage.WriteState;
import com.linbit.drbdmanage.security.AccessContext;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.channels.SelectionKey.*;

/**
 * TCP/IP network communication service
 *
 * @author Robert Altnoeder &lt;robert.altnoeder@linbit.com&gt;
 */
public class TcpConnectorService implements Runnable, TcpConnector, SystemService
{
    private static final ServiceName SERVICE_NAME;
    private static final String SERVICE_INFO = "TCP/IP network communications service";

    protected ServiceName serviceInstanceName;

    private static final long REINIT_THROTTLE_TIME = 3000L;

    public static final int DEFAULT_PORT_VALUE = 9977;
    public static final TcpPortNumber DEFAULT_PORT;

    public static final InetAddress DEFAULT_BIND_INET_ADDRESS;
    public static final SocketAddress DEFAULT_BIND_ADDRESS;

    // Maximum number of connections to accept in one selector iteration
    public static final int MAX_ACCEPT_LOOP = 100;

    private CoreServices coreSvcs;
    private MessageProcessor msgProcessor;

    // Set by shutdown() to shut down the selector loop
    private AtomicBoolean shutdownFlag;

    // Selector loop thread
    private Thread selectorLoopThread;

    // Set to indicate that connections have been updated
    // outside of the selector loop
    private AtomicBoolean updateFlag;

    private ConnectionObserver connObserver;

    static
    {
        try
        {
            DEFAULT_PORT = new TcpPortNumber(DEFAULT_PORT_VALUE);

            SERVICE_NAME = new ServiceName("NetComService");
        }
        catch (ValueOutOfRangeException valueExc)
        {
            throw new ImplementationError(
                String.format(
                    "%s class default port constant is set to the out-of-range value %d",
                    TcpConnectorService.class.getName(), DEFAULT_PORT_VALUE
                ),
                valueExc
            );
        }
        catch (InvalidNameException nameExc)
        {
            throw new ImplementationError(
                String.format(
                    "%s class contains an invalid name constant",
                    TcpConnectorService.class.getName()
                ),
                nameExc
            );
        }

        // Initialize the default bind address
        {
            byte[] defaultIpv6Addr = new byte[16];
            Arrays.fill(defaultIpv6Addr, (byte) 0);
            try
            {
                DEFAULT_BIND_INET_ADDRESS = Inet6Address.getByAddress(defaultIpv6Addr);
            }
            catch (UnknownHostException hostExc)
            {
                throw new ImplementationError(
                    String.format(
                        "%s class default bind address constant is set to an illegal value",
                        TcpConnectorService.class.getName()
                    ),
                    hostExc
                );
            }
        }
        DEFAULT_BIND_ADDRESS = new InetSocketAddress(
            DEFAULT_BIND_INET_ADDRESS,
            DEFAULT_PORT.value
        );
    };

    // Address that the server socket will be listening on
    private SocketAddress bindAddress;

    // Server socket for accepting incoming connections
    private ServerSocketChannel serverSocket;

    // Default access context for a newly connected peer
    protected AccessContext defaultPeerAccCtx;

    // List of SocketChannels to register for OP_CONNECT
    private final LinkedList<SocketChannel> registerToConnect = new LinkedList<>();

    // Selector for all connections
    Selector serverSelector;


    public TcpConnectorService(
        CoreServices coreSvcsRef,
        MessageProcessor msgProcessorRef,
        AccessContext defaultPeerAccCtxRef,
        ConnectionObserver connObserverRef
    )
    {
        ErrorCheck.ctorNotNull(TcpConnectorService.class, CoreServices.class, coreSvcsRef);
        ErrorCheck.ctorNotNull(TcpConnectorService.class, MessageProcessor.class, msgProcessorRef);
        ErrorCheck.ctorNotNull(TcpConnectorService.class, AccessContext.class, defaultPeerAccCtxRef);
        serviceInstanceName = SERVICE_NAME;

        bindAddress     = DEFAULT_BIND_ADDRESS;
        serverSocket    = null;
        serverSelector  = null;
        coreSvcs        = coreSvcsRef;
        msgProcessor    = msgProcessorRef;
        // Prevent entering the run() method's selector loop
        // until initialize() has completed
        shutdownFlag    = new AtomicBoolean(true);
        connObserver    = connObserverRef;

        defaultPeerAccCtx = defaultPeerAccCtxRef;
    }

    public TcpConnectorService(
        CoreServices coreSvcsRef,
        MessageProcessor msgProcessorRef,
        SocketAddress bindAddressRef,
        AccessContext defaultPeerAccCtxRef,
        ConnectionObserver connObserverRef
    )
    {
        this(coreSvcsRef, msgProcessorRef, defaultPeerAccCtxRef, connObserverRef);
        ErrorCheck.ctorNotNull(TcpConnectorService.class, SocketAddress.class, bindAddressRef);
        bindAddress = bindAddressRef;
    }

    Object syncObj = new Object();

    @Override
    public Peer connect(InetSocketAddress address) throws IOException
    {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        String peerId = address.getAddress().getHostAddress() + ":" + address.getPort();
        SelectionKey connKey;
        Peer peer;
        synchronized (syncObj)
        {
            serverSelector.wakeup();
            boolean connected = socketChannel.connect(address);
            if (connected)
            {
                // if connect is true, we will never receive an OP_CONNECT
                // even if we register for it.
                // as the controller does not know about this peer (we didnt return yet)
                // we will register for no operation.
                // As soon as the controller tries to send a message, that will trigger the OP_WRITE anyways
                connKey = socketChannel.register(serverSelector, 0);
            }
            else
            {
                // if connect returns false we will receive OP_CONNECT
                // and we will need to call the finishConnection()
                connKey = socketChannel.register(serverSelector, OP_CONNECT);
            }
            peer = createTcpConnectorPeer(peerId, connKey, true);
            if (connected)
            {
                peer.connectionEstablished();
            }
            connKey.attach(peer);
        }
        return peer;
    }

    @Override
    public synchronized void start()
        throws SystemServiceStartException
    {
        if (selectorLoopThread == null)
        {
            try
            {
                initialize();
            }
            catch (IOException ioExc)
            {
                String descriptionText = String.format(
                    "Initialization of the %s service instance '%s' failed.",
                    TcpConnectorService.class.getName(),
                    serviceInstanceName.displayValue
                );
                throw new SystemServiceStartException(
                    descriptionText,
                    // Description
                    descriptionText,
                    // Cause
                    ioExc.getMessage(),
                    // Correction
                    null,
                    // Details
                    null,
                    ioExc
                );
            }
            selectorLoopThread = new Thread(this);
            selectorLoopThread.setName(serviceInstanceName.getDisplayName());
            selectorLoopThread.start();
        }
    }

    @Override
    public synchronized void shutdown()
    {
        shutdownFlag.set(true);
        if (serverSelector != null)
        {
            serverSelector.wakeup();
        }
    }

    @Override
    public void awaitShutdown(long timeout)
        throws InterruptedException
    {
        Thread joinThr = null;
        synchronized (this)
        {
            joinThr = selectorLoopThread;
        }
        if (joinThr != null)
        {
            joinThr.join(timeout);
        }
    }

    @Override
    public void run()
    {
        // Selector loop
        while (!shutdownFlag.get())
        {
            try
            {

                // Block until I/O operations are ready to be performed
                // on at least one of the channels, or until the selection
                // operation is interrupted (e.g., using wakeup())
                int selectCount = serverSelector.select();

                synchronized (syncObj)
                {
                    // wait for the syncObj to get released
                }

                // Ensure making some progress in the case that
                // the blocking select() call is repeatedly interrupted
                // (e.g., using wakeup()) before having selected any
                // channels
                if (selectCount <= 0)
                {
                    serverSelector.selectNow();
                }

                Iterator<SelectionKey> keysIter = serverSelector.selectedKeys().iterator();
                while (keysIter.hasNext())
                {
                    SelectionKey currentKey = keysIter.next();
                    keysIter.remove();
                    int ops = currentKey.readyOps();
                    if ((ops & OP_READ) != 0)
                    {
                        try
                        {
                            TcpConnectorPeer connPeer = (TcpConnectorPeer) currentKey.attachment();
                            ReadState state = connPeer.msgIn.read((SocketChannel) currentKey.channel());
                            switch (state)
                            {
                                case UNFINISHED:
                                    break;
                                case FINISHED:
                                    msgProcessor.processMessage(connPeer.msgIn, this, connPeer);
                                    connPeer.nextInMessage();
                                    break;
                                case END_OF_STREAM:
                                    closeConnection(currentKey);
                                    break;
                                default:
                                    throw new ImplementationError(
                                        String.format(
                                            "Missing case label for enum member '%s'",
                                            state.name()
                                        ),
                                        null
                                    );
                            }
                        }
                        catch (NotYetConnectedException connExc)
                        {
                            // This might possibly happen if an outbound connection is
                            // marked as READ interested before establishing the connection
                            // is finished; if the Selector would even report it as ready
                            // in this case.
                            // Anyway, the reason would be an implementation flaw of some
                            // kind, therefore, log this error and then treat the connection's
                            // state as a protocol error and close the connection.
                            coreSvcs.getErrorReporter().reportError(new ImplementationError(connExc));
                            closeConnection(currentKey);
                        }
                        catch (IllegalMessageStateException msgStateExc)
                        {
                            coreSvcs.getErrorReporter().reportError(
                                new ImplementationError(
                                    "A message object with an illegal state was registered " +
                                    "as the target of an I/O read operation",
                                    msgStateExc
                                )
                            );
                            closeConnection(currentKey);
                        }
                        catch (IOException ioExc)
                        {
                            // Protocol error - I/O error while reading a message
                            // Close the connection
                            closeConnection(currentKey);
                        }
                    }
                    else
                    if ((ops & OP_ACCEPT) != 0)
                    {
                        try
                        {
                            acceptConnection(currentKey);
                        }
                        catch (ClosedChannelException closeExc)
                        {
                            // May be thrown by accept() if the server socket is closed
                            // Attempt to reinitialize to recover
                            reinitialize();
                            // Break out of iterating over keys, because those are all
                            // invalid after reinitialization, and the set of keys may have
                            // been modified too
                            break;
                        }
                        catch (NotYetBoundException unboundExc)
                        {
                            // Generated if accept() is invoked on an unbound server socket
                            // This should not happen, unless there is an
                            // implementation error somewhere.
                            // Attempt to reinitialize to recover
                            reinitialize();
                            // Break out of iterating over keys, because those are all
                            // invalid after reinitialization, and the set of keys may have
                            // been modified too
                            break;
                        }
                        catch (ClosedSelectorException closeExc)
                        {
                            // Throw by accept() if the selector is closed
                            // Attempt to reinitialize to recover
                            reinitialize();
                            // Break out of iterating over keys, because those are all
                            // invalid after reinitialization, and the set of keys may have
                            // been modified too
                            break;
                        }
                        catch (IOException ioExc)
                        {
                            coreSvcs.getErrorReporter().reportError(ioExc);
                        }
                    }
                    else
                    if ((ops & OP_WRITE) != 0)
                    {
                        try
                        {
                            TcpConnectorPeer connPeer = (TcpConnectorPeer) currentKey.attachment();
                            WriteState state = connPeer.msgOut.write((SocketChannel) currentKey.channel());
                            switch (state)
                            {
                                case UNFINISHED:
                                    break;
                                case FINISHED:
                                    connPeer.nextOutMessage();
                                    break;
                                default:
                                    throw new ImplementationError(
                                        String.format(
                                            "Missing case label for enum member '%s'",
                                            state.name()
                                        ),
                                        null
                                    );
                            }
                        }
                        catch (NotYetConnectedException connExc)
                        {
                            // This might possibly happen if an outbound connection is
                            // marked as WRITE interested before establishing the connection
                            // is finished; if the Selector would even report it as ready
                            // in this case.
                            // Anyway, the reason would be an implementation flaw of some
                            // kind, therefore, log this error and then treat the connection's
                            // state as a protocol error and close the connection.
                            coreSvcs.getErrorReporter().reportError(new ImplementationError(connExc));
                            closeConnection(currentKey);
                        }
                        catch (IllegalMessageStateException msgStateExc)
                        {
                            coreSvcs.getErrorReporter().reportError(
                                new ImplementationError(
                                    "A message object with an illegal state was registered " +
                                    "as the target of an I/O write operation",
                                    msgStateExc
                                )
                            );
                            closeConnection(currentKey);
                        }
                        catch (IOException ioExc)
                        {
                            // Protocol error:
                            // I/O error while writing a message
                            // Close channel / disconnect peer, invalidate SelectionKey
                            coreSvcs.getErrorReporter().reportError(ioExc);
                            closeConnection(currentKey);
                        }
                    }
                    else
                    if ((ops & OP_CONNECT) != 0)
                    {
                        try
                        {
                            establishConnection(currentKey);
                        }
                        catch (IOException ioExc)
                        {
                            coreSvcs.getErrorReporter().reportError(ioExc);
                        }
                    }
                }
            }
            catch (ClosedSelectorException selectExc)
            {
                // Selector became inoperative. Log error and attempt to reinitialize.
                coreSvcs.getErrorReporter().reportError(selectExc);
                reinitialize();
            }
            catch (IOException ioExc)
            {
                // I/O error while selecting (likely), or an uncaught I/O error
                // while performing I/O on a channel (should not happen)
                // Log error and attempt to reinitialize.
                coreSvcs.getErrorReporter().reportError(ioExc);
                reinitialize();
            }
            catch (Exception exc)
            {
                // Uncaught exception. Log error and shut down.
                coreSvcs.getErrorReporter().reportError(exc);
                break;
            }
            catch (ImplementationError implErr)
            {
                // Uncaught exception. Log error and shut down.
                coreSvcs.getErrorReporter().reportError(implErr);
                break;
            }
        }

        uninitialize();

        synchronized (this)
        {
            selectorLoopThread = null;
        }
    }

    private void acceptConnection(SelectionKey currentKey)
        throws IOException
    {
        // Configure the socket for the accepted connection
        for (int loopCtr = 0; loopCtr < MAX_ACCEPT_LOOP; ++loopCtr)
        {
            SocketChannel newSocket = serverSocket.accept();
            if (newSocket != null)
            {
                boolean accepted = false;
                try
                {
                    newSocket.configureBlocking(false);

                    // Generate the id for the peer object from the remote address of the connection
                    SocketAddress sockAddr = newSocket.getRemoteAddress();
                    try
                    {
                        InetSocketAddress inetSockAddr = (InetSocketAddress) sockAddr;
                        InetAddress inetAddr = inetSockAddr.getAddress();
                        if (inetAddr != null)
                        {
                            String peerId = inetAddr.getHostAddress() + ":" + inetSockAddr.getPort();

                            // Register the accepted connection with the selector loop
                            SelectionKey connKey = null;
                            try
                            {
                                connKey = newSocket.register(serverSelector, SelectionKey.OP_READ);
                            }
                            catch (IllegalSelectorException illSelExc)
                            {
                                // Thrown by register() if the selector is from another I/O provider
                                // than the channel that is being registered
                                coreSvcs.getErrorReporter().reportError(
                                    new ImplementationError(
                                        "Registration of the channel with the selector failed, " +
                                        "because the channel was created by another type of " +
                                        "I/O provider",
                                        illSelExc
                                    )
                                );
                                // Connection was not accepted and will be closed in the finally block
                            }
                            catch (IllegalArgumentException illArg)
                            {
                                // Generated if a bit in the I/O operations specified
                                // in register() does not correspond with a supported I/O operation
                                // Should not happen; log the error.
                                // Connection was not accepted and will be closed in the finally block
                                coreSvcs.getErrorReporter().reportError(illArg);
                            }

                            if (connKey != null)
                            {
                                // Prepare the peer object and message
                                TcpConnectorPeer connPeer = createTcpConnectorPeer(peerId, connKey);
                                connKey.attach(connPeer);
                                connPeer.connectionEstablished();
                                if (connObserver != null)
                                {
                                    connObserver.inboundConnectionEstablished(connPeer);
                                }
                                accepted = true;
                            }
                        }
                        else
                        {
                            throw new IOException(
                                "Cannot generate the peer id, because the socket's " +
                                "internet address is uninitialized."
                            );
                        }
                    }
                    catch (ClassCastException ccExc)
                    {
                        throw new IOException(
                            "Peer connection address is not of type InetSocketAddress. " +
                            "Cannot generate the peer id."
                        );
                    }
                }
                catch (ClosedChannelException closeExc)
                {
                    // May be thrown by getRemoteAddress()
                    // Apparently, the peer closed the connection again before it could be accepted.
                    // No-op; connection was not accepted and will be closed in the finally block
                }
                catch (IllegalBlockingModeException illModeExc)
                {
                    // Implementation error, configureBlocking() skipped
                    coreSvcs.getErrorReporter().reportError(
                        new ImplementationError(
                            "The accept() operation failed because the new socket channel's " +
                            "blocking mode was not configured correctly",
                            illModeExc
                        )
                    );
                    // Connection was not accepted and will be closed in the finally block
                }
                catch (CancelledKeyException cancelExc)
                {
                    // May be thrown by register() if the channel is already registered
                    // with the selector, but has already been cancelled too (which should
                    // be impossible, because the channel is first registered after being
                    // accepted)
                    coreSvcs.getErrorReporter().reportError(cancelExc);
                    // Connection was not accepted and will be closed in the finally block
                }
                catch (IOException ioExc)
                {
                    // May be thrown by getRemoteAddress()
                    // This will cause the connection to be rejected because its endpoint
                    // address is undeterminable
                    coreSvcs.getErrorReporter().reportError(ioExc);
                    // Connection was not accepted and will be closed in the finally block
                }
                finally
                {
                    if (!accepted)
                    {
                        // Close any rejected connections
                        try
                        {
                            newSocket.close();
                        }
                        catch (IOException ioExc)
                        {
                            coreSvcs.getErrorReporter().reportError(ioExc);
                        }
                    }
                }
            }
            else
            {
                // No more connections to accept
                break;
            }
        }
    }

    protected TcpConnectorPeer createTcpConnectorPeer(String peerId, SelectionKey connKey)
    {
        return createTcpConnectorPeer(peerId, connKey, false);
    }

    protected TcpConnectorPeer createTcpConnectorPeer(String peerId, SelectionKey connKey, boolean outgoing)
    {
        return new TcpConnectorPeer(
            peerId, this, connKey, defaultPeerAccCtx
        );
    }

    @Override
    public void wakeup()
    {
        serverSelector.wakeup();
    }

    protected void establishConnection(SelectionKey currentKey)
        throws IOException
    {
        @SuppressWarnings("resource")
        SocketChannel channel = (SocketChannel) currentKey.channel();
        try
        {
            channel.finishConnect();
            currentKey.interestOps(0); // when controller wants to send a message, this will be changed to
            // OP_WRITE automatically
            Peer peer = (Peer) currentKey.attachment();
            peer.connectionEstablished();
            if (connObserver != null)
            {
                connObserver.outboundConnectionEstablished(peer);
            }
        }
        catch (ConnectException conExc)
        {
            if (conExc.getMessage().equals("Connection refused"))
            {
                // ignore, Reconnector will retry later
            }
        }
    }

    @Override
    public void closeConnection(TcpConnectorPeer peerObj)
    {
        closeConnection(peerObj.getSelectionKey());
    }

    private void closeConnection(SelectionKey currentKey)
    {
        Peer client = (TcpConnectorPeer) currentKey.attachment();
        if (connObserver != null && client != null)
        {
            connObserver.connectionClosed(client);
        }
        try
        {
            SelectableChannel channel = currentKey.channel();
            if (channel != null)
            {
                channel.close();
            }
        }
        catch (IOException closeIoExc)
        {
            // If close() fails with an I/O error, the reason may be interesting
            // enough to file an error report
            coreSvcs.getErrorReporter().reportError(closeIoExc);
        }
        currentKey.cancel();
    }

    private void closeAllConnections()
    {
        try
        {
            if (serverSelector != null)
            {
                for (SelectionKey currentKey : serverSelector.keys())
                {
                    closeConnection(currentKey);
                }
                serverSelector.close();
            }
        }
        catch (ClosedSelectorException selectExc)
        {
            // Cannot close any connections, because the selector is inoperative
            coreSvcs.getErrorReporter().reportError(selectExc);
        }
        catch (IOException ioExc)
        {
            // If close() fails with an I/O error, the reason may be interesting
            // enough to file an error report
            coreSvcs.getErrorReporter().reportError(ioExc);
        }
    }

    private void closeServerSocket()
    {
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
        catch (IOException ioExc)
        {
            // If close() fails with an I/O error, the reason may be interesting
            // enough to file an error report
            coreSvcs.getErrorReporter().reportError(ioExc);
        }
    }

    public void initialize() throws IOException
    {
        boolean initFlag = false;
        try
        {
            serverSocket = ServerSocketChannel.open();
            serverSelector = Selector.open();
            try
            {
                serverSocket.bind(bindAddress);
            }
            catch (AlreadyBoundException boundExc)
            {
                // Thrown if this server socket is already bound.
                // This is NOT the same error as if the TCP port is already in use,
                // which will generate a java.net.BindException instead (subclass of IOException)
                // This should not happen, unless there is a bug in the code that already
                // bound the socket before trying to bind() again in this section of code
                throw new ImplementationError(
                    "A newly created server socket could not be bound, because it is bound already.",
                    boundExc
                );
            }
            catch (UnsupportedAddressTypeException addrExc)
            {
                // Thrown if the socket can not be bound, because the type of
                // address to bind the socket to is unsupported
                throw new IOException(
                    "Server socket creation failed, the specified server address " +
                    "is not of a supported type.",
                    addrExc
                );
            }
            catch (ClosedChannelException closeExc)
            {
                // Thrown if the socket is closed when bind() is called
                throw new IOException(
                    "Server socket creation failed. The server socket was closed " +
                    "while its initialization was still in progress.",
                    closeExc
                );
            }
            serverSocket.configureBlocking(false);
            try
            {
                serverSocket.register(serverSelector, OP_ACCEPT);
            }
            catch (IllegalBlockingModeException illModeExc)
            {
                // Implementation error, configureBlocking() skipped
                throw new ImplementationError(
                    "The server socket could not be initialized because its " +
                    "blocking mode was not configured correctly",
                    illModeExc
                );
            }
            catch (ClosedSelectorException closeExc)
            {
                // Thrown if the selector is closed when register() is called
                throw new IOException(
                    "The server socket could not be initialized because the " +
                    "selector for the channel was closed when trying to register " +
                    "I/O operations for the server socket.",
                    closeExc
                );
            }
            catch (CancelledKeyException cancelExc)
            {
                // May be thrown by register() if the channel is already registered
                // with the selector, but has already been cancelled too (which should
                // be impossible, because the channel is first registered after being
                // accepted)
                throw new ImplementationError(
                    "Initialization of the server socket failed, because the socket's " +
                    "selection key was already registered and cancelled during initialization",
                    cancelExc
                );
            }
            catch (IllegalSelectorException illSelExc)
            {
                // Thrown by register() if the selector is from another I/O provider
                // than the channel that is being registered
                throw new ImplementationError(
                    "Initialization of the server socket failed because the channel " +
                    "was created by another type of I/O provider than the associated selector",
                    illSelExc
                );
            }
            catch (IllegalArgumentException illArg)
            {
                // Generated if a bit in the I/O operations specified
                // in register() does not correspond with a supported I/O operation
                // Should not happen; log the error.
                // Server socket can not accept connections, treat this as an I/O error
                throw new IOException(
                    "Configuring the server socket to accept new connections failed",
                    illArg
                );
            }

            // Enable entering the run() method's selector loop
            shutdownFlag.set(false);
            initFlag = true;
        }
        finally
        {
            if (!initFlag)
            {
                // Initialization failed, clean up
                uninitialize();
            }
        }
    }

    private void uninitialize()
    {
        closeAllConnections();
        closeServerSocket();

        serverSocket    = null;
        serverSelector  = null;
    }

    private synchronized void reinitialize()
    {
        uninitialize();

        // Throttle reinitialization to avoid busy-looping in case of a
        // persistent error during initialization (e.g., all network drivers down, ...)
        try
        {
            Thread.sleep(REINIT_THROTTLE_TIME);
        }
        catch (InterruptedException intrExc)
        {
            // No-op; thread may be interrupted to shorten the sleep()
        }

        try
        {
            initialize();
        }
        catch (IOException ioExc)
        {
            coreSvcs.getErrorReporter().reportError(ioExc);
        }
    }

    @Override
    public ServiceName getServiceName()
    {
        return SERVICE_NAME;
    }

    @Override
    public String getServiceInfo()
    {
        return SERVICE_INFO;
    }

    @Override
    public ServiceName getInstanceName()
    {
        return serviceInstanceName;
    }

    @Override
    public synchronized boolean isStarted()
    {
        return selectorLoopThread != null;
    }

    @Override
    public synchronized void setServiceInstanceName(ServiceName instanceName)
    {
        if (instanceName == null)
        {
            serviceInstanceName = SERVICE_NAME;
        }
        else
        {
            serviceInstanceName = instanceName;
        }
        if (selectorLoopThread != null)
        {
            selectorLoopThread.setName(serviceInstanceName.getDisplayName());
        }
    }
}
