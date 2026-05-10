package gallery.routefinder.algorithm;

import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphEdge;
import gallery.routefinder.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RouteFinder {

    public static List<GraphNode> findSingleRoute(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        return findMultipleRoutes(from,lookingFor,1,avoid,waypoints,null).getFirst();
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

        for (GraphEdge n : from.getEdges()){
            if(avoid.contains(n.getDestination().getRoom().getId())) continue;
            if (!encountered.contains(n.getDestination())) {
                temp2=findMultipleRoutes(n.getDestination(), lookingFor, max, avoid, waypoints, new ArrayList<>(encountered));

                if (temp2 != null) {
                    for(List<GraphNode> list : temp2)
                        list.addFirst(from);
                    if(result==null) result=temp2;
                    else if (result.size()<max && (waypoints==null || waypoints.isEmpty() || temp2.containsAll(waypoints))) {
                        result.addAll(temp2);
                    }
                }
            }
        }
        return result;
    }

    public static double calculateRouteDistance(List<GraphNode> route){
        double routeDistance=0;
        for(int i=1; i<route.size(); i++){
            GraphNode n=route.get(i-1);
            for(GraphEdge e:n.getEdges()){
                if(e.getDestination().equals(route.get(i))){
                    routeDistance+=e.getDistance();
                    break;
                }
            }
        }
        return routeDistance;
    }

    //Find the shortest path using BreadthFirst
    public static List<GraphNode> bfsShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        List<GraphNode> resultPath = new ArrayList<>();
        for(GraphNode n : waypoints){
            GraphNode prior = resultPath.isEmpty() ? from : resultPath.getLast();
            List<GraphNode> path = bfsShortestPath(prior, n, avoid);
            if(path == null) return null;
            resultPath.addAll(path);
        }
        GraphNode prior = resultPath.isEmpty() ? from : resultPath.getLast();
        List<GraphNode> path = bfsShortestPath(prior, lookingFor, avoid);
        if(path == null) return null;
        resultPath.addAll(path);

        return resultPath;
    }

    public static List<GraphNode> bfsShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid) {
        List<List<GraphNode>> agenda=new ArrayList<>();
        List<GraphNode> firstAgendaPath=new ArrayList<>(),resultPath;
        firstAgendaPath.add(from);
        agenda.add(firstAgendaPath);
        resultPath=findPathBreadthFirst(agenda,null,lookingFor,avoid);
        if(resultPath==null) return null;
        Collections.reverse(resultPath);
        return resultPath;
    }

    public static List<GraphNode> findPathBreadthFirst(List<List<GraphNode>> agenda, List<GraphNode> encountered ,GraphNode lookingFor, Set<String> avoid) {
        if(agenda.isEmpty()) return null;
        List<GraphNode> nextPath=agenda.removeFirst();
        GraphNode current=nextPath.getFirst();
        if(current.getRoom().equals(lookingFor)) return nextPath;
        if (encountered == null) encountered = new ArrayList<>();
        encountered.add(current);

        for (GraphNode n : current.getNodes()) {
            if(avoid.contains(n.getRoom().getId())) continue;
            if (!encountered.contains(n)) {
                List<GraphNode> newPath=new ArrayList<>(nextPath);
                newPath.addFirst(n);
                agenda.add(newPath);
            }
        }
        return findPathBreadthFirst(agenda,encountered,lookingFor, avoid);
    }

    //Find the shortest path using Dijkstra's
    public static class CostedPath {
        public double pathCost=0;
        public List<GraphNode> pathList=new ArrayList<>();
    }

    public static List<GraphNode> dijkstraShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        List<GraphNode> resultPath = new ArrayList<>();
        for(GraphNode n : waypoints){
            GraphNode prior = resultPath.isEmpty() ? from : resultPath.getLast();
            List<GraphNode> path = dijkstraShortestPath(prior, n, avoid);
            if(path == null) return null;
            resultPath.addAll(path);
        }
        GraphNode prior = resultPath.isEmpty() ? from : resultPath.getLast();
        List<GraphNode> path = dijkstraShortestPath(prior, lookingFor, avoid);
        if(path == null) return null;
        resultPath.addAll(path);

        return resultPath;
    }

    public static List<GraphNode> dijkstraShortestPath(GraphNode from, GraphNode lookingFor, Set<String> avoid) {
        CostedPath cp= new CostedPath();
        List<GraphNode> encountered=new ArrayList<>(), unEncountered=new ArrayList<>();
        from.setDistanceFromStart(0);
        unEncountered.add(from);
        GraphNode currentNode;

        do {
            currentNode = unEncountered.removeFirst();
            encountered.add(currentNode);

            if (currentNode.getRoom().equals(lookingFor)) {
                cp.pathList.add(currentNode);
                cp.pathCost = currentNode.getDistanceFromStart();

                while (currentNode != from) {
                    boolean foundPrevPathNode = false;
                    for (GraphNode n : encountered) {
                        for (GraphEdge e : n.getEdges()) {
                            if (e.getDestination().equals(currentNode) && currentNode.getDistanceFromStart() - e.getDistance() == n.getDistanceFromStart()) {
                                cp.pathList.addFirst(n);
                                currentNode = n;
                                foundPrevPathNode = true;
                                break;
                            }
                            if (foundPrevPathNode) break;
                        }
                    }
                    for (GraphNode n : encountered) n.setDistanceFromStart(Integer.MAX_VALUE);
                    for (GraphNode n : unEncountered) n.setDistanceFromStart(Integer.MAX_VALUE);

                    return cp.pathList;
                }
                for (GraphEdge e : currentNode.getEdges()){
                    if(!avoid.contains(e.getDestination().getRoom().getId())) continue;
                    if (!encountered.contains(e.getDestination())) {
                        e.getDestination().setDistanceFromStart((Integer.min((int) (e.getDestination().getDistanceFromStart()), (int) (currentNode.getDistanceFromStart() + e.getDistance()))));
                        if (!unEncountered.contains(e.getDestination())) unEncountered.add(e.getDestination());
                    }
                }
                Collections.sort(unEncountered, (n1, n2) -> (int) (n1.getDistanceFromStart() - n2.getDistanceFromStart()));
            }
        }while (!unEncountered.isEmpty()) ;
        return null;
    }

    //Most interesting Route using Dijkstra's
    public static List<GraphNode> mostInterestingRoute(Graph graph, GraphNode from, GraphNode lookingFor, Set<String> artists , Set<String> avoid, List<GraphNode> waypoints){
        List<GraphNode> rooms = new ArrayList<>();
        artists.forEach(artist -> rooms.addAll(graph.getNodesByArtist(artist)));
        rooms.addAll(waypoints);
        return dijkstraShortestPath(from, lookingFor, avoid, rooms);
    }
}


