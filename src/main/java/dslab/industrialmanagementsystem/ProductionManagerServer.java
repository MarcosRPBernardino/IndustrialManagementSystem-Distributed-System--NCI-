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

public class ProductionManagerServer extends ProductionManagerGrpc.ProductionManagerImplBase {

    public static void main(String[] args) throws IOException, InterruptedException {
        ProductionManagerServer productionServer = new ProductionManagerServer();
        int port = 40002;

        productionServer.registerService(port);

        Server server = ServerBuilder.forPort(port)
                .addService(productionServer)
                .build()
                .start();

        System.out.println("Production Manager Server started on port " + port);
        server.awaitTermination();
    }

    private void registerService(int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo serviceInfo = ServiceInfo.create("_production-manager._tcp.local.", "production-manager", port, "Service for production logs");
            jmdns.registerService(serviceInfo);
            System.out.println("Service registered on JmDNS: _production-manager._tcp.local.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StreamObserver<LogEntry> uploadProductionLog(StreamObserver<ProductionSummary> responseObserver) {
        return new StreamObserver<LogEntry>() {
            int count = 0;
            double totalQuality = 0;

            @Override
            public void onNext(LogEntry log) {
                count++;
                totalQuality += log.getQualityScore();
                System.out.println("Received production log for: " + log.getProductId());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in production stream: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                double average = count > 0 ? totalQuality / count : 0;

                ProductionSummary summary = ProductionSummary.newBuilder()
                        .setTotalProcessed(count)
                        .setAverageQuality(average)
                        .build();

                responseObserver.onNext(summary);
                responseObserver.onCompleted();
                System.out.println("Batch completed. Summary sent.");
            }
        };
    }
}
