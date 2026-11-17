# SGMMS — Sistema de Gestión y Monitoreo de Movilidad y Seguridad

Prototipo de escritorio en Java para el centro de monitoreo urbano de Palmira. El operador observa la ciudad en un mapa 2D, recibe incidentes (accidentes, robos e incendios), los prioriza por gravedad y despacha vehículos de atención por la ruta de menor costo.

Tarea integradora 2026-2 de los cursos Algoritmos y Programación II, Computación y Estructuras Discretas I e Ingeniería de Software II — Universidad Icesi.

## Equipo

| Nombre | Código |
| --- | --- |
| _(pendiente)_ | _(pendiente)_ |
| _(pendiente)_ | _(pendiente)_ |
| _(pendiente)_ | _(pendiente)_ |

## Indicadores de calidad

Fórmulas: densidad de errores-fallos = total de fallos / total de pruebas; confiabilidad = 1 − densidad; completitud = casos de prueba / total de funcionalidades (35).

| Iteración | Commit medido | Alcance | Pruebas | Fallos | Densidad | Confiabilidad | Completitud |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ddb4b080faf8d1cbbd0bf2309301325492a03e02` | LinkedList, Stack, Queue | 17 | 0 | 0.0000 | 1.0000 | 0.49 |
| 2 | `a7af55f2a1627041b7611ef61a0175dde9764928` | HashTable | 23 | 0 | 0.0000 | 1.0000 | 0.66 |
| 3 | `92801d664c486b24033c86765a9af1068049ab65` | PriorityQueue, BinarySearchTree | 36 | 0 | 0.0000 | 1.0000 | 1.03 |
| 4 | `42e48a1b4b6f46651e3360d0084bd641ee867643` | Grafo dual, BFS, DFS, Dijkstra, Floyd-Warshall, Prim, Kruskal | 71 | 0 | 0.0000 | 1.0000 | 2.03 |
| 5 | `1a40cc31e5bfb2c5fb89787d4ea598d8d31ea1bf` | Mapa como grafo, incidentes, vehículos, repositorios, navegador | 91 | 0 | 0.0000 | 1.0000 | 2.60 |
| 6 | `e05d80443ce31e30bf1187b5518a3008fe2ce081` | Despacho, asignación validada, Dijkstra, Floyd-Warshall, puntaje | 108 | 0 | 0.0000 | 1.0000 | 3.09 |
| 7 | `83a29aa56c05fba352215a94ad5bf077672042c9` | Generación de incidentes, tráfico civil, 3 criterios de accidente | 122 | 0 | 0.0000 | 1.0000 | 3.49 |
| 8 | `148adcb8aa0bbf848fa6067d8bcf82a24f5906e2` | Configuración JSON (parser propio y validación) | 133 | 0 | 0.0000 | 1.0000 | 3.80 |
| 9 | `83e03921c8f8e1a9acb23da86e710b29fc7030de` | Serialización y concurrencia (3 hilos) | 139 | 0 | 0.0000 | 1.0000 | 3.97 |
| 10 | `10066547863ccac5f58de42043b2b16e9a99c602` | Interfaz JavaFX MVC (3 escenarios), calibración de la simulación | 140 | 0 | 0.0000 | 1.0000 | 4.00 |
| 11 | `f089350721db1a5bccb968902642c1b2f62c6b6b` | Prueba de reglas técnicas RT1 y RT5 (detecta 21 clases sin Javadoc de complejidad) | 142 | 1 | 0.0070 | 0.9930 | 4.06 |
| 12 | `c451c27d1d6a0883658ae1e0b5fa7798960f001a` | Javadoc de complejidad en todas las clases (corrige la falla de la iteración 11) | 142 | 0 | 0.0000 | 1.0000 | 4.06 |
