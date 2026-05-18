package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for the admin Insights tab.
 * Displays all-time metrics and charts; AI section covers the last 30 days only.
 */
public class InsightsDialogController {

    private ServiceRegistry services;
    private Stage dialogStage;

    // Module stats data
    private List<ModuleStat> moduleStats = new ArrayList<>();

    @FXML
    private Label totalJobsLabel;
    @FXML
    private Label openJobsLabel;
    @FXML
    private Label applicationsLabel;
    @FXML
    private Label hiredLabel;
    @FXML
    private Label hireRateLabel;
    @FXML
    private BarChart<String, Number> hiringStatusChart;
    @FXML
    private VBox insightsContentBox;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox moduleStatsBox;

    public void setServices(ServiceRegistry services) {
        this.services = services;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void initialize() {
        // Initialize charts
        setupCharts();
    }

    private void setupCharts() {
        // Hiring Status Chart setup only
        CategoryAxis statusXAxis = new CategoryAxis();
        statusXAxis.setTickLabelRotation(0);
        NumberAxis statusYAxis = new NumberAxis();
        statusYAxis.setLabel("Count");
        statusYAxis.setMinorTickVisible(false);
        hiringStatusChart.setTitle("");
        hiringStatusChart.setLegendVisible(false);
        hiringStatusChart.setAnimated(false);
        hiringStatusChart.setCategoryGap(10);
    }

    public void loadData() {
        try {
            // Load data from services
            List<com.bupt.tarecruit.entity.Job> jobs = services.jobService().findAllJobs();
            List<com.bupt.tarecruit.entity.ApplicationRecord> allApplications = jobs.stream()
                    .flatMap(job -> services.applicationService().findByJob(job.getJobId()).stream())
                    .collect(Collectors.toList());

            int openJobsCount = (int) jobs.stream().filter(com.bupt.tarecruit.entity.Job::isOpen).count();
            long totalApplications = allApplications.size();
            long totalHired = allApplications.stream()
                    .filter(a -> a.getStatus() == com.bupt.tarecruit.entity.ApplicationStatus.HIRED)
                    .count();

            // Update all-time metric labels
            totalJobsLabel.setText(String.valueOf(jobs.size()));
            openJobsLabel.setText(String.valueOf(openJobsCount));
            applicationsLabel.setText(String.valueOf(totalApplications));
            hiredLabel.setText(String.valueOf(totalHired));

            double hireRate = totalApplications > 0 ? (totalHired * 100.0 / totalApplications) : 0;
            hireRateLabel.setText(String.format("%.1f%%", hireRate));

            // Update charts (all-time)
            updateModuleStats(allApplications, jobs);
            updateHiringStatusChart(allApplications);

            // Load AI insights (last 30 days only)
            loadAiInsights(allApplications, openJobsCount);

        } catch (Exception e) {
            DialogUtil.error("Failed to load insights: " + e.getMessage(), dialogStage);
        }
    }

    private void updateModuleStats(List<com.bupt.tarecruit.entity.ApplicationRecord> applications,
                                   List<com.bupt.tarecruit.entity.Job> jobs) {
        // Create a map of jobId to Job
        Map<String, com.bupt.tarecruit.entity.Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(com.bupt.tarecruit.entity.Job::getJobId, j -> j));

        // Count applications by module
        Map<String, Long> moduleCounts = applications.stream()
                .filter(a -> jobMap.containsKey(a.getJobId()))
                .collect(Collectors.groupingBy(
                        a -> jobMap.get(a.getJobId()).getModuleName(),
                        Collectors.counting()
                ));

        // Build module stats list
        moduleStats.clear();
        for (Map.Entry<String, Long> entry : moduleCounts.entrySet()) {
            moduleStats.add(new ModuleStat(entry.getKey(), entry.getValue()));
        }

        // Default sort by count (descending)
        moduleStats.sort(Comparator.comparingLong(ModuleStat::getCount).reversed());

        renderModuleStats();
    }

    private void renderModuleStats() {
        moduleStatsBox.getChildren().clear();

        if (moduleStats.isEmpty()) {
            Label noData = new Label("No application data available");
            noData.getStyleClass().add("text-muted");
            moduleStatsBox.getChildren().add(noData);
            return;
        }

        // Find max count for bar scaling
        long maxCount = moduleStats.stream()
                .mapToLong(ModuleStat::getCount)
                .max()
                .orElse(1);

        for (ModuleStat stat : moduleStats) {
            HBox row = new HBox();
            row.setSpacing(10);
            row.setStyle("-fx-alignment: center-left;");

            // Module name
            Label nameLabel = new Label(stat.getModuleName());
            nameLabel.setStyle("-fx-pref-width: 200px; -fx-font-size: 13px;");
            nameLabel.setWrapText(true);

            // Bar
            int barLength = (int) (20 * stat.getCount() / Math.max(maxCount, 1));
            String bar = "█".repeat(Math.max(barLength, 1));
            Label barLabel = new Label(bar);
            barLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-family: monospace;");

            // Count label
            Label countLabel = new Label(stat.getCount() + " applications");
            countLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

            row.getChildren().addAll(nameLabel, barLabel, countLabel);
            moduleStatsBox.getChildren().add(row);
        }
    }

    private void updateHiringStatusChart(List<com.bupt.tarecruit.entity.ApplicationRecord> applications) {
        long pending = applications.stream().filter(a -> a.getStatus() == com.bupt.tarecruit.entity.ApplicationStatus.PENDING).count();
        long hired = applications.stream().filter(a -> a.getStatus() == com.bupt.tarecruit.entity.ApplicationStatus.HIRED).count();
        long rejected = applications.stream().filter(a -> a.getStatus() == com.bupt.tarecruit.entity.ApplicationStatus.REJECTED).count();
        long withdrawn = applications.stream().filter(a -> a.getStatus() == com.bupt.tarecruit.entity.ApplicationStatus.WITHDRAWN).count();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Pending", pending));
        series.getData().add(new XYChart.Data<>("Hired", hired));
        series.getData().add(new XYChart.Data<>("Rejected", rejected));
        series.getData().add(new XYChart.Data<>("Withdrawn", withdrawn));

        hiringStatusChart.getData().clear();
        hiringStatusChart.getData().add(series);
    }

    private void loadAiInsights(List<com.bupt.tarecruit.entity.ApplicationRecord> applications,
                                int openJobs) {
        insightsContentBox.getChildren().clear();
        insightsContentBox.getChildren().add(new Label("Generating 30-day insights..."));

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return services.aiService().generate30DayInsights(applications, openJobs);
            }
        };

        task.setOnSucceeded(evt -> {
            String insights = task.getValue();
            displayInsights(insights);
        });

        task.setOnFailed(evt -> {
            insightsContentBox.getChildren().clear();
            insightsContentBox.getChildren().add(new Label("Failed to generate insights: " + task.getException().getMessage()));
        });

        new Thread(task, "ai-insights-loader").start();
    }

    private void displayInsights(String insightsText) {
        insightsContentBox.getChildren().clear();

        if (insightsText == null || insightsText.isBlank()) {
            Label noData = new Label("No insights available.");
            noData.getStyleClass().add("text-muted");
            insightsContentBox.getChildren().add(noData);
            return;
        }

        // Split by numbered points (1. 2. 3.)
        String[] points = insightsText.split("(?m)^\\d+\\.");

        for (String point : points) {
            String trimmed = point.trim();
            if (trimmed.isEmpty()) continue;
            addInsightCard(trimmed);
        }
    }

    private void addInsightCard(String content) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
        card.setSpacing(8);
        card.getStyleClass().add("insight-item");

        // Determine card type based on content keywords
        String lowerContent = content.toLowerCase();
        String cardType = "neutral";
        if (lowerContent.contains("increase") || lowerContent.contains("improve") || lowerContent.contains("good") ||
            lowerContent.contains("strong") || lowerContent.contains("growth") || lowerContent.contains("healthy")) {
            cardType = "positive";
        } else if (lowerContent.contains("decrease") || lowerContent.contains("decline") || lowerContent.contains("low") ||
                   lowerContent.contains("concern") || lowerContent.contains("lack") || lowerContent.contains("below")) {
            cardType = "warning";
        }
        card.getStyleClass().add(cardType);

        // Split into sentences for better formatting
        String[] sentences = content.split("\\.\\s*");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("●")) {
                // Bullet point
                Label bulletLabel = new Label(trimmed);
                bulletLabel.getStyleClass().add("insight-content");
                bulletLabel.setWrapText(true);
                card.getChildren().add(bulletLabel);
            } else {
                // Regular sentence - check if it's a title
                if (card.getChildren().isEmpty()) {
                    // First sentence = title
                    Label titleLabel = new Label(trimmed.endsWith(".") ? trimmed : trimmed + ".");
                    titleLabel.getStyleClass().add("insight-title");
                    titleLabel.setWrapText(true);
                    card.getChildren().add(titleLabel);
                } else {
                    Label contentLabel = new Label(trimmed.endsWith(".") ? trimmed : trimmed + ".");
                    contentLabel.getStyleClass().add("insight-content");
                    contentLabel.setWrapText(true);
                    card.getChildren().add(contentLabel);
                }
            }
        }

        insightsContentBox.getChildren().add(card);
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
        // No-op when the view is embedded as a tab.
    }

    @FXML
    private void handleSortModulesByName() {
        moduleStats.sort(Comparator.comparing(ModuleStat::getModuleName));
        renderModuleStats();
    }

    @FXML
    private void handleSortModulesByCount() {
        moduleStats.sort(Comparator.comparingLong(ModuleStat::getCount).reversed());
        renderModuleStats();
    }

    /**
     * Inner class to hold module statistics
     */
    private static class ModuleStat {
        private final String moduleName;
        private final long count;

        public ModuleStat(String moduleName, long count) {
            this.moduleName = moduleName != null ? moduleName : "Unknown";
            this.count = count;
        }

        public String getModuleName() {
            return moduleName;
        }

        public long getCount() {
            return count;
        }
    }
}
