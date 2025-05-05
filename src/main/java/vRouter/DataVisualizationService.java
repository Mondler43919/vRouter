package vRouter;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import org.knowm.xchart.XYSeries;
import peersim.core.CommonState;
import peersim.core.Control;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataVisualizationService implements Control {
    private static DataVisualizationService instance; // 静态单例

    private final Chart chart;
    private final Map<String, List<Object[]>> nodeStats = new ConcurrentHashMap<>();
    private final Color[] colorPalette;
    private int colorIndex = 0;

    public DataVisualizationService(String prefix) {
        if (instance != null) {
            throw new RuntimeException("DataVisualizationService already initialized!");
        }
        instance = this;

        this.chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        this.chart.addMouseCameraController();
        this.colorPalette = createColorPalette();

        chart.getAxeLayout().setXAxeLabel("Node Count");
        chart.getAxeLayout().setYAxeLabel("Round");
        chart.getAxeLayout().setZAxeLabel("Access Count");
    }

    public static DataVisualizationService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DataVisualizationService not initialized yet!");
        }
        return instance;
    }
    public synchronized void updatePerRound(
            long round,
            Map<String, Integer> accessCounts,
            Map<String, Set<String>> accessNodes,
            Map<String, double[]> dataScores) {

        accessCounts.forEach((dataId, accessCount) -> {
            int nodeCount = accessNodes.getOrDefault(dataId, Collections.emptySet()).size();
            if(dataScores.containsKey(dataId)){
                double score = dataScores.get(dataId)[0];
                int recycled =(int) dataScores.get(dataId)[4];
                int level = (int) dataScores.get(dataId)[3];
                ExcelLogger.SimpleLogger.logSimpleAccess(round, dataId, accessCount, nodeCount, score,recycled,level);
            }
        });
    }

    /**
     * Draw the 3D visualization in the final round
     */
    private void drawFinalVisualization() {
        if (nodeStats.isEmpty()) {
            System.out.println("No data to visualize!");
            return;
        }


        // Draw a line for each node showing its progression through rounds
        nodeStats.forEach((nodeId, statsList) -> {
            LineStrip line = new LineStrip();
            line.setWireframeColor(getNextColor());
            line.setWidth(2f);

            // Add all data points for this node
            statsList.forEach(stats -> {
                int nodeCount =(int) stats[0];
                int round =(int) stats[1];
                int accessCount =(int) stats[2];

                Coord3d point = new Coord3d(nodeCount, round, accessCount);
                line.add(point);
            });

            chart.getScene().add(line);
        });

        // Auto-adjust the view
        chart.getView().shoot();
    }

    /**
     * Get the completed chart object
     */
    public Chart getChart() {
        return chart;
    }

    /**
     * Save the chart as an image
     */
    public void saveChart(String filename) throws Exception {
        chart.screenshot(new java.io.File(filename));
    }

    private Color getNextColor() {
        return colorPalette[colorIndex++ % colorPalette.length];
    }

    private Color[] createColorPalette() {
        Random rand = new Random(42); // Fixed seed for consistent colors
        Color[] colors = new Color[50]; // Supports up to 50 nodes

        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        }

        return colors;
    }

    @Override
    public boolean execute() {
        long currentRound = CommonState.getTime() / 1000;

        // In the final round (149), draw the visualization
        if (currentRound == 149) {
            try {
                System.out.println("Final round reached - drawing visualization");
                drawFinalVisualization();

                // Display the chart
                chart.open("Node Statistics Visualization", 800, 600);

                // Save the chart
                saveChart("final_visualization.png");
            } catch (Exception e) {
                System.err.println("Error during visualization: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }
}