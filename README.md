Multi-Agent Traffic Simulation with BDI Agents
Project Overview
A Java-based traffic simulator where autonomous vehicles (BDI agents) navigate urban environments using logical reasoning. Agents make decisions based on perceptions of traffic lights, obstacles, and other vehicles.


Key Features
BDI Architecture:

Beliefs: Environmental perceptions (traffic lights, obstacles)

Desires: Goals (reach destination, avoid collisions)

Intentions: Actions (accelerate, turn, stop)

Logical Reasoning: Interpreter Pattern for evaluating traffic conditions

Implementation
Core BDI components: Vehicle, BeliefInitial, Desire, Intention

Logical formulas: AndFormula, NotFormula, AtomFormula

Environment: Lane, Road, TrafficLight classes

How to Run
Clone repository

Copy
exucute the main file main.java which contain all implémentation of the scenario, change as you want to test it 

Scenarios Tested
Free-flow traffic

Congested traffic

Results Analysis
Travel time efficiency

Collision avoidance rate

Lane change average

Report
Includes full system design, UML diagrams, code explanation, and experimental results.
