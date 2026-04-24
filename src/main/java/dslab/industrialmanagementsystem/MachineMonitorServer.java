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
import java.util.Random;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class MachineMonitorServer extends MachineMonitorGrpc.MachineMonitorImplBase {

    public static void main(String[] args) throws IOException, InterruptedException {
        MachineMonitorServer monitorServer = new MachineMonitorServer();
        int port = 40001;

        monitorServer.registerService(port);

        Server server = ServerBuilder.forPort(port)
                .addService(monitorServer)
                .build()
                .start();

        System.out.println("Machine Monitor Server started, listening on " + port);

        server.awaitTermination();
    }

    private void registerService(int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            ServiceInfo serviceInfo = ServiceInfo.create("_machine-monitor._tcp.local.", "machine-monitor", port, "Service for industrial monitoring");

            jmdns.registerService(serviceInfo);
            System.out.println("Service registered on JmDNS: " + serviceInfo.getName());

        } catch (IOException e) {
            System.out.println("Error registering service: " + e.getMessage());
        }
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

    @Override
    public void streamSensorData(MonitorRequest request, StreamObserver<SensorData> responseObserver) {
        System.out.println("Starting data stream for: " + request.getMachineId());
        Random random = new Random();

        try {
            for (int i = 0; i < 10; i++) {
                SensorData data = SensorData.newBuilder()
                        .setTemperature(20 + (30 * random.nextDouble()))
                        .setPerformanceLoad(random.nextDouble() * 100)
                        .build();

                responseObserver.onNext(data);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Stream interrupted");
        }

        responseObserver.onCompleted();
    }
}
