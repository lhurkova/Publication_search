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
import cz.cuni.mff.hurkovalu.publication_search.aggregation.Aggregation;
import cz.cuni.mff.hurkovalu.publication_search.aggregation.Filters;
import cz.cuni.mff.hurkovalu.publication_search.models.LSIModel;
import cz.cuni.mff.hurkovalu.publication_search.models.Model;
import cz.cuni.mff.hurkovalu.publication_search.models.TFIDFModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
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
 * Class representing the graphical user interface of the PubMed Search app.
 * @author Lucie Hurkova
 */
public class GUI {
    
    private static final Logger LOGGER = Logger.getLogger(GUI.class.getName());
    
    private JFrame frame;
    private String name;
    private int sizeX;
    private int sizeY;
    private JButton searchButton;
    private JTextField searchField;
    private float fontSize = 16;
    private JPanel mainPanel;
    private JScrollPane scrollFrame;
    private JPanel noResultsPanel;
    private JTextField authorField;
    private JTextField journalField;
    private JSlider yearsStart;
    private JSlider yearsEnd;
    private JRadioButtonMenuItem tfidfItem;
    private JRadioButtonMenuItem lsiItem;
    private JMenuItem filterItem;
    private JMenu modelIntem;
    private JLabel filtersLabel;
    private JProgressBar bar;
    private JLabel barLabel;
    private Timer  progressTimer;    
    private FilterDialog mainFilter;
    private Filters filters;
    private Filters.Filter currFilter;

    private Model currModel;
    private LSIModel lsiModel;
    private TFIDFModel tfidfModel;
    private List<Publication> publications;
    
    private Path dataDirectory;
    private Path serializationDirectory;
    private Path svdsPath;

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

    /**
     * Creates a new instance of the {@link GUI}
     * @param sizeX width of the window
     * @param sizeY height of the window
     * @param name name of the window
     * @param dataDirectory directory containing database XML files
     * @param serializationDirectory directory containing serialized database or directory for future serialization of the database
     * @param svdsPath name of compiled C program for computation of SVD for LSI model
     */
    public GUI(int sizeX, int sizeY, String name, Path dataDirectory, Path serializationDirectory, Path svdsPath) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.name = name;
        this.dataDirectory = dataDirectory;
        this.serializationDirectory = serializationDirectory;
        this.svdsPath = svdsPath;
    }
    
    /**
     * Initializes all graphical components in the {@link GUI} and loads the database.
     */
    public void createGUI() {
        //System.setProperty("apple.laf.useScreenMenuBar", "true");
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(20000);


        frame = new JFrame(name);
        frame.setLocation(20, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        searchButton = new JButton("Search");
        searchButton.setFont(searchButton.getFont().deriveFont(fontSize));
        searchButton.addActionListener(e -> search());
        searchButton.setEnabled(false);

        searchField = new JTextField();
        searchField.setFont(searchField.getFont().deriveFont(fontSize));
        searchField.setMaximumSize(new Dimension(sizeX, searchField.getPreferredSize().height));
        searchField.addActionListener(e -> search());
        searchField.setEnabled(false);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));
        searchPanel.setBorder(new EmptyBorder(0, 50, 0, 50));
        searchPanel.setPreferredSize(new Dimension(sizeX, 50));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        JPanel filtersPanel = new JPanel(new BorderLayout());
        filtersPanel.setPreferredSize(new Dimension(sizeX, 30));
        filtersPanel.setBorder(new EmptyBorder(0, 50, 0, 50));
        filtersLabel = new JLabel("");
        filtersPanel.add(filtersLabel, BorderLayout.CENTER);
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(searchPanel);
        topPanel.add(filtersPanel);

        mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        scrollFrame = new JScrollPane(mainPanel);
        scrollFrame.setPreferredSize(new Dimension(sizeX+50, sizeY));
        
        noResultsPanel = new JPanel();
        noResultsPanel.setLayout(new GridBagLayout());
        noResultsPanel.setSize(new Dimension(sizeX, sizeY/2));
        noResultsPanel.setMaximumSize(new Dimension(sizeX, sizeY/2));
        noResultsPanel.setPreferredSize(new Dimension(sizeX, sizeY/2));
        noResultsPanel.setBackground(Color.WHITE);
        JLabel noResLabel = new JLabel("No results found");
        noResLabel.setFont(noResLabel.getFont().deriveFont(fontSize));
        noResultsPanel.add(noResLabel);
                
        createMenu();
        
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new GridBagLayout());
        JPanel innerProgressPanel = new JPanel();
        innerProgressPanel.setLayout(new BoxLayout(innerProgressPanel, BoxLayout.Y_AXIS));
        bar = new JProgressBar(0, 100);
        bar.setValue(0);
        barLabel = new JLabel("Starting...");
        innerProgressPanel.add(bar);
        innerProgressPanel.add(barLabel);
        progressPanel.add(innerProgressPanel);
        mainPanel.add(progressPanel);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollFrame, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        BackgroundTask backgroundTask = new BackgroundTask();
        backgroundTask.execute();
        progressTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int progress = backgroundTask.getCurrProgress();
                if (progress == -1) {
                    if (!bar.isIndeterminate()) {
                        bar.setIndeterminate(true);
                    }    
                } else {
                    if (bar.isIndeterminate()) {
                        bar.setIndeterminate(false);
                    }    
                    bar.setValue(progress);
                }
            }
        });
        progressTimer.setInitialDelay(100);
        progressTimer.start();
        
        
    }
    
    private void enableMenu() {
        if (lsiModel == null) {
            currModel = tfidfModel;
            tfidfItem.setSelected(true);
            lsiItem.setEnabled(false);
        }
        filterItem.setEnabled(true);
        modelIntem.setEnabled(true);
        searchField.setEnabled(true);
        searchButton.setEnabled(true);
    }
    
    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu applicationMenu = new JMenu("PubMed Search");
        JMenu filterMenu = new JMenu("Filter");
        JMenu modelMenu = new JMenu("Model");
        menuBar.add(applicationMenu);
        menuBar.add(filterMenu);
        menuBar.add(modelMenu);
        
        JMenuItem quitItem = new JMenuItem("Quit PubMed Search");
        applicationMenu.add(quitItem);
        quitItem.addActionListener(e -> System.exit(0));
        
        filterItem = new JMenuItem("Add filter...");
        filterMenu.add(filterItem);
        filterItem.addActionListener(e -> openFilterDialog());
        
        modelIntem = new JMenu("Select model");
        tfidfItem = new JRadioButtonMenuItem("TF-IDF");
        lsiItem = new JRadioButtonMenuItem("LSI", true);
        ButtonGroup group = new ButtonGroup();
        group.add(tfidfItem);
        group.add(lsiItem);
        
        lsiItem.addActionListener((ActionEvent e) -> {
            if (lsiItem.isSelected()) {
                currModel = lsiModel;
            }
        });
        
        tfidfItem.addActionListener((ActionEvent e) -> {
            if (tfidfItem.isSelected()) {
                currModel = tfidfModel;
            }
        });
        
        modelMenu.add(modelIntem);
        modelIntem.add(tfidfItem);
        modelIntem.add(lsiItem);
        
        filterItem.setEnabled(false);
        modelIntem.setEnabled(false);
        
        frame.setJMenuBar(menuBar);
    }
    
    private void openFilterDialog() {
        if (mainFilter == null) {
            mainFilter = new FilterDialog(filters, "Search filters", frame, this);
        }
        mainFilter.setVisible(true);
    }
        
    private void search() {
        String query = searchField.getText();
        currModel.matchQuery(query);
        Aggregation aggregation = new Aggregation(publications);
        List<Publication> topK = aggregation.getTopK(10, currFilter);
        mainPanel.removeAll();
        if (topK.isEmpty()) {
            mainPanel.add(noResultsPanel);
        } else {
            for (Publication p : topK) {
                mainPanel.add(createResult(p));
            }
        }
        mainPanel.setPreferredSize(mainPanel.getMaximumSize());
        mainPanel.invalidate();
        mainPanel.revalidate();
        mainPanel.repaint();
        SwingUtilities.invokeLater(() -> scrollFrame.getVerticalScrollBar().setValue(0));
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
                            LOGGER.log(Level.SEVERE, "createResult", e);
                        }
                    }
                }
            }

        });

        JEditorPane articleAbstract = new JEditorPane();
        articleAbstract.setContentType("text/html");
        String pubAbstract = publication.getPubAbstract();
        articleAbstract.setText(String.format(ABSTRACT_TEMPLATE, pubAbstract));
        articleAbstract.setEditable(false);
        StyledDocument document2 = (StyledDocument) articleAbstract.getDocument();
        document2.setParagraphAttributes(0, document.getLength(), a, false);
        articleAbstract.setBorder(new EmptyBorder(1,1,1,1));

        articleAbstract.setSize(new Dimension(sizeX, 1000));
        int height = articleAbstract.getPreferredSize().height;
        articleAbstract.setMaximumSize(new Dimension(sizeX, height));

        if (height > 100) {
            int pos = articleAbstract.getUI().viewToModel2D(articleAbstract, new Point(sizeX, 100), new Position.Bias[1]);
            String shortAbstract = pubAbstract.substring(0, Math.min(pos + 1, pubAbstract.length()));
            if (shortAbstract.length() < pubAbstract.trim().length()) {
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
                            fullAbstract.setText(String.format(ABSTRACT_TEMPLATE, pubAbstract));
                            fullAbstract.setEditable(false);
                            StyledDocument document3 = (StyledDocument) fullAbstract.getDocument();
                            document3.setParagraphAttributes(0, document.getLength(), a, false);
                            fullAbstract.setBorder(new EmptyBorder(10, 10, 10, 10));
                            fullAbstract.setSize(new Dimension(sizeX/2, 1000));
                            fullAbstract.setMaximumSize(new Dimension(sizeX/2, fullAbstract.getPreferredSize().height));
                            dialog.add(fullAbstract);
                            dialog.setSize(new Dimension(sizeX/2, fullAbstract.getPreferredSize().height+20));
                            dialog.setLocationRelativeTo(frame);
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
    
    /**
     * Sets {@link Filters.Filter} to be used for the next search.
     * @param filter filter for the next search
     */
    public void setCurrentFilter(Filters.Filter filter) {
        currFilter = filter;
        filtersLabel.setText(filter.toString());
    }

    
    private class BackgroundTask extends SwingWorker<Object, Object> {

        
        enum STATE {
            INIT(""),
            XML("Loading Database ..."),
            LSI("Initializing LSI model ..."),
            TFIDF("Initializing TF-IDF model ...");
            
            private final String text;
            private STATE(String t) {
                text = t;
            }       
            
        }
        Preprocessing preprocessing;
        STATE st = STATE.INIT;
        @Override
        protected Object doInBackground() throws Exception {
            preprocessing = new Preprocessing(dataDirectory, serializationDirectory);
            st = STATE.XML;
            setNewProgress(st);
            publications = preprocessing.processDirectory();
            filters = preprocessing.getFilters();
            currFilter = filters.createEmptyFilter();
            
            st = STATE.LSI;
            setNewProgress(st);
            lsiModel = LSIModel.loadFromFile(serializationDirectory, publications);
            if (lsiModel == null && svdsPath != null) {
                lsiModel = new LSIModel(publications, svdsPath);
                lsiModel.processPublications();
            }
            
            st = STATE.TFIDF;
            setNewProgress(st);
            tfidfModel = TFIDFModel.loadFromFile(serializationDirectory, publications);
            if (tfidfModel == null) {
                tfidfModel = new TFIDFModel(publications);
                tfidfModel.processPublications();
            }
            currModel = lsiModel;
            System.out.println("Publications Loaded");
            progressTimer.stop();
            serialize(serializationDirectory);
            return null;
        }

        @Override
        protected void done() {
            mainPanel.removeAll();
            mainPanel.invalidate();
            mainPanel.revalidate();
            mainPanel.repaint();
            enableMenu();
        }
        
        
        private void setNewProgress(STATE st) {
            SwingUtilities.invokeLater(() -> {bar.setValue(0); barLabel.setText(st.text);});
        }
            

        int getCurrProgress() {
            switch (st) {
                case INIT:
                    return 0;
                case XML:
                    return preprocessing.getProgress();
                case LSI:
                    return lsiModel == null ? -1 : lsiModel.getProgress();
                case TFIDF:
                    return tfidfModel == null ? -1 : tfidfModel.getProgress();
            }
            return 0;
        }
        
        private void serialize(Path directory) {
            preprocessing.storePublications();
            lsiModel.saveToFile(directory);
            tfidfModel.saveToFile(directory);
        }
        
        
    }
}
