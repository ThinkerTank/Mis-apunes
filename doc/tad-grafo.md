# TAD Graph⟨T⟩ y representación dual del mapa

## Especificación

Grafo no dirigido con pesos no negativos. Las dos representaciones del proyecto, `AdjacencyListGraph` y `AdjacencyMatrixGraph`, implementan la misma interfaz `IGraph<T>` (RT15), y los seis algoritmos están escritos una sola vez en `AbstractGraph`.

**Valores.** Pares `G = (V, E)` donde `V ⊆ T` es finito y `E ⊆ {{u, v} : u, v ∈ V, u ≠ v}`, con una función de peso `w : E → ℝ≥0`.

**Invariante.** No hay lazos (`u ≠ v`), no hay aristas repetidas (agregar una arista existente reemplaza su peso) y todo peso es finito y no negativo. Cada vértice recibe un índice consecutivo `0 … |V| − 1` guardado en una `HashTable<T, Integer>`.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `addVertex(v)` | Modificadora | `v ≠ null` | `V′ = V ∪ {v}`; retorna `false` si ya estaba |
| `addEdge(a, b, w)` | Modificadora | `a, b ∈ V`, `a ≠ b`, `w ≥ 0` finito | `E′ = E ∪ {{a, b}}` con peso `w` (si existía, solo cambia el peso). Si un vértice no existe: `VertexNotFoundException` |
| `removeEdge(a, b)` | Modificadora | `a, b ∈ V` | `E′ = E ∖ {{a, b}}`; retorna si existía |
| `updateWeight(a, b, w)` | Modificadora | `{a, b} ∈ E`, `w ≥ 0` | `w′({a, b}) = w` (se usa al cambiar la congestión) |
| `getAdjacent(v)` | Analizadora | `v ∈ V` | `{u : {u, v} ∈ E}` |
| `getWeight(a, b)` | Analizadora | `a, b ∈ V` | `w({a, b})` o `+∞` si no son adyacentes |
| `bfs(s)` | Analizadora | `s ∈ V` | Para cada `v`: si es alcanzable desde `s` y su distancia mínima en número de aristas; lista de inalcanzables |
| `dfs()` | Analizadora | — | Componentes conexas de `G` y vértices donde se cierra un ciclo (extremos de aristas de retroceso) |
| `dijkstra(s)` | Analizadora | `s ∈ V` | Para cada `v`: `δ(s, v)` (suma mínima de pesos, `+∞` si es inalcanzable) y un camino que la logra |
| `floydWarshall()` | Analizadora | — | Matriz `D[u][v] = δ(u, v)` para todo par, y matriz de sucesores para reconstruir caminos |
| `primMST(s)` | Analizadora | `s ∈ V` | Árbol de expansión de peso mínimo de la componente de `s` |
| `kruskalMST()` | Analizadora | — | Bosque de expansión de peso mínimo de `G` (un árbol por componente) |
| `vertexCount()`, `edgeCount()`, `vertices()`, `edges()` | Analizadora | — | `|V|`, `|E|`, listas de vértices y aristas |

## Representación dual del mapa (RF29)

El mapa de Palmira existe dos veces, con propósitos distintos:

- **La cuadrícula** (`int[][]` con códigos 0 a 4) es lo que dibuja el Mapa de Tráfico y lo que se carga desde `city.json`.
- **El grafo** es lo que se analiza. Cada celda transitable es un vértice y cada par de celdas vecinas (arriba, abajo, izquierda, derecha) transitables está unido por un tramo de vía no dirigido.

El peso de un tramo es `w = distancia_base × factor_de_congestión`:

```
distancia_base(a, b)   = baseDistance × (factor(zona a) + factor(zona b)) / 2
                         factores: vía principal 1.0 · camino 1.5 · residencial 2.0 · comercial 2.5
factor_de_congestión   = 1 + congestionStep × vehículos en el distrito más cargado de los dos
```

Las vías principales son las más rápidas; las calles residenciales y comerciales son más lentas por peatones y vehículos estacionados. El factor de congestión conecta con el criterio de accidente "congestión crítica": los mismos conteos por distrito que encarecen las vías disparan el accidente cuando superan `zoneCapacity`.

`CityMap` mantiene tres copias del grafo, sincronizadas al bloquear y desbloquear vías:

| Copia | Representación | Pesos | Se usa para |
| --- | --- | --- | --- |
| `baseRoads` | Lista de adyacencia | Base (sin congestión) | BFS de conectividad, DFS de componentes y ciclos, Prim y Kruskal |
| `trafficRoads` | Lista de adyacencia | Con congestión, cambian cada minuto | Dijkstra (rutas de despacho) |
| `matrixRoads` | Matriz de adyacencia | Base | Floyd-Warshall |

Las listas se construyen sobre la `LinkedList` propia (RT2). Los grafos no se serializan: se reconstruyen desde la cuadrícula y la lista de vías bloqueadas al cargar una partida.

### Costo espacial: O(V²) frente a O(V + E)

En la configuración de Palmira hay **V = 359** celdas transitables. Cada vértice tiene a lo sumo cuatro vecinos, así que `E ≤ 2V`; en el mapa real `E = 639`.

| | Lista de adyacencia | Matriz de adyacencia |
| --- | --- | --- |
| Memoria | Θ(V + E) = 359 listas con 1 278 entradas (cada arista aparece dos veces) | Θ(V²): la matriz crece a 512 × 512 = 262 144 celdas `double` (~2 MB), de las cuales menos del 1 % tiene una arista |
| Leer el peso de `(u, v)` | O(grado(u)) ≤ O(4) | O(1) |
| Recorrer los vecinos de `u` | O(grado(u)) | O(V): hay que barrer la fila completa |
| BFS / DFS | O(V + E) | O(V²) |
| Dijkstra | O((V + E) log V) | O(V² log V) |

Para un grafo disperso como una cuadrícula urbana, la lista es mejor en casi todo. La matriz se mantiene porque Floyd-Warshall trabaja de todos modos sobre una matriz V × V de distancias, y porque el enunciado pide ambas representaciones detrás de la misma interfaz: las pruebas de `GraphContractTest` corren idénticas sobre las dos y verifican que den los mismos resultados.

## Algoritmos sobre el dominio

| Algoritmo | Requisito | Uso en el SGMMS |
| --- | --- | --- |
| BFS | RF30 | Al cargar el mapa, recorre desde el centro de monitoreo; si alguna zona queda sin alcanzar, `InvalidMapException` lista las celdas desconectadas. También calcula la cobertura (saltos promedio y máximos por zona) que muestra el Centro de Monitoreo |
| DFS | RF31 | (a) Componentes conexas cuando hay vías bloqueadas: reporta incidentes que quedan en una componente sin vehículos compatibles. (b) Ciclos de la red de vías principales: sus vértices de cruce son los **puntos de riesgo** del criterio de accidente 3 |
| Dijkstra | RF32 | Ruta de costo mínimo del vehículo al incidente con el tráfico actual; se dibuja en el mapa y su costo es el tiempo estimado de llegada. Usa la `PriorityQueue` propia como min-heap |
| Floyd-Warshall | RF33 | Matriz de distancias estructurales, recalculada solo cuando cambia la topología. Da el tiempo estimado de respuesta por zona en O(1) y ordena los vehículos candidatos |
| Prim y Kruskal | RF34 | Corredores de emergencia: MST sobre distancias base. Se dibuja como capa opcional; ambos dan el mismo costo (473.75 en Palmira, 358 tramos) |
| Dijkstra + Floyd-Warshall | RF35 | La puntuación premia enviar el vehículo de menor costo Dijkstra y penaliza enviar uno mucho más lejos que el óptimo según Floyd-Warshall |
