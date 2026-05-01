/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dslab.industrialmanagementsystem;

/**
 *
 * @author marco
 */
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class IndustrialGUI extends JFrame {

    private JTextArea consoleLog;
    private JTextField machineIdInput;
    private volatile String monitorHost;
    private volatile int monitorPort;
    private volatile String productionHost;
    private volatile int productionPort;
    private volatile String emergencyHost;
    private volatile int emergencyPort;

    public IndustrialGUI() {
        setTitle("Industrial Management System - Controller");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Machine ID:"));
        machineIdInput = new JTextField("M1", 5);
        topPanel.add(machineIdInput);

        JButton btnCheck = new JButton("Check Status");
        JButton btnStream = new JButton("Monitor Telemetry");
        JButton btnUpload = new JButton("Upload Logs");
        JButton btnEmergency = new JButton("EMERGENCY");

        btnEmergency.setBackground(Color.RED);
        btnEmergency.setForeground(Color.WHITE);

        topPanel.add(btnCheck);
        topPanel.add(btnStream);
        topPanel.add(btnUpload);
        topPanel.add(btnEmergency);

        add(topPanel, BorderLayout.NORTH);

        consoleLog = new JTextArea();
        consoleLog.setEditable(false);
        consoleLog.setBackground(Color.BLACK);
        consoleLog.setForeground(Color.GREEN);
        add(new JScrollPane(consoleLog), BorderLayout.CENTER);

        btnCheck.addActionListener(e -> checkStatus());
        btnStream.addActionListener(e -> streamTelemetry());
        btnUpload.addActionListener(e -> uploadProduction());
        btnEmergency.addActionListener(e -> triggerEmergency());

        new Thread(this::discoverServices).start();
    }

    private void discoverServices() {
        try {
            log("System: Looking for services on network...");
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            ServiceListener commonListener = new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    log("Service lost: " + event.getName());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    String name = event.getName();
                    String host = event.getInfo().getHostAddresses()[0];
                    int port = event.getInfo().getPort();

                    if (name.contains("machine-monitor")) {
                        monitorHost = host;
                        monitorPort = port;
                    } else if (name.contains("production-manager")) {
                        productionHost = host;
                        productionPort = port;
                    } else if (name.contains("emergency-center")) {
                        emergencyHost = host;
                        emergencyPort = port;
                    }
                    log("RESOLVED: " + name + " at " + host + ":" + port);
                }
            };

            jmdns.addServiceListener("_machine-monitor._tcp.local.", commonListener);
            jmdns.addServiceListener("_production-manager._tcp.local.", commonListener);
            jmdns.addServiceListener("_emergency-center._tcp.local.", commonListener);
        } 
        catch (IOException e) {
            log("JmDNS Error: " + e.getMessage());
        }
    }

    private void checkStatus() {
        if (monitorHost == null) {
            log("Error: Monitor service not found.");
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(monitorHost, monitorPort).usePlaintext().build();
        try {
            MachineMonitorGrpc.MachineMonitorBlockingStub stub = MachineMonitorGrpc.newBlockingStub(channel);
            StatusResponse resp = stub.checkMachineStatus(StatusRequest.newBuilder().setMachineId(machineIdInput.getText()).build());
            log("SERVER: " + resp.getDescription() + " (Active: " + resp.getIsActive() + ")");
        } 
        finally {
            channel.shutdown();
        }
    }

    private void streamTelemetry() {
        if (monitorHost == null) {
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(monitorHost, monitorPort).usePlaintext().build();
        MachineMonitorGrpc.MachineMonitorStub stub = MachineMonitorGrpc.newStub(channel);
        stub.streamSensorData(MonitorRequest.newBuilder().setMachineId(machineIdInput.getText()).build(), new StreamObserver<SensorData>() {
            @Override
            public void onNext(SensorData d) {
                log(String.format("TELEMETRY: Temp %.2fC | Load %.2f%%", d.getTemperature(), d.getPerformanceLoad()));
            }

            @Override
            public void onError(Throwable t) {
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                log("Telemetry stream ended.");
                channel.shutdown();
            }
        });
    }

    private void uploadProduction() {
        if (productionHost == null) {
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(productionHost, productionPort).usePlaintext().build();
        ProductionManagerGrpc.ProductionManagerStub stub = ProductionManagerGrpc.newStub(channel);
        StreamObserver<LogEntry> request = stub.uploadProductionLog(new StreamObserver<ProductionSummary>() {
            @Override
            public void onNext(ProductionSummary s) {
                log("SUMMARY: Processed " + s.getTotalProcessed() + " items.");
            }

            @Override
            public void onError(Throwable t) {
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });
        request.onNext(LogEntry.newBuilder().setProductId("Batch-01").setQualityScore(88).build());
        request.onCompleted();
    }

    private void triggerEmergency() {
        if (emergencyHost == null) {
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(emergencyHost, emergencyPort).usePlaintext().build();
        EmergencyCenterGrpc.EmergencyCenterStub stub = EmergencyCenterGrpc.newStub(channel);
        StreamObserver<AlertMessage> request = stub.emergencyChannel(new StreamObserver<AlertMessage>() {
            @Override
            public void onNext(AlertMessage m) {
                log("EMERGENCY RESPONSE: " + m.getText());
            }

            @Override
            public void onError(Throwable t) {
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });
        request.onNext(AlertMessage.newBuilder().setUser("Marcos").setText("CRITICAL ALERT").setPriority(1).build());
        request.onCompleted();
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> consoleLog.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IndustrialGUI().setVisible(true));
    }
}
