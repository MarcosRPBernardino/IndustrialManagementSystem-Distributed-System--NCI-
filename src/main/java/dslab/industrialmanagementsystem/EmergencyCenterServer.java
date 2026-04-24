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
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class EmergencyCenterServer extends EmergencyCenterGrpc.EmergencyCenterImplBase {

    public static void main(String[] args) throws IOException, InterruptedException {
        EmergencyCenterServer emergencyServer = new EmergencyCenterServer();
        int port = 40003;

        emergencyServer.registerService(port);

        Server server = ServerBuilder.forPort(port)
                .addService(emergencyServer)
                .build()
                .start();

        System.out.println("Emergency Center Server started on port " + port);
        server.awaitTermination();
    }

    private void registerService(int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo serviceInfo = ServiceInfo.create("_emergency-center._tcp.local.", "emergency-center", port, "Service for emergency coordination");
            jmdns.registerService(serviceInfo);
            System.out.println("Service registered on JmDNS: _emergency-center._tcp.local.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StreamObserver<AlertMessage> emergencyChannel(StreamObserver<AlertMessage> responseObserver) {
        return new StreamObserver<AlertMessage>() {
            @Override
            public void onNext(AlertMessage alert) {
                System.out.println("Emergency Alert from " + alert.getUser() + ": " + alert.getText() + " [Priority: " + alert.getPriority() + "]");

                AlertMessage response = AlertMessage.newBuilder()
                        .setUser("SYSTEM")
                        .setText("ACKNOWLEDGED: Emergency protocol initiated for " + alert.getUser())
                        .setPriority(alert.getPriority())
                        .build();

                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Emergency channel error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                System.out.println("Emergency channel closed by operator.");
            }
        };
    }
}
