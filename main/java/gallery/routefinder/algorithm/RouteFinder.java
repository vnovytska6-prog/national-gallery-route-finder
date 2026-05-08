package gallery.routefinder.algorithm;

import gallery.routefinder.graph.GraphEdge;
import gallery.routefinder.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteFinder {

    //Find all paths using DepthFirst
    public static <T> List<List<GraphNode>> findAllPathsDepthFirst(GraphNode from, List<GraphNode> encountered, T lookingFor) {
        List<List<GraphNode>> result=null, temp2;

        if (from.getRoom().equals(lookingFor)) {
            List<GraphNode> temp=new ArrayList<>();
            temp.add(from);
            result=new ArrayList<>();
            result.add(temp);
            return result;
        }

        if (encountered == null) encountered = new ArrayList<>();
        encountered.add(from);

        for (GraphNode n : from.getNodes())
            if (!encountered.contains(n)) {
                temp2=findAllPathsDepthFirst(n, new ArrayList<>(encountered), lookingFor);

                if (temp2 != null) {
                    for(List<GraphNode> list : temp2)
                        list.add(0,from);
                    if(result==null) result=temp2;
                    else result.addAll(temp2);
                }
            }
        return result;
    }

    //Find the shortest path using BreadthFirst
    public static <T> List<GraphNode> findPathBreadthFirst(GraphNode from, T lookingFor) {
        List<List<GraphNode>> agenda=new ArrayList<>();
        List<GraphNode> firstAgendaPath=new ArrayList<>(),resultPath;
        firstAgendaPath.add(from);
        agenda.add(firstAgendaPath);
        resultPath=findPathBreadthFirst(agenda,null,lookingFor);
        Collections.reverse(resultPath);
        return resultPath;
    }

    public static <T> List<GraphNode> findPathBreadthFirst(List<List<GraphNode>> agenda, List<GraphNode> encountered ,T lookingFor) {
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
        return findPathBreadthFirst(agenda,encountered,lookingFor);
    }

    //Find the shortest path using Dijkstra's
    public static class CostedPath {
        public double pathCost=0;
        public List<GraphNode> pathList=new ArrayList<>();
    }

    public static <T> CostedPath findCheapestPathDijkstra(GraphNode from, T lookingFor){
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


