/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dslab.industrialmanagementsystem;

/**
 *
 * @author marco
 */
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class MachineMonitorServer extends MachineMonitorGrpc.MachineMonitorImplBase {

    public static void main(String[] args) throws IOException, InterruptedException {
        MachineMonitorServer monitorServer = new MachineMonitorServer();
        int port = 40001;

        Server server = ServerBuilder.forPort(port)
                .addService(monitorServer)
                .build()
                .start();

        System.out.println("Machine Monitor Server started, listening on " + port);

        server.awaitTermination();
    }

    @Override
    public void checkMachineStatus(StatusRequest request, StreamObserver<StatusResponse> responseObserver) {
        String machineId = request.getMachineId();
        System.out.println("Checking status for machine ID: " + machineId);

        boolean isActive = !machineId.isEmpty();
        String description = isActive ? "Machine " + machineId + " is operational." : "Invalid Machine ID.";

        StatusResponse response = StatusResponse.newBuilder()
                .setIsActive(isActive)
                .setDescription(description)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
