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
//import industry.IndustryService.StatusRequest;
//import industry.IndustryService.StatusResponse;
//import industry.IndustryService.MonitorRequest;
//import industry.IndustryService.SensorData;
//import industry.MachineMonitorGrpc;

public class IndustrialGUI extends JFrame {

    private JTextArea consoleLog;
    private JTextField machineIdInput;
    private volatile String monitorHost;
    private volatile int monitorPort;

    public IndustrialGUI() {
        setTitle("Industrial Management System - Controller");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Machine ID:"));
        machineIdInput = new JTextField(10);
        topPanel.add(machineIdInput);

        JButton btnCheck = new JButton("Check Status");
        topPanel.add(btnCheck);

        JButton btnStream = new JButton("Monitor Telemetry");
        topPanel.add(btnStream);

        add(topPanel, BorderLayout.NORTH);

        consoleLog = new JTextArea();
        consoleLog.setEditable(false);
        consoleLog.setBackground(Color.BLACK);
        consoleLog.setForeground(Color.GREEN);
        add(new JScrollPane(consoleLog), BorderLayout.CENTER);

        new Thread(this::discoverServices).start();

        btnCheck.addActionListener(e -> {
            if (monitorHost != null) {
                String id = machineIdInput.getText();
                if (id.isEmpty()) {
                    log("Warning: Please enter a Machine ID.");
                    return;
                }

                log("System: Connecting to Monitor via gRPC...");

                ManagedChannel channel = ManagedChannelBuilder.forAddress(monitorHost, monitorPort)
                        .usePlaintext()
                        .build();

                try {
                    MachineMonitorGrpc.MachineMonitorBlockingStub stub = MachineMonitorGrpc.newBlockingStub(channel);
                    StatusRequest request = StatusRequest.newBuilder()
                            .setMachineId(id)
                            .build();
                    StatusResponse response = stub.checkMachineStatus(request);
                    log("SERVER RESPONSE: " + response.getDescription() + " (Active: " + response.getIsActive() + ")");
                } catch (Exception ex) {
                    log("gRPC Error: " + ex.getMessage());
                } finally {
                    channel.shutdown();
                }
            } else {
                log("Error: Machine Monitor Service not discovered yet!");
            }
        });
 
        btnStream.addActionListener(e -> {
            if (monitorHost != null) {
                log("System: Starting Telemetry Stream...");

                ManagedChannel channel = ManagedChannelBuilder.forAddress(monitorHost, monitorPort)
                        .usePlaintext().build();

                MachineMonitorGrpc.MachineMonitorStub asyncStub = MachineMonitorGrpc.newStub(channel);

                MonitorRequest request = MonitorRequest.newBuilder()
                        .setMachineId(machineIdInput.getText())
                        .build();

                asyncStub.streamSensorData(request, new StreamObserver<SensorData>() {
                    @Override
                    public void onNext(SensorData data) {
                        log(String.format("TELEMETRY [%s]: Temp: %.2f°C | Load: %.2f%%",
                                machineIdInput.getText(), data.getTemperature(), data.getPerformanceLoad()));
                    }

                    @Override
                    public void onError(Throwable t) {
                        log("Stream Error: " + t.getMessage());
                        channel.shutdown();
                    }

                    @Override
                    public void onCompleted() {
                        log("System: Stream completed by server.");
                        channel.shutdown();
                    }
                });
            } else {
                log("Error: Service not found.");
            }
        });
    }

    private void discoverServices() {
        try {
            log("System: Looking for services on network...");
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            jmdns.addServiceListener("_machine-monitor._tcp.local.", new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    log("Service found: " + event.getName());
                    jmdns.requestServiceInfo(event.getType(), event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    log("Service offline: " + event.getName());
                    monitorHost = null;
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    monitorHost = event.getInfo().getHostAddresses()[0];
                    monitorPort = event.getInfo().getPort();
                    log("RESOLVED: Machine Monitor at " + monitorHost + ":" + monitorPort);
                }
            });

        } catch (IOException e) {
            log("JmDNS Error: " + e.getMessage());
        }
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> consoleLog.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IndustrialGUI().setVisible(true));
    }
}
