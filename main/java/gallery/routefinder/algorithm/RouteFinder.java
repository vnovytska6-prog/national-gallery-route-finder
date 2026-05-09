package gallery.routefinder.algorithm;

import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphEdge;
import gallery.routefinder.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RouteFinder {

    public static List<GraphNode> findSingleRoute(Graph graph, GraphNode from, GraphNode lookingFor, Set<String> avoid, List<GraphNode> waypoints){
        return findMultipleRoutes(graph,from,lookingFor,1,avoid,waypoints,null).getFirst();
    }

    //Find all paths using DepthFirst
    public static <T> List<List<T>> findMultipleRoutes(Graph graph, T from, T lookingFor, int max, Set<String> avoid, List<T> waypoints, List<T> encountered) {
        List<List<T>> result=null, temp2;

        if(from==null) return result;

        if (from.equals(lookingFor)) {
            List<T> temp=new ArrayList<>();
            temp.add(from);
            result=new ArrayList<>();
            result.add(temp);
            return result;
        }

        if(encountered==null) encountered=new ArrayList<>();
        encountered.add(from);

        for (gallery.routefinder.graph.GraphNode n : graph.getAllNodes())
            if (!encountered.contains((T)n)) {
                temp2=findMultipleRoutes(graph, (T) n, lookingFor, max, avoid, waypoints, new ArrayList<>(encountered));

                if (temp2 != null) {
                    for(List<T> list : temp2)
                        list.addFirst(from);
                    if(result==null) result=temp2;
                    else result.addAll(temp2);
                }
            }
        return result;
    }

    //Find the shortest path using BreadthFirst
    public static <T> List<GraphNode> bfsShortestPath(Graph graph, GraphNode from, T lookingFor, Set<String> avoid, List<GraphNode> waypoints) {
        List<List<GraphNode>> agenda=new ArrayList<>();
        List<GraphNode> firstAgendaPath=new ArrayList<>(),resultPath;
        firstAgendaPath.add(from);
        agenda.add(firstAgendaPath);
        resultPath=findPathBreadthFirst(agenda,null,lookingFor, avoid, waypoints);
        Collections.reverse(resultPath);
        return resultPath;
    }

    public static <T> List<GraphNode> findPathBreadthFirst(List<List<GraphNode>> agenda, List<GraphNode> encountered ,T lookingFor, Set<String> avoid, List<GraphNode> waypoints) {
        if(agenda.isEmpty()) return null;
        List<GraphNode> nextPath=agenda.removeFirst();
        GraphNode current=nextPath.getFirst();
        if(current.getRoom().equals(lookingFor)) return nextPath;
        if (encountered == null) encountered = new ArrayList<>();
        encountered.add(current);

        for (GraphNode n : current.getNodes()) {
            if (!encountered.contains(n)) {
                List<GraphNode> newPath=new ArrayList<>(nextPath);
                newPath.addFirst(n);
                agenda.add(newPath);
            }
        }
        return findPathBreadthFirst(agenda,encountered,lookingFor, avoid, waypoints);
    }

    //Find the shortest path using Dijkstra's
    public static class CostedPath {
        public double pathCost=0;
        public List<GraphNode> pathList=new ArrayList<>();
    }

    public static <T> CostedPath dijkstraShortestPath(Graph graph, GraphNode from, T lookingFor, Set<String> avoid, List<GraphNode> waypoints) {
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

                    return cp;
                }
                for (GraphEdge e : currentNode.getEdges())
                    if (!encountered.contains(e.getDestination())) {
                        e.getDestination().setDistanceFromStart((Integer.min((int) (e.getDestination().getDistanceFromStart()), (int) (currentNode.getDistanceFromStart() + e.getDistance()))));
                        if (!unEncountered.contains(e.getDestination())) unEncountered.add(e.getDestination());
                    }
                Collections.sort(unEncountered, (n1, n2) -> (int) (n1.getDistanceFromStart() - n2.getDistanceFromStart()));
            }
        }while (!unEncountered.isEmpty()) ;
        return null;
    }
}


