# Reporte de pruebas

Ejecución de la suite completa de JUnit 5 sobre la rama `develop` en la iteración 15. El diseño de cada caso (escenario, entrada, resultado esperado) está en [especificacion.md](especificacion.md#diseño-de-casos-de-prueba).

**Resumen:** 142 pruebas, 0 fallos, 0 errores, 0 omitidas. Tiempo total: 1.1 s.

## Resultados por clase de prueba

| Paquete | Clase de prueba | Pruebas | Fallos | Tiempo (s) |
| --- | --- | ---: | ---: | ---: |
| structures | `LinkedListTest` | 7 | 0 | 0.000 |
| structures | `StackTest` | 5 | 0 | 0.000 |
| structures | `QueueTest` | 5 | 0 | 0.000 |
| structures | `HashTableTest` | 6 | 0 | 0.000 |
| structures | `PriorityQueueTest` | 6 | 0 | 0.006 |
| structures | `BinarySearchTreeTest` | 7 | 0 | 0.004 |
| structures | `UnionFindTest` | 4 | 0 | 0.016 |
| structures | `AdjacencyListGraphTest` (hereda `GraphContractTest`) | 15 | 0 | 0.002 |
| structures | `AdjacencyMatrixGraphTest` (hereda `GraphContractTest`) | 16 | 0 | 0.000 |
| model | `CityMapTest` | 10 | 0 | 0.048 |
| model | `IncidentRepositoryTest` | 5 | 0 | 0.000 |
| model | `VehicleAndNavigatorTest` | 5 | 0 | 0.027 |
| model | `DispatchCenterTest` | 13 | 0 | 0.068 |
| model | `ScoreCalculatorTest` | 4 | 0 | 0.000 |
| simulation | `AccidentDetectorTest` | 7 | 0 | 0.003 |
| simulation | `IncidentGeneratorTest` | 4 | 0 | 0.014 |
| simulation | `SimulationTest` | 4 | 0 | 0.037 |
| simulation | `ConcurrencyTest` | 2 | 0 | 0.460 |
| persistence | `JsonParserTest` | 5 | 0 | 0.006 |
| persistence | `ConfigLoaderTest` | 6 | 0 | 0.031 |
| persistence | `SimulationRepositoryTest` | 4 | 0 | 0.159 |
| (raíz) | `ProjectRulesTest` | 2 | 0 | 0.076 |
| **Total** | | **142** | **0** | **1.117** |

## Qué cubre la suite

- **Estructuras:** operaciones normales, casos límite (vacía, un elemento, crecimiento y redimensionamiento, colisiones en la tabla hash) y la excepción `EmptyStructureException`.
- **Grafo:** las mismas 15 pruebas de contrato (`GraphContractTest`) corren sobre la lista y la matriz de adyacencia, así que las dos representaciones devuelven exactamente lo mismo en BFS, DFS, Dijkstra, Floyd-Warshall, Prim y Kruskal. La matriz tiene una prueba propia adicional.
- **Dominio:** mapa como grafo (pesos por zona, congestión, bloqueo de vías, puntos de riesgo), prioridad de incidentes, consistencia BST + hash, asignación validada con todas sus excepciones, puntuación.
- **Simulación:** cada criterio de accidente por separado con un generador aleatorio controlado, enfriamientos, generación por zona y tope de incidentes activos.
- **Concurrencia:** productores, consumidores y reloj sobre el mismo `DispatchCenter`; al final todas las estructuras son consistentes.
- **Persistencia:** JSON válido e inválido (mensajes con línea y columna), configuración inconsistente, guardar y recuperar una partida idéntica, archivo dañado o que no es una partida.
- **Reglas del proyecto:** ninguna colección de `java.util` en el código fuente y Javadoc de complejidad en cada clase (esta prueba encontró 21 clases incumplidas en la iteración 11; se corrigieron en la 12).

## Fallos registrados durante el desarrollo

| Iteración | Prueba | Causa | Corrección |
| --- | --- | --- | --- |
| 11 | `ProjectRulesTest.everyClassDocumentsTheComplexityOfAtLeastTwoPublicOperations` | 21 clases sin el Javadoc de complejidad exigido por la regla técnica | Iteración 12: Javadoc "Complexity —" con el análisis real de cada método |

La interfaz gráfica no tiene pruebas automáticas: se verificó ejecutándola (los tres escenarios, navegación, capas del mapa, asignación y resolución, guardar y cargar).

## Cómo reproducir

```bash
mvn test
```

Los reportes quedan en `target/surefire-reports/`.
