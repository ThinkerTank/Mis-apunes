package co.edu.icesi.sgmms.view;

import co.edu.icesi.sgmms.model.CityMap;
import co.edu.icesi.sgmms.model.Incident;
import co.edu.icesi.sgmms.model.IncidentStatus;
import co.edu.icesi.sgmms.model.Position;
import co.edu.icesi.sgmms.model.Vehicle;
import co.edu.icesi.sgmms.model.VehicleStatus;
import co.edu.icesi.sgmms.model.ZoneType;
import co.edu.icesi.sgmms.model.structures.Edge;
import co.edu.icesi.sgmms.model.structures.LinkedList;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws the traffic map on a canvas: zones, roads, blocked segments, emergency corridors, risk
 * points, incidents, emergency vehicles with their routes, private cars and the operator.
 * It is pure view code: it only reads the {@link MapScene} prepared by the controller.
 */
public class MapRenderer {

    private double cell;
    private double originX;
    private double originY;

    /**
     * Draws a full frame scaled to fit {@code width} × {@code height}.
     *
     * <p>Complexity — time: O(W·H + m + c + n + E_mst) per frame; space: O(1).
     */
    public void draw(GraphicsContext g, double width, double height, MapScene scene) {
        CityMap map = scene.map;
        g.setFill(Palette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        if (map == null) {
            return;
        }
        cell = Math.floor(Math.min(width / map.getWidth(), height / map.getHeight()));
        originX = Math.floor((width - cell * map.getWidth()) / 2);
        originY = Math.floor((height - cell * map.getHeight()) / 2);

        drawCells(g, map);
        drawCorridors(g, scene.corridors);
        drawBlocked(g, scene.blocked);
        drawRiskPoints(g, scene.riskPoints);
        drawMonitoringCenter(g, map.getMonitoringCenter());
        drawRoutes(g, scene);
        drawIncidents(g, scene.incidents);
        drawCivilians(g, scene.civilians);
        drawVehicles(g, scene);
        drawCursor(g, scene.selected, Palette.SELECTION, 2);
        drawCursor(g, scene.pendingBlock, Palette.BLOCKED, 3);
        drawCursor(g, scene.operator, Palette.OPERATOR, 3.5);
    }

    /** Cell under a point of the canvas, or {@code null}. Complexity — O(1). */
    public Position cellAt(double x, double y, CityMap map) {
        if (map == null || cell <= 0) {
            return null;
        }
        int cx = (int) Math.floor((x - originX) / cell);
        int cy = (int) Math.floor((y - originY) / cell);
        Position p = new Position(cx, cy);
        return map.isInside(p) ? p : null;
    }

    private void drawCells(GraphicsContext g, CityMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                ZoneType zone = map.zoneAt(x, y);
                g.setFill(Palette.zone(zone));
                g.fillRect(left(x), top(y), cell, cell);
                if (zone == ZoneType.OBSTACLE) {
                    g.setStroke(Color.web("#1e293b"));
                    g.setLineWidth(1);
                    g.strokeLine(left(x) + 3, top(y) + 3, left(x) + cell - 3, top(y) + cell - 3);
                }
            }
        }
        g.setStroke(Palette.GRID_LINE);
        g.setLineWidth(1);
        for (int x = 0; x <= map.getWidth(); x++) {
            g.strokeLine(left(x), top(0), left(x), top(map.getHeight()));
        }
        for (int y = 0; y <= map.getHeight(); y++) {
            g.strokeLine(left(0), top(y), left(map.getWidth()), top(y));
        }
    }

    private void drawCorridors(GraphicsContext g, LinkedList<Edge<Position>> corridors) {
        g.setStroke(Palette.CORRIDOR);
        g.setLineWidth(Math.max(2, cell * 0.14));
        g.setLineCap(StrokeLineCap.ROUND);
        for (Edge<Position> edge : corridors) {
            g.strokeLine(cx(edge.getFrom()), cy(edge.getFrom()), cx(edge.getTo()), cy(edge.getTo()));
        }
    }

    private void drawBlocked(GraphicsContext g, LinkedList<Edge<Position>> blocked) {
        g.setStroke(Palette.BLOCKED);
        g.setLineWidth(Math.max(3, cell * 0.18));
        g.setLineCap(StrokeLineCap.ROUND);
        for (Edge<Position> edge : blocked) {
            double mx = (cx(edge.getFrom()) + cx(edge.getTo())) / 2;
            double my = (cy(edge.getFrom()) + cy(edge.getTo())) / 2;
            double half = cell * 0.35;
            if (edge.getFrom().getX() != edge.getTo().getX()) {
                g.strokeLine(mx, my - half, mx, my + half);
            } else {
                g.strokeLine(mx - half, my, mx + half, my);
            }
        }
    }

    private void drawRiskPoints(GraphicsContext g, LinkedList<Position> riskPoints) {
        g.setStroke(Palette.RISK);
        g.setLineWidth(2);
        double r = cell * 0.42;
        for (Position p : riskPoints) {
            g.strokeOval(cx(p) - r, cy(p) - r, 2 * r, 2 * r);
        }
    }

    private void drawMonitoringCenter(GraphicsContext g, Position center) {
        double pad = cell * 0.08;
        g.setFill(Color.web("#0f172a"));
        g.fillRoundRect(left(center.getX()) + pad, top(center.getY()) + pad, cell - 2 * pad, cell - 2 * pad, 6, 6);
        label(g, "CM", cx(center), cy(center), Palette.CENTER, cell * 0.36);
    }

    private void drawRoutes(GraphicsContext g, MapScene scene) {
        g.setLineDashes(cell * 0.25, cell * 0.18);
        for (Vehicle vehicle : scene.vehicles) {
            LinkedList<Position> route = scene.routes.get(vehicle.getId());
            if (route == null || route.isEmpty()) {
                continue;
            }
            boolean highlighted = vehicle.getId().equals(scene.highlightedVehicle);
            g.setStroke(Palette.vehicle(vehicle.getType()));
            g.setLineWidth(highlighted ? Math.max(3, cell * 0.16) : Math.max(2, cell * 0.08));
            Position previous = vehicle.getPosition();
            for (Position step : route) {
                g.strokeLine(cx(previous), cy(previous), cx(step), cy(step));
                previous = step;
            }
        }
        g.setLineDashes((double[]) null);
    }

    private void drawIncidents(GraphicsContext g, LinkedList<Incident> incidents) {
        double r = cell * 0.40;
        for (Incident incident : incidents) {
            double x = cx(incident.getPosition());
            double y = cy(incident.getPosition());
            double[] xs = {x, x + r, x, x - r};
            double[] ys = {y - r, y, y + r, y};
            g.setFill(Palette.incident(incident.getType()));
            g.fillPolygon(xs, ys, 4);
            g.setStroke(Palette.severity(incident.getSeverity()));
            g.setLineWidth(incident.getStatus() == IncidentStatus.PENDING ? 3 : 1.5);
            g.strokePolygon(xs, ys, 4);
            label(g, Palette.incidentLetter(incident.getType()), x, y, Color.WHITE, cell * 0.34);
        }
    }

    private void drawCivilians(GraphicsContext g, LinkedList<Position> civilians) {
        g.setFill(Palette.CIVILIAN);
        double r = Math.max(2.5, cell * 0.12);
        for (Position p : civilians) {
            g.fillOval(left(p.getX()) + cell * 0.18 - r, top(p.getY()) + cell * 0.80 - r, 2 * r, 2 * r);
        }
    }

    private void drawVehicles(GraphicsContext g, MapScene scene) {
        double r = cell * 0.34;
        for (Vehicle vehicle : scene.vehicles) {
            double x = cx(vehicle.getPosition());
            double y = cy(vehicle.getPosition());
            Color body = vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE
                    ? Color.web("#64748b") : Palette.vehicle(vehicle.getType());
            g.setFill(body);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setStroke(vehicle.getStatus() == VehicleStatus.ATTENDING ? Palette.OPERATOR : Color.WHITE);
            g.setLineWidth(vehicle.getId().equals(scene.highlightedVehicle) ? 3 : 1.5);
            g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            label(g, Palette.vehicleLetter(vehicle.getType()), x, y, Color.WHITE, cell * 0.36);
        }
    }

    private void drawCursor(GraphicsContext g, Position p, Color color, double width) {
        if (p == null) {
            return;
        }
        g.setStroke(color);
        g.setLineWidth(width);
        double inset = width / 2 + 1;
        g.strokeRect(left(p.getX()) + inset, top(p.getY()) + inset, cell - 2 * inset, cell - 2 * inset);
    }

    private void label(GraphicsContext g, String text, double x, double y, Color color, double size) {
        g.setFill(color);
        g.setFont(Font.font("System", FontWeight.BOLD, Math.max(9, size)));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(text, x, y);
    }

    private double left(int x) {
        return originX + x * cell;
    }

    private double top(int y) {
        return originY + y * cell;
    }

    private double cx(Position p) {
        return left(p.getX()) + cell / 2;
    }

    private double cy(Position p) {
        return top(p.getY()) + cell / 2;
    }
}
