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

import cz.cuni.mff.hurkovalu.preprocessing.Preprocessing;
import cz.cuni.mff.hurkovalu.publication_search.Author;
import cz.cuni.mff.hurkovalu.publication_search.Publication;
import cz.cuni.mff.hurkovalu.publication_search.models.LSIModel;
import cz.cuni.mff.hurkovalu.publication_search.models.Model;
import cz.cuni.mff.hurkovalu.publication_search.aggregation.TopKOperator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Lucie Hurkova
 */
public class GUI {

    private JFrame frame;
    private String name;
    private int sizeX;
    private int sizeY;
    private JButton searchButton;
    private JTextField searchField;
    private float fontSize = 16;
    private JPanel mainPanel;
    private JScrollPane scrollFrame;

    private Model model;
    private List<Publication> publications;

    private static String OPEN_LINK = "open";
    private static String HEADER_TEMPLATE = """
                                          <!DOCTYPE html>
                                          <html>
                                          <h3><a href="https://pubmed.ncbi.nlm.nih.gov/%d/">%s</a></h3>
                                          <p><b>%s</b></p>
                                          <p><b>%s, %d</b></p>
                                          <p><b>doi:</b> %s</p>
                                          </html>                               
                                          """;
    private static String ABSTRACT_TEMPLATE = """
                                          <!DOCTYPE html>
                                          <html>
                                          <p>%s</p>
                                          </html>                            
                                          """;

    public GUI(int sizeX, int sizeY, String name) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.name = name;
    }

    public void createGUI() {
        //System.setProperty("apple.laf.useScreenMenuBar", "true");
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(20000);

        loadPublications();

        frame = new JFrame(name);
        frame.setLocation(20, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        URL resource = getClass().getResource("icon.png");
//        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(resource));
        searchButton = new JButton("Search");
        searchButton.setFont(searchButton.getFont().deriveFont(fontSize));
        searchButton.addActionListener(e -> search());

        searchField = new JTextField();
        searchField.setFont(searchField.getFont().deriveFont(fontSize));
        searchField.setMaximumSize(new Dimension(sizeX, searchField.getPreferredSize().height));
        searchField.addActionListener(e -> search());

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));
        searchPanel.setBorder(new EmptyBorder(0, 50, 0, 50));
        searchPanel.setPreferredSize(new Dimension(sizeX, 50));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        scrollFrame = new JScrollPane(mainPanel);
        scrollFrame.setPreferredSize(new Dimension(sizeX, sizeY));

        frame.getContentPane().add(searchPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollFrame, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void search() {
        String query = searchField.getText();
        model.matchQuery(query);
        TopKOperator aggregation = new TopKOperator(publications);
        List<Publication> topK = aggregation.getTopK(10);
        mainPanel.removeAll();
        for (Publication p : topK) {
            mainPanel.add(createResult(p));
        }
        mainPanel.setPreferredSize(mainPanel.getMaximumSize());
        mainPanel.invalidate();
        mainPanel.revalidate();
        mainPanel.repaint();
        SwingUtilities.invokeLater(() -> scrollFrame.getVerticalScrollBar().setValue(0));
    }

    private void loadPublications() {
        Path directory = Paths.get("..");
        Preprocessing preprocessing = new Preprocessing();
        publications = preprocessing.processDirectory(directory);
        model = new LSIModel(publications);
        model.processPublications();
        System.out.println("Publications Loaded");
    }

    private JComponent createResult(Publication publication) {
        List<Author> authors = publication.getAuthors();
        StringBuilder authorsString = new StringBuilder();
        for (Author a : authors) {
            authorsString.append(a.lastName()).append(" ");
            for (String n : a.foreNames()) {
                authorsString.append(n);
            }
            authorsString.append(", ");
        }
        authorsString.delete(authorsString.length() - 2, authorsString.length());
        String doi = publication.getDoi();
        if (doi == null) {
            doi = "-";
        }
        JPanel articlePanel = new JPanel();
        articlePanel.setLayout(new BoxLayout(articlePanel, BoxLayout.PAGE_AXIS));
        articlePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
        articlePanel.setBackground(Color.WHITE);
        String content = String.format(HEADER_TEMPLATE, publication.getId(),
                publication.getTitle(), authorsString.toString(),
                publication.getJournal(), publication.getYear(), doi);
        JEditorPane header = new JEditorPane();
        header.setContentType("text/html");
        header.setText(content);
        header.setEditable(false);

        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setSpaceBelow(a, .5f);
        StyleConstants.setSpaceAbove(a, 0);
        StyledDocument document = (StyledDocument) header.getDocument();
        document.setParagraphAttributes(0, document.getLength(), a, false);

        header.setSize(new Dimension(sizeX, 2000));
        header.setMaximumSize(new Dimension(sizeX, header.getPreferredSize().height));
        header.setPreferredSize(header.getMaximumSize());
        header.setBorder(new EmptyBorder(1, 1, 1, 1));
        header.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }

        });

        JEditorPane articleAbstract = new JEditorPane();
        articleAbstract.setContentType("text/html");
        articleAbstract.setText(String.format(ABSTRACT_TEMPLATE, publication.getPubAbstract()));
        articleAbstract.setEditable(false);
        StyledDocument document2 = (StyledDocument) articleAbstract.getDocument();
        document2.setParagraphAttributes(0, document.getLength(), a, false);
        articleAbstract.setBorder(new EmptyBorder(1,1,1,1));

        articleAbstract.setSize(new Dimension(sizeX, 1000));
        int height = articleAbstract.getPreferredSize().height;
        articleAbstract.setMaximumSize(new Dimension(sizeX, height));

        if (height > 100) {
            int pos = articleAbstract.getUI().viewToModel2D(articleAbstract, new Point(sizeX, 100), new Position.Bias[1]);
            String shortAbstract = publication.getPubAbstract().substring(0, pos + 1);
            if (shortAbstract.length() < publication.getPubAbstract().trim().length()) {
                int end = shortAbstract.lastIndexOf(' ');
                while ((pos - end) < 4) {
                    end = shortAbstract.substring(0, end).lastIndexOf(' ');
                }

                shortAbstract = shortAbstract.substring(0, end) + " \u2026";
                articleAbstract.setText(String.format(ABSTRACT_TEMPLATE, shortAbstract));
                document2 = (StyledDocument) articleAbstract.getDocument();
                document2.setParagraphAttributes(0, document.getLength(), a, false);
                articleAbstract.setSize(new Dimension(sizeX, 1000));
                articleAbstract.setMaximumSize(new Dimension(sizeX, articleAbstract.getPreferredSize().height));
                
                articleAbstract.setToolTipText("Double click to view the complete abstract");
                
                articleAbstract.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent mouseEvent) {
                        mouseEvent.getSource();
                        if (mouseEvent.getClickCount() == 2) {
                            StringBuilder title = new StringBuilder(publication.getAuthors().getFirst().lastName());
                            if (publication.getAuthors().size() > 1) {
                                title.append(" et al.");
                            }
                            title.append(" ").append("(").append(publication.getYear()).append(")");
                            JDialog dialog = new JDialog(frame, title.toString());
                            JEditorPane fullAbstract = new JEditorPane();
                            fullAbstract.setContentType("text/html");
                            fullAbstract.setText(String.format(ABSTRACT_TEMPLATE, publication.getPubAbstract()));
                            fullAbstract.setEditable(false);
                            StyledDocument document3 = (StyledDocument) fullAbstract.getDocument();
                            document3.setParagraphAttributes(0, document.getLength(), a, false);
                            fullAbstract.setBorder(new EmptyBorder(10, 10, 10, 10));
                            fullAbstract.setSize(new Dimension(sizeX/2, 1000));
                            articleAbstract.setMaximumSize(new Dimension(sizeX/2, articleAbstract.getPreferredSize().height));
                            dialog.add(fullAbstract);
                            dialog.setSize(new Dimension(sizeX/2, articleAbstract.getPreferredSize().height));
                            dialog.setVisible(true);
                        }
                    }
                });
            }
        }
        
        articleAbstract.setPreferredSize(articleAbstract.getMaximumSize());

        articlePanel.add(header);
        articlePanel.add(articleAbstract);

        return articlePanel;
    }

}
