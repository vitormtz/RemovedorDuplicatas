/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.removedorduplicatas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFileChooser;
import java.util.*;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;


/**
 *
 * @author vitor
 */
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class RemovedorDuplicatas extends javax.swing.JFrame {

    private JTextArea logArea;
    private JCheckBox chkSort; // Novo componente

    public RemovedorDuplicatas() {
        initComponents();
    }

    private void initComponents() {
        btnSelectFile = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();
        logArea = new javax.swing.JTextArea();
        chkSort = new javax.swing.JCheckBox("Ordenar linhas alfabeticamente"); // Novo componente
        JScrollPane scrollPane = new JScrollPane(logArea);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Remove Linhas Duplicadas");

        btnSelectFile.setText("Selecionar Arquivo");
        btnSelectFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectFileActionPerformed(evt);
            }
        });

        lblStatus.setText("Selecione um arquivo para começar");

        logArea.setEditable(false);
        logArea.setRows(10);
        logArea.setColumns(40);
        logArea.setWrapStyleWord(true);
        logArea.setLineWrap(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnSelectFile)
                                        .addComponent(chkSort)
                                        .addComponent(lblStatus)
                                        .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnSelectFile)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(chkSort)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblStatus)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }

    private void btnSelectFileActionPerformed(java.awt.event.ActionEvent evt) {
        // ... (código do seletor de arquivo permanece o mesmo)
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String[] textExtensions = {
                    "txt", "log", "csv", "md", "rtf", "text",
                    "json", "xml", "html", "htm", "css", "js",
                    "java", "py", "c", "cpp", "h", "ini", "cfg",
                    "properties", "yml", "yaml", "conf"
                };

                String fileName = f.getName().toLowerCase();
                for (String ext : textExtensions) {
                    if (fileName.endsWith("." + ext)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Arquivos de texto";
            }
        });

        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();
            try {
                ProcessResult result = processFile(inputFile, chkSort.isSelected());
                lblStatus.setText("Arquivo processado com sucesso! "
                        + result.getDuplicatesCount() + " linha(s) duplicada(s) encontrada(s).");

                StringBuilder log = new StringBuilder();
                log.append("=== Relatório de Processamento ===\n\n");
                log.append("Total de linhas originais: ").append(result.getTotalLines()).append("\n");
                log.append("Total de linhas únicas: ").append(result.getUniqueLines()).append("\n");
                log.append("Total de duplicatas removidas: ").append(result.getDuplicatesCount()).append("\n\n");

                if (!result.getDuplicates().isEmpty()) {
                    log.append("Linhas duplicadas encontradas:\n");
                    for (Map.Entry<String, Integer> entry : result.getDuplicates().entrySet()) {
                        log.append("- \"").append(entry.getKey()).append("\" (")
                                .append(entry.getValue()).append(" ocorrências)\n");
                    }
                }

                logArea.setText(log.toString());

            } catch (IOException e) {
                lblStatus.setText("Erro ao processar arquivo: " + e.getMessage());
                logArea.setText("Erro: " + e.getMessage());
            }
        }
    }

    private class ProcessResult {

        private final int totalLines;
        private final int uniqueLines;
        private final Map<String, Integer> duplicates;

        public ProcessResult(int totalLines, int uniqueLines, Map<String, Integer> duplicates) {
            this.totalLines = totalLines;
            this.uniqueLines = uniqueLines;
            this.duplicates = duplicates;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public int getUniqueLines() {
            return uniqueLines;
        }

        public int getDuplicatesCount() {
            return totalLines - uniqueLines;
        }

        public Map<String, Integer> getDuplicates() {
            return duplicates;
        }
    }

    private ProcessResult processFile(File inputFile, boolean sort) throws IOException {
        Map<String, String> originalToStripped = new HashMap<>();
        Map<String, Integer> duplicateCount = new HashMap<>();
        Set<String> uniqueStrippedLines = new HashSet<>();
        List<String> linesToWrite = new ArrayList<>();
        int totalLines = 0;

        // Primeira passagem: conta linhas e identifica duplicatas
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                String strippedLine = stripCharacters(line);
                originalToStripped.put(line, strippedLine);

                if (!uniqueStrippedLines.add(strippedLine)) {
                    // É uma duplicata
                    duplicateCount.merge(line, 1, Integer::sum);
                } else {
                    // Se não é duplicata, adiciona à lista de linhas para escrever
                    linesToWrite.add(line);
                }
            }
        }

        // Ordena as linhas se necessário
        if (sort) {
            Collections.sort(linesToWrite);
        }

        // Cria o arquivo de saída
        String outputPath = inputFile.getParent() + File.separator
                + "processado_" + inputFile.getName();

        // Escreve as linhas no arquivo
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (String line : linesToWrite) {
                writer.write(line);
                writer.newLine();
            }
        }

        return new ProcessResult(totalLines, uniqueStrippedLines.size(), duplicateCount);
    }

    private String stripCharacters(String line) {
        return line.replace("\"", "").replace(":", "").trim();
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RemovedorDuplicatas.class.getName())
                    .log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new RemovedorDuplicatas().setVisible(true);
        });
    }

    private javax.swing.JButton btnSelectFile;
    private javax.swing.JLabel lblStatus;
}
