# Aplicación del método de la ingeniería — Cola de prioridad de atención

Aplicación del método completo (RT14) a la estructura que decide qué incidente se atiende primero.

## 1. Identificación del problema

El operador del centro de monitoreo recibe incidentes de forma continua y debe atender primero los más graves. Entre incidentes de igual gravedad, debe atender primero el más antiguo. El Panel de Incidentes necesita mostrar en todo momento cuál es el incidente pendiente más prioritario, y el botón *Atender siguiente* debe sacarlo para asignarle un vehículo.

**Necesidades concretas.**

- Consultar el más prioritario muchas veces por segundo (la interfaz se refresca cuatro veces por segundo).
- Insertar cada incidente nuevo en cuanto aparece, desde el hilo generador.
- Extraer el más prioritario cuando el operador pide atender el siguiente.
- Quitar un incidente arbitrario cuando recibe vehículo por otra vía (asignación manual desde la tabla).
- No usar `java.util.PriorityQueue` ni otras colecciones de Java.

**Definición del problema.** Diseñar una estructura que mantenga los incidentes pendientes ordenados por (gravedad descendente, antigüedad ascendente) y que permita consultar el máximo muy rápido, e insertar y extraer con costo predecible.

## 2. Recopilación de información

- Tamaño esperado: la simulación mantiene como máximo 12 incidentes activos, pero el operador puede registrar más a mano, y las pruebas de concurrencia insertan 200.
- Frecuencia: la consulta del máximo ocurre unas 4 veces por segundo; las inserciones, una cada 6 minutos simulados en promedio; las extracciones, cuando el operador decide.
- Orden de llegada: los incidentes llegan ordenados por tiempo, de modo que dentro de una misma gravedad cada uno es menos prioritario que los anteriores. Esto importa para estructuras que dependen del orden de inserción.
- La misma estructura sirve como min-heap para Dijkstra y Prim (Entrega 2) si el comparador se invierte.

## 3. Búsqueda de soluciones creativas

1. **Lista enlazada sin orden.** Insertar al final; buscar el máximo recorriendo toda la lista.
2. **Lista enlazada ordenada.** Insertar en su posición; el máximo siempre al frente.
3. **Árbol binario de búsqueda.** El máximo es el nodo más a la derecha.
4. **Heap binario sobre arreglo propio.** El máximo siempre en la posición 0.
5. **Tres colas FIFO, una por gravedad.** Revisar primero la de gravedad alta.

## 4. Transición de las ideas a los diseños preliminares

- Las **tres colas FIFO** (5) se descartan: funcionan solo porque hoy hay tres gravedades y porque el desempate coincide con el orden de llegada. Si el criterio cambia (por ejemplo, desempatar por distancia al centro), deja de servir, y además no reutiliza el comparador compartido con el BST.
- La **lista sin orden** (1) hace que consultar el máximo cueste O(n) en cada refresco de pantalla: es justo la operación más frecuente.
- Quedan la lista ordenada (2), el BST (3) y el heap (4).

## 5. Evaluación y selección

| Criterio | Peso | Lista ordenada | BST | Heap binario |
| --- | --- | --- | --- | --- |
| Consultar el máximo (la operación más frecuente) | 30 % | O(1) → 5 | O(h) → 3 | O(1) → 5 |
| Insertar | 25 % | O(n) → 2 | O(log n) prom., O(n) peor; aquí tiende al peor por el orden de llegada → 2 | O(log n) garantizado → 5 |
| Extraer el máximo | 20 % | O(1) → 5 | O(h) → 3 | O(log n) → 4 |
| Reutilizable como min-heap en Dijkstra y Prim | 15 % | Sí, pero con inserción O(n) haría a Dijkstra O(V·E) → 2 | Posible, más complejo → 3 | Sí, solo invirtiendo el comparador → 5 |
| Simplicidad de implementación | 10 % | 5 | 3 | 4 |
| **Total ponderado** | | **3.80** | **2.75** | **4.70** |

**Selección: heap binario sobre un arreglo propio.** Es el único que garantiza O(1) en la consulta y O(log n) en inserción y extracción sin importar el orden de llegada, y es exactamente el min-heap que necesitan Dijkstra y Prim.

## 6. Preparación de informes y especificaciones

- TAD `PriorityQueue⟨T⟩` con valores, invariante de heap y pre/poscondiciones: ver [tad-estructuras.md](tad-estructuras.md#tad-priorityqueuet-heap-binario).
- Complejidad por operación justificada: ver [analisis-complejidad.md](analisis-complejidad.md#4-priorityqueuet-heap-binario).
- Comparación con el BST y razones para mantener ambos: ver [analisis-complejidad.md](analisis-complejidad.md#6-bst-o-cola-de-prioridad-para-el-incidente-más-prioritario-rt13).

## 7. Implementación del diseño

- `model/structures/PriorityQueue.java`: arreglo `Object[]` que se duplica al llenarse; `siftUp` y `siftDown` mueven el elemento con un "hueco" en lugar de intercambiar en cada nivel; `remove` busca linealmente y repara en ambos sentidos.
- `model/IncidentPriorityComparator.java`: el criterio único (gravedad → antigüedad → secuencia de creación), serializable para que la cola se guarde con la partida.
- `model/DispatchCenter.java`: la cola `attentionQueue` recibe cada incidente nuevo; `mostUrgentPending()` usa `peekMax()`; `attendNext()` usa `extractMax()` y, si no hay vehículo compatible, reinserta el incidente; `assignVehicle()` usa `remove()` cuando el operador elige un incidente de la tabla.
- `model/structures/AbstractGraph.java`: Dijkstra y Prim usan la misma clase con el comparador invertido.

## 8. Pruebas

| Prueba | Qué verifica |
| --- | --- |
| `PriorityQueueTest.extractMaxReturnsElementsByPriority` | El orden de extracción respeta la prioridad |
| `PriorityQueueTest.tieOnPriorityIsBrokenByAge` | Empate de prioridad: sale primero el más antiguo |
| `PriorityQueueTest.peekMaxDoesNotRemoveAndEmptyQueueThrows` | `peekMax` no modifica; cola vacía lanza `EmptyStructureException` |
| `PriorityQueueTest.reversedComparatorBehavesAsMinHeap` | El mismo código sirve como min-heap |
| `PriorityQueueTest.removeArbitraryElementKeepsHeapOrder` | Quitar un elemento intermedio conserva la propiedad de heap |
| `PriorityQueueTest.manyRandomInsertionsComeOutSorted` | 500 inserciones aleatorias salen ordenadas |
| `DispatchCenterTest.attendNextTakesTheMostUrgentAndBreaksTiesByAge` | Integración: *Atender siguiente* toma el incidente correcto |
| `DispatchCenterTest.attendNextWithoutCompatibleVehicleKeepsTheIncidentPending` | Si no hay vehículo, el incidente vuelve a la cola |
| `GraphContractTest.dijkstra*`, `primAndKruskal*` | El heap como min-heap dentro de Dijkstra y Prim |
