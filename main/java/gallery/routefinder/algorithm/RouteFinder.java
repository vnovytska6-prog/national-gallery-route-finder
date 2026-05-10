package gallery.routefinder.algorithm;

import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphEdge;
import gallery.routefinder.graph.GraphNode;

import java.util.*;

public class RouteFinder {

    //Single route (DFS)
    public static List<GraphNode> findSingleRoute(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        List<List<GraphNode>> routes = findMultipleRoutes(from, lookingFor, 1, avoid, waypoints, null);
        if (routes == null || routes.isEmpty()) return null;
        return routes.get(0);
    }

    //Find all paths using DepthFirst
    public static List<List<GraphNode>> findMultipleRoutes(GraphNode from, GraphNode lookingFor, int max, Set<String> avoid, List<GraphNode> waypoints, List<GraphNode> encountered) {
        List<List<GraphNode>> result=null, temp2;

        if(from==null) return result;

        if (from.equals(lookingFor)) {
            List<GraphNode> temp=new ArrayList<>();
            temp.add(from);
            result=new ArrayList<>();
            result.add(temp);
            return result;
        }

        if(encountered==null) encountered=new ArrayList<>();
        encountered.add(from);

        for (GraphEdge edge: from.getEdges()){
            GraphNode neighbor = edge.getDestination();
            //skip avoided rooms
            if (avoid != null && avoid.contains(neighbor.getRoom().getId())) continue;
            if (!encountered.contains(neighbor)) {
                temp2=findMultipleRoutes(neighbor, lookingFor, max, avoid, waypoints, new ArrayList<>(encountered));

                if (temp2 != null) {
                    for(List<GraphNode> list : temp2) {
                        list.addFirst(from);

                        //if route contains all waypoints
                        if (waypoints != null && !waypoints.isEmpty()) {
                            boolean hasAllWaypoints = true;
                            for (GraphNode wp : waypoints) {
                                if (!list.contains(wp)) {
                                    hasAllWaypoints = false;
                                    break;}
                            }
                            if (!hasAllWaypoints) continue; }
                    }

                        if(result==null) result=temp2;
                    else if (result.size()<max) {
                        result.addAll(temp2);
                    }
                }
            }
        }
        return result;
    }

    //Calculating route distance
    public static double calculateRouteDistance(List<GraphNode> route){
        if (route == null || route.size() < 2) return 0;
        double routeDistance=0;
        for(int i=1; i<route.size(); i++){
            GraphNode current=route.get(i-1);
            GraphNode next = route.get(i);
            for(GraphEdge edge : current.getEdges()) {
                if(edge.getDestination().equals(next)) {
                    routeDistance += edge.getDistance();
                    break;
                }
            }
        }
        return routeDistance;
    }

    //Find the shortest path using BreadthFirst
    public static List<GraphNode> bfsShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        if (from == null || lookingFor == null) return null;
        List<GraphNode> resultPath = new ArrayList<>();

        // Handle waypoints in sequence
        if (waypoints != null && !waypoints.isEmpty()) {
            GraphNode currentStart = from;
            for(GraphNode waypoint : waypoints) {
                List<GraphNode> segment = bfsShortestPathSegment(currentStart, waypoint, avoid);
                if (segment == null) return null;
                // Don't duplicate the start of next segment
                if (resultPath.isEmpty()) {
                    resultPath.addAll(segment);
                } else {
                    resultPath.addAll(segment.subList(1, segment.size()));
                }
                currentStart = waypoint;
            }
            // Final segment to destination
            List<GraphNode> finalSegment = bfsShortestPathSegment(currentStart, lookingFor, avoid);
            if (finalSegment == null) return null;
            resultPath.addAll(finalSegment.subList(1, finalSegment.size()));
        } else {
            // Direct path without waypoints
            return bfsShortestPathSegment(from, lookingFor, avoid);
        }

        return resultPath;
    }

    private static List<GraphNode> bfsShortestPathSegment(GraphNode from, GraphNode lookingFor, Set<String> avoid) {
        //BFS using agenda of paths
        Queue<List<GraphNode>> agenda = new LinkedList<>();
        List<GraphNode> startPath = new ArrayList<>();
        startPath.add(from);
        agenda.add(startPath);
        Set<GraphNode> visited = new HashSet<>();
        visited.add(from);

        while (!agenda.isEmpty()) {
            List<GraphNode> currentPath = agenda.poll();
            GraphNode current = currentPath.get(currentPath.size() - 1);

            if (current.equals(lookingFor)) {
                return currentPath;
            }
            // iterate through edges
            for (GraphEdge edge : current.getEdges()) {
                GraphNode neighbor = edge.getDestination();

                // Skip avoided rooms
                if (avoid != null && avoid.contains(neighbor.getRoom().getId())) continue;

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<GraphNode> newPath = new ArrayList<>(currentPath);
                    newPath.add(neighbor);
                    agenda.add(newPath);
                }
            }
        }
        return null;
    }

    //Find the shortest path using Dijkstra's
    public static List<GraphNode> dijkstraShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints) {
        if (from == null || lookingFor == null) return null;

        List<GraphNode> resultPath = new ArrayList<>();

        // Handle waypoints in sequence
        if (waypoints != null && !waypoints.isEmpty()) {
            GraphNode currentStart = from;
            for (GraphNode waypoint : waypoints) {
                List<GraphNode> segment = dijkstraShortestPathSegment(currentStart, waypoint, avoid);
                if (segment == null) return null;
                if (resultPath.isEmpty()) {
                    resultPath.addAll(segment);
                } else {
                    resultPath.addAll(segment.subList(1, segment.size()));
                }
                currentStart = waypoint;
            }
            List<GraphNode> finalSegment = dijkstraShortestPathSegment(currentStart, lookingFor, avoid);
            if (finalSegment == null) return null;
            resultPath.addAll(finalSegment.subList(1, finalSegment.size()));
        } else {
            return dijkstraShortestPathSegment(from, lookingFor, avoid);
        }

        return resultPath;
    }

    private static List<GraphNode> dijkstraShortestPathSegment(GraphNode from, GraphNode lookingFor, Set<String> avoid) {
        Map<GraphNode, Double> distances = new HashMap<>();
        Map<GraphNode, GraphNode> previous = new HashMap<>();
        PriorityQueue<GraphNode> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        Set<GraphNode> settled = new HashSet<>();

        // Get all reachable nodes
        Set<GraphNode> allNodes = getAllReachableNodes(from);
        for (GraphNode node : allNodes) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }

        distances.put(from, 0.0);
        pq.add(from);

        while (!pq.isEmpty()) {
            GraphNode current = pq.poll();

            if (settled.contains(current)) continue;
            settled.add(current);

            if (current.equals(lookingFor)) {
                return reconstructPath(previous, lookingFor);
            }

            for (GraphEdge edge : current.getEdges()) {
                GraphNode neighbor = edge.getDestination();

                // Skip avoided rooms (condition was reversed)
                if (avoid != null && avoid.contains(neighbor.getRoom().getId())) continue;

                if (!settled.contains(neighbor)) {
                    double newDist = distances.get(current) + edge.getDistance();
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        previous.put(neighbor, current);
                        pq.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    private static Set<GraphNode> getAllReachableNodes(GraphNode start) {
        Set<GraphNode> nodes = new HashSet<>();
        Queue<GraphNode> queue = new LinkedList<>();
        nodes.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            for (GraphEdge edge : current.getEdges()) {
                if (!nodes.contains(edge.getDestination())) {
                    nodes.add(edge.getDestination());
                    queue.add(edge.getDestination());
                }
            }
        }
        return nodes;
    }

    private static List<GraphNode> reconstructPath(Map<GraphNode, GraphNode> previous, GraphNode target) {
        List<GraphNode> path = new ArrayList<>();
        GraphNode current = target;
        while (current != null) {
            path.addFirst(current);
            current = previous.get(current);
        }
        return path;
    }

    //Most interesting Route using Dijkstra's
    public static List<GraphNode> mostInterestingRoute(Graph graph, GraphNode from, GraphNode lookingFor, Set<String> artists , Set<String> avoid, List<GraphNode> waypoints){
        List<GraphNode> rooms = new ArrayList<>();
            if (artists != null) {
                for (String artist : artists) {
                    List<GraphNode> artistRooms = graph.getNodesByArtist(artist);
                    if (artistRooms != null) {
                        rooms.addAll(artistRooms);
                    }
                }
            }
            if (waypoints != null) { rooms.addAll(waypoints);}
            // Remove duplicates
            List<GraphNode> uniqueRooms = new ArrayList<>(new LinkedHashSet<>(rooms));
            return dijkstraShortestPath(from, lookingFor, avoid, uniqueRooms);
        }
    }


