# National Gallery Route Finder

A JavaFX route-planning application developed as a college project. It models rooms in the National Gallery in London as a graph and uses different algorithms to generate and compare routes.

## Demo

The demo shows route generation using different algorithms, required waypoints and rooms to avoid.

<img width="1000" alt="National Gallery Route Finder demo" src="https://github.com/user-attachments/assets/b1b4f04f-d93d-4708-b775-4a420bd564bb" />

## Features

* Find routes between selected gallery rooms
* Compare DFS, BFS and Dijkstra's algorithms
* Add required waypoints
* Select rooms to avoid
* View routes on an interactive gallery map
* Load room, connection and artwork data from CSV files

## Route with Waypoints and Rooms to Avoid

<img width="1000" alt="Route with waypoints and rooms to avoid" src="https://github.com/user-attachments/assets/1e1d7804-2794-4127-9baf-3ea0fefb5fe9" />

## Technologies

* Java
* JavaFX
* Maven
* JUnit
* Graph data structures and algorithms
* CSV data
* Object-oriented programming

## How to Run

1. Clone the repository.
2. Open the project in IntelliJ IDEA.
3. Allow Maven to load the required dependencies.
4. Run `gallery.routefinder.Launcher`.

## What I Learned

Building this project helped me understand how DFS, BFS and Dijkstra’s algorithm can produce different routes through the same graph. I learned how to include waypoints and rooms to avoid while keeping the route information and map display updated. I also gained practical experience with JavaFX, Maven and loading application data from CSV files.
