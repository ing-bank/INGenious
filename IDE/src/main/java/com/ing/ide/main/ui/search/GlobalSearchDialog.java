package com.ing.ide.main.ui.search;

import com.ing.datalib.component.Project;
import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public class GlobalSearchDialog extends JDialog {

    private JTextField searchField;
    private JList<Object> resultsList;
    private DefaultListModel<Object> resultsModel;
    private SearchService searchService;
    private AppMainFrame mainFrame;
    private RecentFilesManager recentFilesManager;

    public GlobalSearchDialog(AppMainFrame parent, Project project) {
        super(parent, "Search Everywhere", true);
        this.mainFrame = parent;
        this.searchService = new SearchService(project);
        this.recentFilesManager = parent.getRecentFilesManager();
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(false);
        setSize(600, 400);
        setLocationRelativeTo(getParent());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Dialog", Font.PLAIN, 16));
        searchPanel.add(searchIcon, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Dialog", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        searchPanel.add(searchField, BorderLayout.CENTER);

        mainPanel.add(searchPanel, BorderLayout.NORTH);

        resultsModel = new DefaultListModel<>();
        resultsList = new JList<>(resultsModel);
        resultsList.setCellRenderer(new SearchResultRenderer());
        resultsList.setFont(new Font("Dialog", Font.PLAIN, 13));

        resultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = resultsList.getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < resultsModel.getSize()) {
                    Object selected = resultsModel.getElementAt(selectedIndex);
                    if (selected instanceof SeparatorItem) {
                        int nextIndex = findNextSelectableIndex(selectedIndex + 1);
                        if (nextIndex != -1) {
                            resultsList.setSelectedIndex(nextIndex);
                        } else {
                            int prevIndex = findPreviousSelectableIndex(selectedIndex - 1);
                            if (prevIndex != -1) {
                                resultsList.setSelectedIndex(prevIndex);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel hintLabel = new JLabel("Type to search test cases, reusables, test data, and object repositories");
        hintLabel.setFont(new Font("Dialog", Font.ITALIC, 11));
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        mainPanel.add(hintLabel, BorderLayout.SOUTH);

        add(mainPanel);

        setupListeners();
    }

    private void setupListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                performSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                performSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                performSearch();
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (resultsModel.getSize() > 0) {
                        int index = findNextSelectableIndex(0);
                        if (index != -1) {
                            resultsList.setSelectedIndex(index);
                            resultsList.requestFocus();
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (resultsList.getSelectedValue() != null) {
                        openSelectedResult();
                    }
                }
            }
        });

        resultsList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelectedResult();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    searchField.requestFocus();
                }
            }
        });

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedResult();
                }
            }
        });
    }

    private void performSearch() {
        String query = searchField.getText();
        resultsModel.clear();

        if (recentFilesManager.hasRecentFiles()) {
            List<SearchResult> recentFiles = recentFilesManager.getRecentFiles();

            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase().trim();
                boolean hasMatchingRecent = false;

                for (SearchResult recent : recentFiles) {
                    if (recent.getName().toLowerCase().contains(lowerQuery)) {
                        if (!hasMatchingRecent) {
                            resultsModel.addElement(new SeparatorItem("Recent Files"));
                            hasMatchingRecent = true;
                        }
                        resultsModel.addElement(recent);
                    }
                }

                List<SearchResult> results = searchService.search(query);
                if (!results.isEmpty() && hasMatchingRecent) {
                    resultsModel.addElement(new SeparatorItem("Search Results"));
                }
                for (SearchResult result : results) {
                    resultsModel.addElement(result);
                }
            } else {
                resultsModel.addElement(new SeparatorItem("Recent Files"));
                for (SearchResult recent : recentFiles) {
                    resultsModel.addElement(recent);
                }
            }
        } else if (query != null && !query.trim().isEmpty()) {
            List<SearchResult> results = searchService.search(query);
            for (SearchResult result : results) {
                resultsModel.addElement(result);
            }
        }
    }

    private void openSelectedResult() {
        Object selected = resultsList.getSelectedValue();
        if (selected instanceof SearchResult) {
            SearchResult result = (SearchResult) selected;
            recentFilesManager.addRecentFile(result);
            navigateToResult(result);
            dispose();
        }
    }

    private void navigateToResult(SearchResult result) {
        SwingUtilities.invokeLater(() -> {
            switch (result.getType()) {
                case TEST_CASE:
                    navigateToTestCase(result);
                    break;
                case REUSABLE:
                    navigateToReusable(result);
                    break;
                case TEST_DATA:
                    navigateToTestData(result);
                    break;
                case OBJECT_REPOSITORY:
                    navigateToObjectRepository(result);
                    break;
            }
        });
    }

    private void navigateToTestCase(SearchResult result) {
        if (mainFrame != null) {
            mainFrame.showTestDesign();
            String[] parts = result.getPath().split("/");
            if (parts.length >= 3) {
                String scenarioName = parts[1];
                String testCaseName = parts[2];
                mainFrame.getTestDesign().navigateToTestCase(scenarioName, testCaseName);
            }
        }
    }

    private void navigateToReusable(SearchResult result) {
        if (mainFrame != null) {
            mainFrame.showTestDesign();
            mainFrame.getTestDesign().navigateToReusable(result.getName());
        }
    }

    private void navigateToTestData(SearchResult result) {
        if (mainFrame != null) {
            mainFrame.showTestDesign();
            String[] parts = result.getPath().split("/");
            if (parts.length >= 2) {
                String environment = parts[0];
                String fileName = parts[1];
                mainFrame.getTestDesign().navigateToTestData(environment, fileName);
            } else {
                mainFrame.getTestDesign().showTestDataPanel();
            }
        }
    }

    private void navigateToObjectRepository(SearchResult result) {
        if (mainFrame != null) {
            mainFrame.showTestDesign();

            String[] parts = result.getPath().split("/");

            if (parts.length >= 3) {
                String orType = parts[0];
                String pageName = parts[1];
                String objectName = parts[2];

                mainFrame.getTestDesign().navigateToObjectRepository(orType);

                mainFrame.getTestDesign().getObjectRepo().navigateToObject(objectName, pageName);
            } else if (parts.length >= 2) {
                String orType = parts[0];
                String pageName = parts[1];

                mainFrame.getTestDesign().navigateToObjectRepository(orType);

            } else {

                String orType = result.getName();
                mainFrame.getTestDesign().navigateToObjectRepository(orType);
            }
        }
    }

    public void showDialog() {
        resultsModel.clear();
        searchField.setText("");
        performSearch();
        setVisible(true);
        searchField.requestFocus();
    }

    private int findNextSelectableIndex(int startIndex) {
        for (int i = startIndex; i < resultsModel.getSize(); i++) {
            if (resultsModel.getElementAt(i) instanceof SearchResult) {
                return i;
            }
        }
        return -1;
    }

    private int findPreviousSelectableIndex(int startIndex) {
        for (int i = startIndex; i >= 0; i--) {
            if (resultsModel.getElementAt(i) instanceof SearchResult) {
                return i;
            }
        }
        return -1;
    }

    private static class SeparatorItem {
        private final String label;

        public SeparatorItem(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class SearchResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof SeparatorItem) {

                SeparatorItem separator = (SeparatorItem) value;
                JLabel label = new JLabel(separator.getLabel());
                label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
                label.setForeground(Color.GRAY);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(3, 5, 3, 5)
                ));
                label.setOpaque(true);
                label.setBackground(new Color(240, 240, 240));
                return label;
            } else if (value instanceof SearchResult) {

                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                SearchResult result = (SearchResult) value;
                label.setIcon(result.getIcon());
                label.setText(result.getName());

                String badge = " [" + result.getType().getDisplayName() + "]";
                label.setText(result.getName() + badge);

                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return label;
            }

            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
