/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.cuni.mff.hurkovalu.publication_search;

import cz.cuni.mff.hurkovalu.publication_search.aggregation.Filters;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class representing a dialog for search filters selection.
 * @author Lucie Hurkova
 */
public class FilterDialog {

    private JDialog dialog;
    private Filters filters;
    private JTextField authorField;
    private JTextField journalFiled;
    private JTextField yearsStart;
    private JTextField yearsEnd;
    private JButton applyButton;
    private GUI gui;
    
    private boolean authorCorrect = true;
    private boolean journalCorrect = true;
    private boolean startCorrect = true;
    private boolean endCorrect = true;

    /**
     * Creates a new {@link FilterDialog} with given filter options, dialog title, parent frame and parent GUI.
     * @param filters filter options
     * @param title title of the dialog
     * @param homeFrame parent frame
     * @param gui parent GUI
     */
    public FilterDialog(Filters filters, String title, Frame homeFrame, GUI gui) {
        this.filters = filters;
        this.gui = gui;
        dialog = new JDialog(homeFrame, title);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setPreferredSize(new Dimension(300, 300));
        dialog.setSize(300, 300);
        dialog.setResizable(false);

        JPanel authorPanel = new JPanel();
        authorPanel.setLayout(new BoxLayout(authorPanel, BoxLayout.X_AXIS));
        JLabel authorLabel = new JLabel("Author:  ");
        authorField = new JTextField();
        authorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateAuthor();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateAuthor();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateAuthor();
            }
        });
        authorPanel.add(authorLabel);
        authorPanel.add(authorField);
        authorPanel.setBorder(new EmptyBorder(10, 10, 20, 10));

        JPanel journalPanel = new JPanel();
        journalPanel.setLayout(new BoxLayout(journalPanel, BoxLayout.X_AXIS));
        JLabel journalLabel = new JLabel("Journal:  ");
        journalFiled = new JTextField();
        journalFiled.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateJournal();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateJournal();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateJournal();
            }
        });
        journalPanel.add(journalLabel);
        journalPanel.add(journalFiled);
        journalPanel.setBorder(new EmptyBorder(10, 10, 20, 10));

        Filters.TimeRange validYears = this.filters.getTimeRange();

        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
        JLabel startLable = new JLabel("From:    ");
        yearsStart = new JTextField();
        yearsStart.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateStart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateStart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateStart();
            }
        });
        startPanel.add(startLable);
        startPanel.add(yearsStart);
        startPanel.setBorder(new EmptyBorder(10, 10, 20, 10));

        JPanel endPanel = new JPanel();
        endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.X_AXIS));
        JLabel endLable = new JLabel("To:       ");
        yearsEnd = new JTextField();
        yearsEnd.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateEnd();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateEnd();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateEnd();
            }
        });
        endPanel.add(endLable);
        endPanel.add(yearsEnd);
        endPanel.setBorder(new EmptyBorder(10, 10, 20, 10));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> apply());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> deleteAllContent());
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.add(applyButton);
        buttonPanel.add(clearButton);

        dialog.add(authorPanel);
        dialog.add(journalPanel);
        dialog.add(startPanel);
        dialog.add(endPanel);
        dialog.add(buttonPanel);
    }
    
    private void apply() {
        setVisible(false);
        int start = filters.getTimeRange().start();
        int end = filters.getTimeRange().end();
        try {
            start = Integer.parseInt(yearsStart.getText());
        } catch (NumberFormatException e) {}
        try {
            end = Integer.parseInt(yearsEnd.getText());
        } catch (NumberFormatException e) {}
        
        Filters.Filter filter = filters.createFilter(authorField.getText(), journalFiled.getText(), new Filters.TimeRange(start, end));
        gui.setCurrentFilter(filter);
    }
    
    private void deleteAllContent() {
        authorField.setText("");
        journalFiled.setText("");
        yearsStart.setText("");
        yearsEnd.setText("");
        gui.setCurrentFilter(filters.createEmptyFilter());
    }

    private void validateJournal() {
        if (filters.isJournalValid(journalFiled.getText()) || "".equals(journalFiled.getText())) {
            journalFiled.setForeground(Color.BLACK);
            journalCorrect = true;
        } else {
            journalFiled.setForeground(Color.RED);
            journalCorrect = false;
        }
        changeButtonState();
    }

    private void validateAuthor() {
        if (filters.isAuthorValid(authorField.getText()) || "".equals(authorField.getText())) {
            authorField.setForeground(Color.BLACK);
            authorCorrect = true;
        } else {
            authorField.setForeground(Color.RED);
            authorCorrect = false;
        }
        changeButtonState();
    }
    
    private boolean isStartValid() {
        Filters.TimeRange validYears = this.filters.getTimeRange();
        try {
            int year = Integer.parseInt(yearsStart.getText());
            return year >= validYears.start();
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    
    private boolean isEndValid() {
        Filters.TimeRange validYears = this.filters.getTimeRange();
        try {
            int year = Integer.parseInt(yearsEnd.getText());
            return year <= validYears.end();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void validateStart() {
        if (!"".equals(yearsStart.getText())) {
            if (isStartValid()) {
                if (isEndValid()) {
                    int start = Integer.parseInt(yearsStart.getText());
                    int end = Integer.parseInt(yearsEnd.getText());
                    if (start <= end) {
                        yearsStart.setForeground(Color.BLACK);
                        yearsEnd.setForeground(Color.BLACK);
                        startCorrect = true;
                        endCorrect = true;
                    } else {
                        yearsStart.setForeground(Color.RED);
                        startCorrect = false;
                    }
                } else {
                    yearsStart.setForeground(Color.BLACK);
                    startCorrect = true;
                }
            } else {
                yearsStart.setForeground(Color.RED);
                startCorrect = false;
            }
        } else {
            yearsStart.setForeground(Color.BLACK);
            startCorrect = true;
        }
        changeButtonState();
    }
    
    private void validateEnd() {
        if (!"".equals(yearsEnd.getText())) {
            if (isEndValid()) {
                if (isStartValid()) {
                    int start = Integer.parseInt(yearsStart.getText());
                    int end = Integer.parseInt(yearsEnd.getText());
                    if (start <= end) {
                        yearsEnd.setForeground(Color.BLACK);
                        yearsStart.setForeground(Color.BLACK);
                        endCorrect = true;
                        startCorrect = true;
                    } else {
                        yearsEnd.setForeground(Color.RED);
                        endCorrect = false;
                    }
                } else {
                    yearsEnd.setForeground(Color.BLACK);
                    endCorrect = true;
                }
            } else {
                yearsEnd.setForeground(Color.RED);
                endCorrect = false;
            }
        } else {
            yearsEnd.setForeground(Color.BLACK);
            endCorrect = true;
        }
        changeButtonState();
    }
    
    /**
     * Sets visibility of the dialog.
     * @param b visibility
     */
    public void setVisible(boolean b) {
        dialog.setLocationRelativeTo(dialog.getParent());
        dialog.setVisible(b);
    }
    
    private void changeButtonState() {
        applyButton.setEnabled(authorCorrect && journalCorrect && startCorrect && endCorrect);
    }
        

}
