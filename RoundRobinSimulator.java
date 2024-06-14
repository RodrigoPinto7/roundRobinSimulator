import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

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
            SystemInterrupt currentSystemInterrupt = null;
            int systemInterruptRemaining = 0;

            while (!readyQueue.isEmpty() || !blockedQueue.isEmpty() || systemInterruptRemaining > 0) {
                if (currentSystemInterrupt != null && systemInterruptRemaining > 0) {
                    for (int i = 0; i < quantum && systemInterruptRemaining > 0; i++) {
                        result.append("Time ").append(time).append(": System interrupt executing\n");
                        time++;
                        systemInterruptRemaining--;
                        if (systemInterruptRemaining > 0 && i < quantum - 1) {
                            result.append("Time ").append(time).append(": Delay\n");
                            time++;
                        }
                    }
                    if (systemInterruptRemaining <= 0) {
                        currentSystemInterrupt = null;
                    }
                    continue;
                }

                if (currentSystemInterrupt == null && !systemInterrupts.isEmpty()) {
                    for (SystemInterrupt si : systemInterrupts) {
                        if (si.time == time) {
                            currentSystemInterrupt = si;
                            systemInterruptRemaining = si.duration;
                            break;
                        }
                    }
                }

                Process process = readyQueue.poll();

                if (process != null) {
                    if (process.quantumUsed >= process.interruptTime && !process.interrupted) {
                        process.interrupted = true;
                        result.append("Time ").append(time).append(": Process ").append(process.name)
                              .append(" interrupted for ").append(process.interruptDuration).append(" units\n");

                        int interruptRemaining = process.interruptDuration;
                        while (interruptRemaining > 0) {
                            int interruptQuantum = Math.min(interruptRemaining, quantum);
                            for (int i = 0; i < interruptQuantum; i++) {
                                result.append("Time ").append(time).append(": Process ").append(process.name)
                                      .append(" interrupt executing\n");
                                time++;
                                interruptRemaining--;
                                if (interruptRemaining > 0 && i < interruptQuantum - 1) {
                                    result.append("Time ").append(time).append(": Delay\n");
                                    time++;
                                }
                            }
                        }

                        blockedQueue.add(process);
                        continue;
                    }

                    if (process.remainingTime > 0) {
                        result.append("Time ").append(time).append(": Process ").append(process.name).append(" is running\n");

                        int quantumRemaining = quantum;
                        while (quantumRemaining > 0 && process.remainingTime > 0) {
                            result.append("Time ").append(time).append(": Process ").append(process.name).append(" is running\n");
                            time++;
                            quantumRemaining--;
                            process.remainingTime--;
                            process.quantumUsed++;
                            if (quantumRemaining > 0 && process.remainingTime > 0) {
                                result.append("Time ").append(time).append(": Delay\n");
                                time++;
                            }
                        }

                        if (process.remainingTime > 0) {
                            readyQueue.add(process);
                        } else {
                            finishedProcesses.add(process.name);
                            result.append("Process ").append(process.name).append(" finished at time ").append(time).append("\n");
                        }
                    }
                }

                if (!blockedQueue.isEmpty()) {
                    readyQueue.addAll(blockedQueue);
                    blockedQueue.clear();
                }

                if (currentSystemInterrupt == null && readyQueue.isEmpty() && blockedQueue.isEmpty()) {
                    time++;
                    result.append("Time ").append(time).append(": Idle\n");
                }
            }

            outputArea.setText(result.toString());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter valid quantum and delay values.");
        }
    }
}



