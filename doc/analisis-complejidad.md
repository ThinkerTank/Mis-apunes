# Análisis de complejidad

Cada tabla da la complejidad temporal en el mejor caso, caso promedio y peor caso, y la complejidad espacial adicional (memoria que la operación usa además de la estructura). La justificación sigue lo que el código hace paso a paso, no solo la forma de los ciclos. `n` es el número de elementos; en el grafo, `V` y `E` son vértices y aristas.

Los tiempos medidos se tomaron sobre la configuración de Palmira (V = 359, E = 639) con JDK 21; sirven para dimensionar, no como referencia absoluta.

---

## 1. LinkedList⟨T⟩

| Operación | Mejor | Promedio | Peor | Espacio | Justificación por trazado |
| --- | --- | --- | --- | --- | --- |
| `addFirst` | O(1) | O(1) | O(1) | O(1) | Crea un nodo, lo enlaza a `head` y mueve `head`; si la lista estaba vacía también fija `tail`. Cuatro asignaciones, sin ciclos |
| `addLast` | O(1) | O(1) | O(1) | O(1) | Gracias a `tail` no recorre: enlaza `tail.next` al nodo nuevo y mueve `tail` |
| `removeFirst` | O(1) | O(1) | O(1) | O(1) | Lee `head.value`, avanza `head` y, si quedó vacía, limpia `tail` |
| `removeLast` | O(1) | O(n) | O(n) | O(1) | Con un solo nodo delega en `removeFirst`; si no, el `while` avanza `previous` hasta el penúltimo nodo: `n − 2` pasos porque la lista es simple |
| `get(i)`, `set(i)` | O(1) | O(n) | O(n) | O(1) | `nodeAt` avanza `i` veces desde `head`; promedio `n/2` |
| `remove(x)` | O(1) | O(n) | O(n) | O(1) | Recorre hasta encontrar `x` (o hasta el final si no está) y desenlaza con `previous` |
| `removeIf(p)` | O(n) | O(n) | O(n) | O(1) | Visita cada nodo una vez, siempre |
| `sort(c)` | O(n log n) | O(n log n) | O(n log n) | O(log n) | Merge sort sobre nodos: divide con puntero lento/rápido (O(n) por nivel), `log n` niveles, mezcla reenlazando sin copiar. La pila de recursión mide `log n` |

## 2. Stack⟨T⟩ y Queue⟨T⟩

| Operación | Mejor | Promedio | Peor | Espacio | Justificación |
| --- | --- | --- | --- | --- | --- |
| `push` | O(1) | O(1) | O(1) | O(1) | Un `addFirst`: el tope es la cabeza |
| `pop`, `peek` | O(1) | O(1) | O(1) | O(1) | Una verificación de vacío más `removeFirst`/`getFirst` |
| `enqueue` | O(1) | O(1) | O(1) | O(1) | Un `addLast` sobre la referencia `tail` |
| `dequeue`, `front` | O(1) | O(1) | O(1) | O(1) | `removeFirst`/`getFirst` sobre `head` |
| `Queue.remove(x)` | O(1) | O(n) | O(n) | O(1) | Delegada en `LinkedList.remove`; solo la usa la cola de despacho |

Si la pila estuviera en la cola de la lista, `pop` costaría O(n) porque `removeLast` recorre la lista simple. Por eso el tope es la cabeza.

## 3. HashTable⟨K, V⟩

Sea `m` el número de buckets y `α = n/m ≤ 0.75` el factor de carga.

| Operación | Mejor | Promedio | Peor | Espacio | Justificación |
| --- | --- | --- | --- | --- | --- |
| `put` | O(1) | O(1) amortizado | O(n) | O(1) amortizado | Calcula el bucket (O(1)), busca la clave en su cadena (longitud esperada ≤ α) e inserta al inicio. Si `(n+1)/m > 0.75`, `resize` duplica `m` y reinserta todo: O(n + m) esa vez, pero como la capacidad se duplica, la suma de todos los redimensionamientos durante `n` inserciones es ≤ 2n → O(1) amortizado. Peor caso: todas las claves en un bucket |
| `get`, `containsKey` | O(1) | O(1) | O(n) | O(1) | Bucket en O(1) y recorrido de una cadena de longitud esperada α. El peor caso solo ocurre si `hashCode` colisiona para todas las claves (la prueba `twoKeysInTheSameBucketAreBothRetrievable` lo fuerza a propósito) |
| `remove` | O(1) | O(1) | O(n) | O(1) | Igual que `get` más `LinkedList.remove` en esa misma cadena |
| `keys`, `values` | O(n + m) | O(n + m) | O(n + m) | O(n) | Recorre los `m` buckets y sus `n` entradas |

## 4. PriorityQueue⟨T⟩ (heap binario)

La altura del heap es `h = ⌊log₂ n⌋` porque el árbol es completo.

| Operación | Mejor | Promedio | Peor | Espacio | Justificación |
| --- | --- | --- | --- | --- | --- |
| `insert` | O(1) | O(log n) | O(log n) | O(1) amortizado | Coloca el elemento en `heap[n]` y `siftUp` lo compara con su padre: una sola comparación si ya está bien (mejor caso), hasta `h` si sube a la raíz. El arreglo se duplica al llenarse: O(1) amortizado |
| `extractMax` | O(1) | O(log n) | O(log n) | O(1) | Mueve la última hoja a la raíz y `siftDown` la baja: en cada nivel compara los dos hijos y el elemento (2 comparaciones) durante a lo sumo `h` niveles |
| `peekMax` | O(1) | O(1) | O(1) | O(1) | El máximo siempre está en `heap[0]` |
| `remove(x)` | O(1) | O(n) | O(n) | O(1) | Busca linealmente `x` (el heap no está ordenado), y luego `siftDown` + `siftUp` en O(log n) |

## 5. BinarySearchTree⟨T⟩

`h` es la altura del árbol.

| Operación | Mejor | Promedio | Peor | Espacio | Justificación |
| --- | --- | --- | --- | --- | --- |
| `insert` | O(1) | O(log n) | O(n) | O(h) | Desciende comparando hasta un hijo nulo: `h` pasos. La recursión ocupa `h` marcos |
| `remove` | O(1) | O(log n) | O(n) | O(h) | Desciende hasta el nodo; con dos hijos busca el sucesor (bajando por la izquierda del subárbol derecho) y lo elimina, siempre dentro de un mismo camino de longitud ≤ h |
| `contains`, `max`, `min` | O(1) | O(log n) | O(n) | O(1) | Un solo camino desde la raíz, iterativo |
| `inOrder`, `reverseInOrder` | O(n) | O(n) | O(n) | O(n) | Visita cada nodo exactamente una vez |

---

## 6. ¿BST o cola de prioridad para "el incidente más prioritario"? (RT13)

Las dos estructuras usan el mismo `IncidentPriorityComparator`.

| | BST (APO II) | Heap binario (CyED I) |
| --- | --- | --- |
| Insertar un incidente | O(log n) promedio, **O(n) peor** | **O(log n) garantizado** |
| Consultar el más prioritario | O(h): bajar por la derecha | **O(1)**: `heap[0]` |
| Extraer el más prioritario | O(h) | O(log n) garantizado |
| Listar todos por prioridad | **O(n)** con recorrido en orden | O(n log n): habría que extraerlos todos |
| Buscar por gravedad (filtrar) | **O(n)** sobre la lista ordenada, ya en orden | O(n) y luego ordenar |
| Eliminar uno arbitrario (por ID) | O(h) con el comparador | O(n) por la búsqueda lineal |

**Un hallazgo del propio dominio.** Los incidentes llegan en orden de tiempo, y el desempate dice que el más antiguo es el más prioritario. Por eso cada incidente nuevo de una gravedad es *menor* que todos los anteriores de esa gravedad, y siempre se inserta a la izquierda de ellos. El BST tiende a degenerar en tres cadenas, una por gravedad, con altura cercana a `n/3`. La prueba `sortedInsertionsDegenerateIntoALinearHeight` muestra el mismo efecto con inserciones ordenadas. El heap no sufre esto: su altura es `⌊log₂ n⌋` sin importar el orden de llegada.

**Por qué el sistema mantiene ambas.** Cada una resuelve mejor una pregunta distinta:

- El **heap** responde "¿cuál atiendo ahora?" (`peekMax` en O(1) para el Panel de Incidentes y `extractMax` en O(log n) para el botón *Atender siguiente*), con costo garantizado sin importar el orden de llegada.
- El **BST** responde "muéstreme todos, en orden" y "solo los de gravedad alta": un recorrido en orden entrega la lista ya ordenada en O(n), algo que el heap no puede hacer sin destruirse.

Con el tope de 12 incidentes activos de la simulación, la degeneración del BST no se nota en tiempo real, pero el análisis muestra por qué "atender siguiente" no debe depender del BST.

---

## 7. Algoritmos de grafos (RT16)

| Algoritmo | Tiempo | Espacio | Justificación | Medido en Palmira |
| --- | --- | --- | --- | --- |
| BFS | O(V + E) con lista, O(V²) con matriz | O(V) | Cada vértice entra una vez a la `Queue` (se marca al encolar) y al salir se recorren sus vecinos una vez: Σ grado(v) = 2E. Con matriz, recorrer vecinos cuesta O(V) por vértice | 0.16 ms |
| DFS | O(V + E) con lista, O(V²) con matriz | O(V) | Versión iterativa con una `Stack` de marcos (vértice, iterador de vecinos): cada vértice se apila una vez y cada iterador avanza por sus vecinos una sola vez. Iterativa para no desbordar la pila de llamadas en mapas grandes | — |
| Dijkstra | O((V + E) log V) | O(V + E) | Min-heap propio con *lazy deletion*: en lugar de disminuir una clave, se inserta la distancia mejorada y al extraer se descartan entradas viejas. Cada relajación exitosa inserta (≤ E veces, O(log E) = O(log V) cada una) y cada vértice se fija una vez | 0.21 ms |
| Floyd-Warshall | Θ(V³) | Θ(V²) | Tres ciclos anidados sobre los vértices, sin salida temprana (solo se salta `i` cuando `D[i][k] = ∞`). Matrices de distancias y de sucesores de V × V | 42 ms |
| Prim | O(E log V) | O(E) | Versión perezosa con la `PriorityQueue` propia como min-heap de aristas candidatas: cada arista entra y sale a lo sumo una vez (O(log E) = O(log V) cada operación) | 0.21 ms con lista, 0.44 ms con matriz |
| Kruskal | O(E log E) | O(V + E) | Ordenar las aristas con el merge sort de la `LinkedList` cuesta O(E log E); luego E operaciones de Union-Find a O(α(V)) cada una | 0.39 ms |

### Mejora de Union-Find

Sin optimizaciones, `union` puede encadenar los conjuntos en una lista y `find` costar O(n). La implementación combina:

- **Unión por rango.** El árbol de menor rango cuelga del de mayor, así que la altura crece solo cuando se unen dos árboles de igual rango: ningún árbol supera `log₂ n` de altura.
- **Compresión de caminos.** `find` recorre una vez hasta la raíz y en una segunda pasada hace que cada nodo visitado apunte directamente a ella.

Juntas dan O(m · α(n)) para `m` operaciones, donde α es la inversa de la función de Ackermann (≤ 4 para cualquier `n` práctico). La prueba `longChainStillResolvesToOneRoot` encadena 1 000 uniones y verifica que todos lleguen a la misma raíz.

### Dijkstra frente a Floyd-Warshall (RF33)

| | Dijkstra bajo demanda | Floyd-Warshall precalculado |
| --- | --- | --- |
| Costo de preparación | Ninguno | Θ(V³): 42 ms en Palmira |
| Costo por consulta | O((V + E) log V): ~0.21 ms | O(1): dos búsquedas en la tabla hash y un acceso a la matriz |
| Memoria | O(V + E) por consulta | Θ(V²) permanente |
| ¿Refleja la congestión? | Sí: corre sobre `trafficRoads`, cuyos pesos cambian cada minuto | No: corre sobre distancias base y solo se recalcula al bloquear o desbloquear una vía |

Cuarenta y dos milisegundos equivalen a unas 200 ejecuciones de Dijkstra. El sistema usa cada uno donde gana: Floyd-Warshall para las preguntas estructurales que se repiten mucho (tiempo de respuesta por zona, ranking de vehículos candidatos, "¿qué tan lejos del óptimo quedó esta decisión?"), y Dijkstra para la pregunta operativa de cada asignación: por qué calles va la ambulancia *ahora*, con el tráfico de este minuto.

### Prim frente a Kruskal (RF34)

Los dos producen el mismo costo total en Palmira (473.75, 358 tramos) y en el grafo de prueba con pesos empatados (`primAndKruskalAgreeOnCostWhenWeightsTie`), aunque pueden elegir tramos distintos cuando hay empates.

- **Prim** crece un único árbol desde un vértice. Con min-heap es O(E log V); en un grafo **denso** (E ≈ V²) su variante con arreglos llega a O(V²), mejor que ordenar V² aristas.
- **Kruskal** ordena todas las aristas una vez y no necesita un vértice inicial; en un grafo **disperso** (E ≈ V) el ordenamiento es barato y resulta muy simple. Además, si el grafo está desconectado, entrega un bosque completo (un árbol por componente), mientras que Prim solo cubre la componente de su vértice inicial.

La red vial es dispersa (E ≈ 1.8 V), así que ambos son baratos aquí; la diferencia práctica es que Kruskal sigue dando corredores para toda la ciudad cuando un bloqueo la parte en dos.
