import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.Queue;

class Process {
    String name;
    int arrivalTime;
    int burstTime;
    int remainingTime;
    int interruptTime;
    int interruptDuration;
    boolean interrupted;
    int quantumUsed;

    Process(String name, int arrivalTime, int burstTime, int interruptTime, int interruptDuration) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.interruptTime = interruptTime;
        this.interruptDuration = interruptDuration;
        this.interrupted = false;
        this.quantumUsed = 0;
    }
}

class SystemInterrupt {
    int time;
    int duration;

    SystemInterrupt(int time, int duration) {
        this.time = time;
        this.duration = duration;
    }
}

public class RoundRobinSimulator {
    private static final int MAX_PROCESSES = 7;

    private JFrame frame;
    private JTextField quantumField;
    private JTextField delayField;
    private JTable processTable;
    private DefaultTableModel tableModel;
    private JButton addProcessButton;
    private JButton simulateButton;
    private JTextArea outputArea;
    private ArrayList<SystemInterrupt> systemInterrupts = new ArrayList<>();

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                RoundRobinSimulator window = new RoundRobinSimulator();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public RoundRobinSimulator() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Panel para los campos de entrada
        JPanel inputPanel = new JPanel();
        frame.getContentPane().add(inputPanel, BorderLayout.NORTH);

        JLabel lblQuantum = new JLabel("Quantum:");
        inputPanel.add(lblQuantum);

        quantumField = new JTextField();
        inputPanel.add(quantumField);
        quantumField.setColumns(5);

        JLabel lblDelay = new JLabel("Delay:");
        inputPanel.add(lblDelay);

        delayField = new JTextField();
        inputPanel.add(delayField);
        delayField.setColumns(5);

        addProcessButton = new JButton("Add Process");
        inputPanel.add(addProcessButton);

        JButton addSystemInterruptButton = new JButton("Add System Interrupt");
        inputPanel.add(addSystemInterruptButton);

        simulateButton = new JButton("Simulate");
        inputPanel.add(simulateButton);

        JScrollPane scrollPane = new JScrollPane();
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        processTable = new JTable();
        tableModel = new DefaultTableModel(new Object[]{"Process", "Arrival Time", "Burst Time", "Interrupt Time", "Interrupt Duration"}, 0);
        processTable.setModel(tableModel);
        scrollPane.setViewportView(processTable);

        outputArea = new JTextArea();
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        frame.getContentPane().add(outputScrollPane, BorderLayout.SOUTH);

        addProcessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addProcess();
            }
        });

        addSystemInterruptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSystemInterrupt();
            }
        });

        simulateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateRoundRobin();
            }
        });
    }

    private void addProcess() {
        if (tableModel.getRowCount() < MAX_PROCESSES) {
            String name = JOptionPane.showInputDialog("Enter process name:");
            int arrivalTime = Integer.parseInt(JOptionPane.showInputDialog("Enter arrival time:"));
            int burstTime = Integer.parseInt(JOptionPane.showInputDialog("Enter burst time:"));
            int interruptTime = Integer.parseInt(JOptionPane.showInputDialog("Enter interrupt time:"));
            int interruptDuration = Integer.parseInt(JOptionPane.showInputDialog("Enter interrupt duration:"));

            tableModel.addRow(new Object[]{name, arrivalTime, burstTime, interruptTime, interruptDuration});
        } else {
            JOptionPane.showMessageDialog(frame, "Maximum number of processes reached.");
        }
    }

    private void addSystemInterrupt() {
        int interruptTime = Integer.parseInt(JOptionPane.showInputDialog("Enter system interrupt time:"));
        int interruptDuration = Integer.parseInt(JOptionPane.showInputDialog("Enter system interrupt duration:"));
        systemInterrupts.add(new SystemInterrupt(interruptTime, interruptDuration));
    }

    private void simulateRoundRobin() {
        try {
            int quantum = Integer.parseInt(quantumField.getText());
            int delay = Integer.parseInt(delayField.getText());
    
            Queue<Process> queue = new LinkedList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                int arrivalTime = (int) tableModel.getValueAt(i, 1);
                int burstTime = (int) tableModel.getValueAt(i, 2);
                int interruptTime = (int) tableModel.getValueAt(i, 3);
                int interruptDuration = (int) tableModel.getValueAt(i, 4);
                queue.add(new Process(name, arrivalTime, burstTime, interruptTime, interruptDuration));
            }
    
            int time = 0;
            StringBuilder result = new StringBuilder();
            Queue<Process> readyQueue = new LinkedList<>(queue);
            Queue<Process> blockedQueue = new LinkedList<>();
            Set<String> finishedProcesses = new HashSet<>();
            Process currentProcess = null;
            int executedUnits = 0;
    
            // Aplicar el delay inicial (overhead)
            for (int i = 0; i < delay; i++) {
                result.append("Time ").append(time).append(": Delay\n");
                time++;
            }
    
            while (!readyQueue.isEmpty() || !blockedQueue.isEmpty() || currentProcess != null) {
                // Procesar interrupciones
                if (currentProcess != null && currentProcess.interrupted) {
                    int interruptTimeRemaining = currentProcess.interruptDuration;
                    while (interruptTimeRemaining > 0) {
                        result.append("Time ").append(time).append(": Process ").append(currentProcess.name)
                              .append(" interrupt\n");
                        time++;
                        interruptTimeRemaining--;
                        executedUnits++;
                        if (executedUnits == 2) {
                            result.append("Time ").append(time).append(": Delay\n");
                            time++;
                            executedUnits = 0;
                        }
                    }
                    blockedQueue.add(currentProcess);
                    currentProcess = null;
                    continue;
                }
    
                // Mover procesos de la cola de bloqueados a la cola de listos
                if (!blockedQueue.isEmpty()) {
                    readyQueue.addAll(blockedQueue);
                    blockedQueue.clear();
                }
    
                // Procesar el siguiente proceso en la cola de listos
                if (currentProcess == null && !readyQueue.isEmpty()) {
                    currentProcess = readyQueue.poll();
                }
    
                if (currentProcess != null) {
                    int quantumRemaining = quantum;
                    while (quantumRemaining > 0 && currentProcess.remainingTime > 0) {
                        result.append("Time ").append(time).append(": Process ").append(currentProcess.name).append(" is running\n");
                        time++;
                        quantumRemaining--;
                        currentProcess.remainingTime--;
                        currentProcess.quantumUsed++;
                        executedUnits++;
    
                        // Verificar si el proceso necesita ser interrumpido durante el quantum
                        if (currentProcess.quantumUsed >= currentProcess.interruptTime && !currentProcess.interrupted) {
                            currentProcess.interrupted = true;
                            break;
                        }
    
                        if (executedUnits == 2) {
                            result.append("Time ").append(time).append(": Delay\n");
                            time++;
                            executedUnits = 0;
                        }
                    }
    
                    // Si el proceso no ha terminado, volver a añadirlo a la cola de listos
                    if (currentProcess.remainingTime > 0 && !currentProcess.interrupted) {
                        readyQueue.add(currentProcess);
                    } else if (currentProcess.remainingTime == 0) {
                        finishedProcesses.add(currentProcess.name);
                        result.append("Process ").append(currentProcess.name).append(" finished at time ").append(time).append("\n");
                        currentProcess = null;
                    }
                }
    
                // Verificar si el sistema está ocioso
                if (currentProcess == null && readyQueue.isEmpty() && blockedQueue.isEmpty()) {
                    result.append("Time ").append(time).append(": Idle\n");
                    time++;
                }
            }
    
            outputArea.setText(result.toString());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter valid numbers for quantum and delay.");
        }
    }       
}
