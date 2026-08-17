package service;

import model.AdminCommandRequest;
import model.AdminCommandResponse;
import model.InventoryItemData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Background administration service for the Store Front application.
 *
 * This service listens on the local loopback interface and processes admin
 * commands without interrupting the normal user console flow.
 *
 * Supported commands:
 * U = update inventory using a JSON payload
 * R = return all inventory as JSON
 */
public class AdministrationService implements Runnable {

    private final StoreFront storeFront;
    private final String inventoryFilePath;
    private final int port;
    private final FileService fileService;

    private volatile boolean running;
    private Thread workerThread;
    private ServerSocket serverSocket;

    /**
     * Creates a new administration service.
     *
     * @param storeFront store front instance to update and query
     * @param inventoryFilePath path to inventory JSON file
     * @param port local server port
     */
    public AdministrationService(StoreFront storeFront, String inventoryFilePath, int port) {
        this.storeFront = storeFront;
        this.inventoryFilePath = inventoryFilePath;
        this.port = port;
        this.fileService = new FileService();
    }

    /**
     * Starts the background service thread.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        workerThread = new Thread(this, "AdministrationServiceThread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Stops the background service.
     */
    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // Safe to ignore during shutdown.
        }
    }

    /**
     * Main service loop.
     */
    @Override
    public void run() {
        try (ServerSocket localServerSocket =
                     new ServerSocket(port, 50, InetAddress.getLoopbackAddress())) {
            this.serverSocket = localServerSocket;

            while (running) {
                try (Socket socket = localServerSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                    String requestJson = reader.readLine();
                    AdminCommandResponse response = handleRequest(requestJson);

                    writer.write(fileService.toJson(response));
                    writer.newLine();
                    writer.flush();
                } catch (IOException | FileServiceException | RuntimeException ex) {
                    // Continue running even if a single request fails.
                }
            }
        } catch (IOException ex) {
            if (running) {
                System.out.println("Administration Service failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Handles one admin request.
     *
     * @param requestJson JSON request string
     * @return response to send back to the admin client
     * @throws FileServiceException when file or JSON operations fail
     */
    private AdminCommandResponse handleRequest(String requestJson) throws FileServiceException {
        try {
            AdminCommandRequest request = fileService.fromJson(requestJson, AdminCommandRequest.class);
            String command = request.getCommand() == null ? "" : request.getCommand().trim().toUpperCase();

            switch (command) {
                case "U":
                    List<InventoryItemData> items =
                            fileService.parseInventoryJsonString(request.getPayload());

                    storeFront.replaceInventoryFromAdmin(items, inventoryFilePath);

                    return new AdminCommandResponse(
                            true,
                            "Inventory was updated successfully.",
                            ""
                    );

                case "R":
                    String inventoryJson = storeFront.getInventoryAsJson(inventoryFilePath);

                    return new AdminCommandResponse(
                            true,
                            "Inventory was returned successfully.",
                            inventoryJson
                    );

                default:
                    return new AdminCommandResponse(
                            false,
                            "Invalid command. Supported commands are U and R.",
                            ""
                    );
            }
        } catch (Exception ex) {
            return new AdminCommandResponse(
                    false,
                    "Request failed: " + ex.getMessage(),
                    ""
            );
        }
    }
}
