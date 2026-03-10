import random
import math
import numpy as np

class RouteOptimizer:
    def __init__(self, coordinates, population_size=50, generations=100, mutation_rate=0.01):
        """
        Coordinates is a list of (lat, lon) tuples.
        """
        self.coordinates = coordinates
        self.population_size = population_size
        self.generations = generations
        self.mutation_rate = mutation_rate
        self.num_points = len(coordinates)

    def _distance(self, p1, p2):
        """Euclidean distance between two points."""
        return math.sqrt((p1[0] - p2[0])**2 + (p1[1] - p2[1])**2)

    def _total_distance(self, path):
        """Calculate total distance of a given path."""
        distance = 0
        for i in range(len(path) - 1):
            distance += self._distance(self.coordinates[path[i]], self.coordinates[path[i+1]])
        return distance

    def _create_individual(self):
        """Creates a random path."""
        path = list(range(self.num_points))
        random.shuffle(path)
        return path

    def _crossover(self, parent1, parent2):
        """Ordered crossover for TSP."""
        start = random.randint(0, self.num_points - 2)
        end = random.randint(start + 1, self.num_points - 1)
        
        child = [-1] * self.num_points
        child[start:end] = parent1[start:end]
        
        p2_remaining = [item for item in parent2 if item not in child]
        
        idx = 0
        for i in range(self.num_points):
            if child[i] == -1:
                child[i] = p2_remaining[idx]
                idx += 1
        return child

    def _mutate(self, individual):
        """Swap mutation."""
        for i in range(self.num_points):
            if random.random() < self.mutation_rate:
                j = random.randint(0, self.num_points - 1)
                individual[i], individual[j] = individual[j], individual[i]
        return individual

    def optimize(self):
        if self.num_points <= 2:
            return list(range(self.num_points))

        # Initial population
        population = [self._create_individual() for _ in range(self.population_size)]

        for _ in range(self.generations):
            # Sort by fitness (shorter distance = better)
            population = sorted(population, key=lambda x: self._total_distance(x))
            
            # Selection (Take top 20%)
            next_generation = population[:int(self.population_size * 0.2)]
            
            # Breeding
            while len(next_generation) < self.population_size:
                p1, p2 = random.sample(population[:50], 2)
                child = self._crossover(p1, p2)
                child = self._mutate(child)
                next_generation.append(child)
            
            population = next_generation

        best_path_indices = sorted(population, key=lambda x: self._total_distance(x))[0]
        return [self.coordinates[i] for i in best_path_indices]
