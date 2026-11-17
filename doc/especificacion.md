# Especificación de requerimientos, historias de usuario y diseño de casos de prueba

SGMMS — Sistema de Gestión y Monitoreo de Movilidad y Seguridad, prototipo para el centro de monitoreo urbano de Palmira.

**Actor principal.** El operador del centro de monitoreo: observa la ciudad, recibe incidentes, decide qué vehículo enviar y cierra la atención.

**Actores secundarios.** El reloj de simulación y el generador de incidentes (procesos automáticos que corren en hilos propios).

---

## 1. Requerimientos funcionales

### 1.1 Generales (RF1–RF20)

| ID | Nombre | Descripción | Entradas | Resultado | Implementación |
| --- | --- | --- | --- | --- | --- |
| RF1 | Gestionar los escenarios | Mostrar el Centro de Monitoreo, el Mapa de Tráfico y el Panel de Incidentes y navegar entre ellos | Clic en las pestañas o en los botones de acceso | Se muestra el escenario elegido | `MainController.show`, `Screen` |
| RF2 | Representar el mapa | Mostrar zonas residenciales, comerciales, vías principales, obstáculos, rutas, vehículos e incidentes | Estado del modelo | Mapa dibujado y escalado | `TrafficMapController.redraw`, `MapRenderer` |
| RF3 | Gestionar incidentes | Registrar y consultar incidentes con ID, tipo, ubicación, gravedad, fecha y hora, descripción, estado y vehículo | Tipo, gravedad, X, Y, descripción / ID | Incidente creado o encontrado | `DispatchCenter.reportIncident`, `findIncident` |
| RF4 | Generar incidentes | Robos e incendios aleatorios con probabilidad por zona; accidentes por tres criterios de comportamiento | Paso del tiempo | Incidentes nuevos | `IncidentGenerator`, `AccidentDetector` |
| RF5 | Prioridad de incidentes | Ordenar por gravedad y desempatar por antigüedad; consultar y atender el más prioritario | — | Incidente más prioritario | `IncidentPriorityComparator`, BST y heap |
| RF6 | Gestionar vehículos | Registrar y consultar vehículos, su tipo, ubicación y estado; controlar los cambios de estado | ID, tipo, X, Y | Vehículo registrado / fuera de servicio | `registerVehicle`, `setOutOfService` |
| RF7 | Asignar vehículos | Asignar solo vehículos disponibles y compatibles, rechazar asignaciones inválidas y proponer un candidato | Incidente, vehículo | Asignación o excepción | `assignVehicle`, `suggestVehicle` |
| RF8 | Gestionar la atención | Iniciar y finalizar la atención, actualizar estados y liberar el vehículo | Llegada del vehículo / clic en Resolver | Incidente resuelto, vehículo disponible | `startAttention`, `resolveIncident` |
| RF9 | Rutas y desplazamientos | Representar las conexiones entre zonas y calcular la ruta de cada vehículo | Vehículo, incidente | Ruta de celdas | `CityMap`, `RouteService.routesFrom` |
| RF10 | Analizar la red vial | Verificar conectividad, identificar zonas desconectadas y rutas de menor costo | Mapa | Componentes y rutas | `validateConnectivity`, `structuralAnalysis` |
| RF11 | Información de movilidad | Distancias y tiempos estimados entre puntos para apoyar la asignación | Dos puntos | Distancia / tiempo | `structuralDistance`, `responseTime` |
| RF12 | Corredores de emergencia | Mostrar la red mínima que conecta todas las zonas | Activar la capa | MST dibujado | `primCorridors`, `kruskalCorridors` |
| RF13 | Indicadores en tiempo real | Total de activos, accidentes, robos, incendios, vehículos disponibles y puntaje | — | Indicadores actualizados 4 veces por segundo | `Indicators`, `SimulationEngine` |
| RF14 | Puntaje del operador | Sumar puntos al resolver según gravedad, tiempo y ruteo | Resolución | Puntos | `ScoreCalculator` |
| RF15 | Configuración JSON | Cargar mapa, rutas y vehículos validando existencia, estructura y consistencia | `city.json`, `vehicles.json` | Simulación lista o `ConfigurationException` | `ConfigLoader`, `JsonParser` |
| RF16 | Persistencia | Guardar y recuperar la simulación con serialización | Archivo `.sgmms` | Estado restaurado o `PersistenceException` | `SimulationRepository` |
| RF17 | Historial | Registrar movimientos y acciones del operador y consultar la última | — | Pila de movimientos, pila de acciones, historial de eventos | `OperatorNavigator`, `actions`, `eventHistory` |
| RF18 | Eventos | Registrar y procesar eventos de generación, resolución y cambios de estado para refrescar la interfaz | — | Eventos consumidos en orden | `SimulationEvent`, `pollEvents` |
| RF19 | Errores de ejecución | Controlar situaciones inválidas con excepciones y mensajes claros, sin cerrar la aplicación | Cualquier operación inválida | Mensaje en la barra de estado | Paquete `exceptions`, `AppContext.perform` |
| RF20 | Procesos concurrentes | Ejecutar concurrentemente el reloj, la generación y la actualización de indicadores con acceso seguro | — | Tres hilos sobre un recurso sincronizado | `SimulationEngine`, `DispatchCenter` |

### 1.2 Computación y Estructuras Discretas I — Entrega 1 (RF21–RF28)

| ID | Nombre | Descripción | Estructura | Implementación |
| --- | --- | --- | --- | --- |
| RF21 | Historial de movimientos | Cada movimiento válido (W, A, S, D, flechas) se apila; U o el botón *Deshacer* lo deshace; deshacer sin historial lanza `EmptyStructureException` y la interfaz lo informa | `Stack<Movement>` | `OperatorNavigator` |
| RF22 | Pila de acciones de gestión | Asignar y resolver (y bloquear vías) se apilan como `OperatorAction`; el panel muestra la última con `peek` | `Stack<OperatorAction>` | `DispatchCenter.lastAction` |
| RF23 | Cola de despacho | Todo incidente sin vehículo se encola; el panel muestra el frente; al asignarse sale de la cola | `Queue<Incident>` | `dispatchQueue`, `nextInDispatchQueue` |
| RF24 | Cola de eventos | Los eventos del ciclo de simulación se encolan y la interfaz los consume | `Queue<SimulationEvent>` | `events`, `pollEvents` |
| RF25 | Registro de vehículos por ID | Consulta, asignación y cambio de estado por clave en O(1) esperado; ID inexistente lanza `VehicleNotFoundException` | `HashTable<String, Vehicle>` | `VehicleRepository` |
| RF26 | Índice de incidentes por ID | Consultar, asignar y resolver por ID con el índice; `IncidentNotFoundException` si no existe; consistencia atómica hash–BST | `HashTable<String, Incident>` + BST | `IncidentRepository` |
| RF27 | Cola de prioridad de atención | Heap con gravedad y antigüedad; tope en O(1); *Atender siguiente* extrae en O(log n) | `PriorityQueue<Incident>` | `attentionQueue`, `attendNext` |
| RF28 | Vehículo candidato | Proponer el vehículo disponible y compatible más adecuado; el operador confirma | `PriorityQueue<Vehicle>` | `suggestVehicle` |

### 1.3 Computación y Estructuras Discretas I — Entrega 2 (RF29–RF35)

| ID | Nombre | Descripción | Algoritmo | Implementación |
| --- | --- | --- | --- | --- |
| RF29 | Mapa como grafo | Representación dual: cuadrícula para la vista y grafo para el análisis; peso = distancia base × congestión; lista y matriz de adyacencia | — | `CityMap`, `IGraph` |
| RF30 | Conectividad | Al cargar, BFS desde el centro; zona inalcanzable → `InvalidMapException` con los vértices desconectados; cobertura en saltos | BFS | `validateConnectivity`, `coverage` |
| RF31 | Análisis estructural | (a) Componentes con vías bloqueadas e incidentes sin vehículos; (b) ciclos → puntos de riesgo del criterio de accidente | DFS | `isolatedIncidents`, `riskPoints` |
| RF32 | Ruta de despacho | Ruta de costo mínimo con congestión, dibujada en el mapa; su costo alimenta el tiempo estimado | Dijkstra (min-heap propio) | `assignVehicle`, `MapRenderer.drawRoutes` |
| RF33 | Matriz de tiempos | Precalcular distancias de todos los pares y recalcular al cambiar la topología; tiempo de respuesta por zona en O(1); refinar el candidato | Floyd-Warshall | `RouteService.distanceMatrix` |
| RF34 | Corredores de emergencia | MST sobre distancias base con ambos algoritmos, mismo costo, capa opcional | Prim y Kruskal (Union-Find propio) | `primCorridors`, `kruskalCorridors` |
| RF35 | Estrategia basada en grafos | Bonificación por enviar el vehículo de menor costo Dijkstra; penalización si supera 1.5 veces el óptimo de Floyd-Warshall | Dijkstra + Floyd-Warshall | `ScoreCalculator`, `AssignmentRecord` |

---

## 2. Requerimientos no funcionales

| ID | Requerimiento | Cómo se cumple |
| --- | --- | --- |
| RNF1 | Java en la versión del curso | Java 17 y JavaFX 17 (`pom.xml`, `maven.compiler.release = 17`) |
| RNF2 | Interfaz clara y consistente | Mismo encabezado, tarjetas, colores y convenciones en los tres escenarios; leyenda en el mapa |
| RNF3 | Mensajes claros | Toda operación muestra en la barra de estado un mensaje de éxito (verde) o de error (rojo) en español |
| RNF4 | Información actualizada | Hilo de indicadores cada 250 ms; refresco inmediato tras cada acción del operador |
| RNF5 | Consistencia de la información | BST e índice hash se actualizan en el mismo método sincronizado; `DispatchCenter.isConsistent()` lo verifica en las pruebas de concurrencia |
| RNF6 | Sin cierres inesperados | Excepciones de dominio capturadas en `AppContext.perform`; errores de hilos reportados por `SimulationListener.onError` sin detener el hilo |
| RNF7 | Recuperación consistente | Guardado con escritura atómica (archivo temporal + movimiento) y bloqueo de la simulación; los grafos se reconstruyen al cargar |
| RNF8 | Buenas prácticas de POO | MVC, paquetes con responsabilidades claras, código en inglés, Javadoc; `ProjectRulesTest` vigila reglas técnicas |

**Restricciones.** Sin bases de datos, sin internet, sin GPS, sin APIs externas, sin mapas reales y sin colecciones de Java para reemplazar las estructuras propias.

---

## 3. Historias de usuario

**HU1 — Ver el estado de la ciudad** (RF1, RF13) · Prioridad alta
Como operador quiero ver de un vistazo cuántos incidentes hay de cada tipo, cuántos vehículos están libres y mi puntaje, para saber si la situación está bajo control.
- *Dado* que la aplicación está abierta, *cuando* estoy en el Centro de Monitoreo, *entonces* veo nombre del sistema, puntaje, total de activos, accidentes, robos, incendios y vehículos disponibles.
- *Dado* que aparece un incidente, *cuando* pasa menos de un segundo, *entonces* los contadores ya lo reflejan.

**HU2 — Recorrer el mapa** (RF2, RF21) · Prioridad alta
Como operador quiero moverme por el mapa con el teclado y deshacer mis movimientos, para revisar zonas sin perder mi posición anterior.
- *Cuando* pulso W, A, S, D o las flechas, *entonces* el cursor amarillo se mueve una celda.
- *Cuando* intento salir del mapa o entrar a un obstáculo, *entonces* no me muevo y veo un mensaje.
- *Cuando* pulso U, *entonces* vuelvo a la posición anterior; si no hay movimientos, veo "No hay movimientos para deshacer".

**HU3 — Registrar un incidente reportado** (RF3, RF23) · Prioridad alta
Como operador quiero registrar un incidente que me reportan por teléfono, para que entre al flujo de atención.
- *Cuando* lleno tipo, gravedad y ubicación y pulso *Registrar*, *entonces* el incidente aparece en la tabla, en la cola de despacho y con un ID nuevo.
- *Cuando* la ubicación es un obstáculo o no es numérica, *entonces* veo el error y no se crea nada.

**HU4 — Consultar un incidente por su ID** (RF3, RF26) · Prioridad media
Como operador quiero buscar un incidente por su ID, para responder rápido cuando me preguntan por él.
- *Cuando* escribo `INC-0003` y pulso *Consultar*, *entonces* la fila queda seleccionada con su detalle.
- *Cuando* el ID no existe, *entonces* veo "No existe un incidente activo con id …".

**HU5 — Saber qué atender primero** (RF5, RF27) · Prioridad alta
Como operador quiero ver cuál es el incidente pendiente más urgente, para no dejar esperando a los más graves.
- *Dado* un robo de gravedad alta y un accidente de gravedad media, *entonces* el más urgente es el robo.
- *Dado* dos de gravedad alta, *entonces* el más urgente es el más antiguo.

**HU6 — Despachar un vehículo** (RF7, RF28, RF32) · Prioridad alta
Como operador quiero que el sistema me sugiera el vehículo adecuado y me deje confirmarlo, para enviar ayuda rápido sin equivocarme.
- *Cuando* selecciono un incidente pendiente, *entonces* veo el vehículo sugerido (compatible, disponible y más cercano) y la lista de alternativas válidas.
- *Cuando* asigno, *entonces* el vehículo pasa a *En ruta*, su ruta aparece en el mapa y veo el tiempo estimado.
- *Cuando* intento enviar un camión de bomberos a un robo, *entonces* la asignación se rechaza y no gano puntos.

**HU7 — Atender el siguiente con un clic** (RF27) · Prioridad media
Como operador quiero un botón que envíe el vehículo sugerido al incidente más urgente, para despachar rápido en momentos de mucha carga.
- *Cuando* pulso *Atender siguiente*, *entonces* el incidente más urgente recibe el vehículo sugerido.
- *Cuando* no hay vehículo compatible, *entonces* veo el motivo y el incidente sigue pendiente.

**HU8 — Cerrar una atención** (RF8, RF14, RF35) · Prioridad alta
Como operador quiero marcar como resuelto un incidente cuando el vehículo ya está en el lugar, para liberar el vehículo y sumar puntos.
- *Dado* que el vehículo todavía va en camino, *cuando* intento resolver, *entonces* veo que aún no llega.
- *Cuando* resuelvo a tiempo con el mejor vehículo, *entonces* gano base + 20 + 10 puntos y el vehículo queda *Disponible*.

**HU9 — Gestionar la flota** (RF6, RF25) · Prioridad media
Como operador quiero registrar vehículos y sacarlos de servicio, para que la simulación refleje la flota real del día.
- *Cuando* registro un vehículo con un ID nuevo sobre una vía, *entonces* aparece en la tabla y en el mapa.
- *Cuando* el ID ya existe o la celda no es una vía, *entonces* el registro se rechaza.
- *Cuando* un vehículo está ocupado, *entonces* no puedo sacarlo de servicio.

**HU10 — Cerrar una vía** (RF31, RF33) · Prioridad media
Como operador quiero bloquear un tramo de vía, para simular un cierre y ver qué incidentes quedan sin acceso.
- *Cuando* bloqueo un tramo, *entonces* se dibuja en rojo, los vehículos en camino recalculan su ruta y la matriz de tiempos se recalcula.
- *Cuando* el cierre aísla un incidente, *entonces* veo una alerta con su ID.

**HU11 — Ver los corredores de emergencia** (RF12, RF34) · Prioridad baja
Como operador quiero ver la red mínima de vías que mantiene conectada la ciudad, para saber cuáles priorizar en mantenimiento.
- *Cuando* activo la capa y elijo Prim o Kruskal, *entonces* el MST se dibuja y veo que ambos tienen el mismo costo.

**HU12 — Guardar y continuar después** (RF16) · Prioridad alta
Como operador quiero guardar la simulación y retomarla más tarde, para no perder el turno.
- *Cuando* guardo y luego cargo el archivo, *entonces* vuelven los incidentes, vehículos, posiciones, vías cerradas, historial y puntaje.
- *Cuando* elijo un archivo que no es una partida o está dañado, *entonces* veo el motivo y la simulación actual sigue intacta.

**HU13 — Usar otra ciudad** (RF15) · Prioridad media
Como administrador quiero cargar otro mapa y otra flota desde JSON, para probar escenarios distintos.
- *Cuando* el archivo es válido, *entonces* la simulación arranca con ese mapa.
- *Cuando* falta un campo, el tipo es incorrecto, hay zonas inalcanzables o vehículos repetidos, *entonces* veo un mensaje que dice qué archivo y qué campo fallan.

**HU14 — Que el tiempo corra solo** (RF4, RF18, RF20) · Prioridad alta
Como operador quiero que la ciudad siga viva mientras trabajo, para practicar decisiones bajo presión.
- *Cuando* pulso *Iniciar*, *entonces* pasa el tiempo, los vehículos se mueven, aparecen robos e incendios y los accidentes surgen por el tráfico.
- *Cuando* pulso *Pausar*, *entonces* el tiempo se detiene, pero puedo seguir asignando y resolviendo.

---

## 4. Diseño de casos de prueba

Las pruebas usan JUnit 5. Todas pasan; el reporte de ejecución está en [reporte-pruebas.md](reporte-pruebas.md).

### 4.1 Escenarios de prueba

| Escenario | Clase | Configuración |
| --- | --- | --- |
| `listaVacia` / `pilaVacia` / `colaVacia` | `LinkedListTest`, `StackTest`, `QueueTest` | Estructura recién creada |
| `tablaHash16` | `HashTableTest` | `HashTable<String, Integer>` con capacidad inicial 16; claves `CollidingKey` con `hashCode` fijo para forzar el mismo bucket |
| `arbol7` | `BinarySearchTreeTest` | BST de enteros con 50, 30, 70, 20, 40, 60, 80 (altura 3) |
| `grafoAMano` | `GraphContractTest` | A–B 4, A–C 1, C–B 2, B–D 5, C–D 8, C–E 10, D–E 2. Resultados calculados a mano: Dijkstra desde A: C=1, B=3, D=8, E=10; MST = 10 |
| `grafoDesconectado` | `GraphContractTest` | A–B, C–D, E aislado |
| `grafoAleatorio30` | `GraphContractTest` | 30 vértices, 80 aristas aleatorias (semilla 2026), pesos 1–20 |
| `ciudad8x6` | `CityMapTest`, `DispatchCenterTest` | Mapa 8 × 6 con vías principales en filas 0, 3, 5 y columnas 0, 7; obstáculos en (2,2) y (5,2); centro en (0,0). Vehículos P-1 (0,0), A-1 (7,5), A-2 (1,3), B-1 (7,0) |
| `ciudad10x7` | Pruebas de simulación | Mapa 10 × 7 con 4 vehículos; semillas fijas para reproducibilidad |
| `palmira` | `ConfigLoaderTest`, `SimulationRepositoryTest` | Configuración incluida: 24 × 16, 359 vértices, 9 vehículos |

### 4.2 Estructuras

**`LinkedListTest`** — escenario `listaVacia`

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `addFirstAndAddLastKeepOrderAndBothEnds` | addLast 2, addFirst 1, addLast 3 | `[1, 2, 3]`, primero 1, último 3 |
| `removeFirstOnEmptyListThrows` | lista vacía | `EmptyStructureException` en `removeFirst` y `getLast` |
| `removeLastUpdatesTailSoLaterAppendsStillWork` | [1,2,3], removeLast, add 4 | Retorna 3; queda `[1, 2, 4]` |
| `removeValueInTheMiddleAndAtTheTail` | [1..4], remove 2, remove 4, remove 99 | `[1, 3]`; la última remoción retorna `false` |
| `insertAndRemoveAtIndexes` | insert(1,2), insert(3,4), removeAt 0 y 2, get(5) | `[1,2,3,4]` → retornos 1 y 4; `IndexOutOfBoundsException` |
| `removeIfFilterFindAndCount` | [1..6] | count pares 3; filter `[2,4,6]`; find >3 → 4; removeIf pares → `[1,3,5]` |
| `sortIsStableAndKeepsTailValid` | [5,3,8,1,9,2], sort, add 10 | `[1,2,3,5,8,9,10]` |

**`StackTest`** — escenario `pilaVacia`

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `newStackIsEmpty` | — | `isEmpty` verdadero, tamaño 0 |
| `popReturnsElementsInReverseOrder` | push W, A, S | pop → S, A, W |
| `peekDoesNotRemove` | push assign, resolve | peek → resolve; tamaño 2 |
| `popAndPeekOnEmptyStackThrow` | pila vacía | `EmptyStructureException` |
| `stackIsReusableAfterBeingEmptied` | push, pop, push two | peek → two |

**`QueueTest`** — escenario `colaVacia`

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `dequeueReturnsElementsInArrivalOrder` | INC-1, INC-2, INC-3 | Salen en ese orden |
| `frontDoesNotRemove` | INC-1, INC-2 | front → INC-1; tamaño 2 |
| `dequeueAndFrontOnEmptyQueueThrow` | cola vacía | `EmptyStructureException` |
| `enqueueAfterEmptyingStillWorks` | A, dequeue, B, C | dequeue → B; front → C |
| `removeTakesAnElementOutOfTheMiddle` | INC-1..3, remove INC-2 | Quedan INC-1 y INC-3 en orden |

**`HashTableTest`** — escenario `tablaHash16`

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `putAndGetReturnStoredValues` | VEH-1→1, VEH-2→2 | get correcto; VEH-404 → null; tamaño 2 |
| `putOnExistingKeyReplacesValueWithoutGrowing` | VEH-1→1, VEH-1→10 | Retorna 1; valor 10; tamaño 1 |
| `removeAndContainsKey` | INC-1, INC-2, remove INC-1 | Retorna 1; ya no está; tamaño 1 |
| `twoKeysInTheSameBucketAreBothRetrievable` | dos `CollidingKey` distintas | **Colisión**: mismo bucket con 2 entradas, ambas recuperables; tras quitar una, la otra sigue |
| `tableGrowsAndKeepsLoadFactorUnderLimit` | 100 claves | La capacidad creció; factor de carga ≤ 0.75; todas recuperables |
| `nullKeysAreRejected` | clave null | `IllegalArgumentException` |

**`PriorityQueueTest`**

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `extractMaxReturnsElementsByPriority` | 5, 1, 9, 3, 7 | Extrae 9, 7, 5 |
| `tieOnPriorityIsBrokenByAge` | dos de nivel 3 (llegadas 200 y 100) y uno de nivel 1 | **Empate**: sale primero el de llegada 100 |
| `peekMaxDoesNotRemoveAndEmptyQueueThrows` | vacía; luego 4 y 8 | Excepción en vacía; peek → 8 sin quitar |
| `reversedComparatorBehavesAsMinHeap` | comparador invertido con 5, 2, 8 | Extrae 2, 5 |
| `removeArbitraryElementKeepsHeapOrder` | 1..10, remove 7 | Extracción estrictamente decreciente sin el 7 |
| `manyRandomInsertionsComeOutSorted` | 500 aleatorios | Salen en orden no creciente |

**`BinarySearchTreeTest`** — escenario `arbol7`

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `inOrderIsAscendingAndReverseIsDescending` | — | `[20..80]` y su inverso |
| `maxAndMinAreTheExtremes` | — | max 80, min 20 |
| `duplicatesAreRejected` | insert 40 | `false`; tamaño 7 |
| `removeLeafNodeWithOneChildAndNodeWithTwoChildren` | insert 65; remove 20, 60, 50 | Los tres casos de eliminación; `[30,40,65,70,80]` |
| `emptyTreeThrowsOnMaxAndMin` | árbol vacío | `EmptyStructureException`; altura 0 |
| `sortedInsertionsDegenerateIntoALinearHeight` | 1..10 en orden | Altura 10 (degenerado) frente a 3 |
| `findAllReturnsMatchesInDescendingOrder` | múltiplos de 20 | `[80, 60, 40, 20]` |

**`UnionFindTest`**

| Prueba | Entradas | Resultado esperado |
| --- | --- | --- |
| `everyElementStartsInItsOwnSet` | n = 5 | 5 componentes; nada conectado |
| `unionMergesSetsAndIsTransitive` | union(0,1), union(1,2) | 0 y 2 conectados; 3 componentes |
| `unionInsideTheSameSetReportsACycle` | union(0,2) tras unir 0-1-2 | `false` (cerraría ciclo) |
| `longChainStillResolvesToOneRoot` | 1 000 uniones en cadena | Todos con la misma raíz |

### 4.3 Grafo y algoritmos

**`GraphContractTest`** — se ejecuta completo sobre `AdjacencyListGraphTest` y `AdjacencyMatrixGraphTest` (30 ejecuciones)

| Prueba | Escenario | Resultado esperado |
| --- | --- | --- |
| `basicOperationsKeepCountsConsistent` | grafoAMano | 5 vértices, 7 aristas, vértice duplicado rechazado, peso A–E infinito |
| `addingAnExistingEdgeReplacesItsWeight` | grafoAMano + A–B 9 | Siguen 7 aristas; peso 9 |
| `removeEdgeAndUpdateWeight` | grafoAMano | Quitar C–E; actualizar D–E; actualizar inexistente lanza excepción |
| `invalidEdgesAndUnknownVerticesAreRejected` | pesos negativos, lazos, vértice Z | `IllegalArgumentException` / `VertexNotFoundException` |
| `bfsComputesHopsOnTheHandGraph` | grafoAMano, desde A | A0, B1, C1, D2, E2 |
| `bfsReportsUnreachableVerticesOnADisconnectedGraph` | grafoDesconectado | **Grafo desconectado**: inalcanzables `[C, D, E]` |
| `dfsFindsEveryConnectedComponent` | grafoDesconectado | 3 componentes; sin ciclos |
| `dfsDetectsTheCycleClosedByANewEdge` | camino A-B-C, luego C-A | Sin ciclo; luego ciclo con A y C |
| `dijkstraMatchesTheHandComputedDistancesAndPath` | grafoAMano | **Resultado a mano**: C1, B3, D8, E10; camino A,C,B,D,E |
| `dijkstraGivesInfiniteDistanceToAnUnreachableVertex` | grafoDesconectado | **Vértice inalcanzable**: distancia infinita, camino vacío |
| `floydWarshallMatchesTheHandGraph` | grafoAMano | A–E = 10, C–D = 7, mismo camino que Dijkstra |
| `floydWarshallAgreesWithDijkstraOnARandomGraph` | grafoAleatorio30 | **Regresión**: misma distancia para todos los pares muestreados |
| `primAndKruskalProduceTheHandComputedTree` | grafoAMano | Costo 10; incluye B–D; excluye A–B |
| `primAndKruskalAgreeOnCostWhenWeightsTie` | cuadrado con diagonal, todos los pesos 2 | **Empates de peso**: ambos costo 6 |
| `kruskalBuildsAForestOnADisconnectedGraph` | grafoDesconectado | 2 aristas; no expande todos los vértices |
| `matrixGrowsBeyondItsInitialCapacityAndKeepsEdges` | 40 vértices en cadena (solo matriz) | 39 aristas; distancia 0→39 = 39 |

### 4.4 Modelo de dominio

**`CityMapTest`** — escenario `ciudad8x6`

| Prueba | Resultado esperado |
| --- | --- |
| `everyTraversableCellBecomesAVertexInTheThreeGraphs` | 46 vértices en las tres copias; mismas aristas en lista y matriz |
| `baseWeightDependsOnTheZonesOfBothCells` | Vía–vía 1.0; vía–residencial 1.5; comercial–comercial 2.5 |
| `invalidGridsAreRejected` | Código 9, fila irregular y centro en obstáculo lanzan `InvalidMapException` |
| `bfsRejectsAMapWithAnEnclosedZone` | El mensaje nombra la celda aislada (1,2) |
| `coverageReportsHopsFromTheMonitoringCenter` | Cada zona con celdas y saltos máximos ≥ promedio |
| `blockingAndUnblockingARoadUpdatesEveryGraph` | El tramo desaparece de las tres copias y vuelve; la versión de topología sube |
| `dfsFindsTwoComponentsWhenABlockedRoadSplitsTheCity` | Mapa lineal partido: 2 componentes |
| `congestionMultipliesTheWeightOfSegmentsInBusyDistricts` | Con 2 vehículos: 1.0 × (1 + 0.25 × 2) = 1.5; la base no cambia |
| `riskPointsAreCrossingsOfTheMainRoadNetwork` | Cada punto de riesgo es vía principal con ≥ 3 tramos de vía principal |
| `serializationRebuildsTheGraphsAndKeepsBlockedRoads` | Tras serializar y leer: mismos vértices y la vía sigue bloqueada |

**`IncidentRepositoryTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `findByIdUsesTheHashIndex` | Encuentra INC-1; INC-404 lanza `IncidentNotFoundException` |
| `mostUrgentPrefersSeverityThenTheOldest` | Orden HIGH-OLD, HIGH-NEW, MED, LOW |
| `sameSeverityAndMinuteAreOrderedByCreationSequence` | Mismo minuto y gravedad: gana la secuencia menor |
| `archiveRemovesFromTreeAndIndexAtTheSameTime` | **Consistencia hash–BST al eliminar**: `isConsistent()` verdadero en cada paso |
| `duplicateIdsAreRejectedAndCountsByType` | ID repetido rechazado; conteos por tipo y búsqueda por gravedad |

**`VehicleAndNavigatorTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `compatibilityRulesMatchTheStatement` | Patrulla: robo y accidente; ambulancia: accidente; bomberos: incendio |
| `repositoryFindsByIdAndFiltersAvailableCompatibleVehicles` | `VehicleNotFoundException` para X-9; ID repetido rechazado; filtros correctos |
| `operatorMovesOnlyOnTraversableCellsInsideTheMap` | Obstáculo y bordes lanzan `InvalidMovementException`; la pila no crece |
| `undoRestoresPositionsInReverseOrderAndFailsWhenEmpty` | Tres deshacer vuelven a (0,0); el cuarto lanza `EmptyStructureException` |
| `vehicleFollowsItsRouteAndIsReleasedAfterAttending` | En ruta → avanza → atendiendo → disponible |

**`DispatchCenterTest`** — escenario `ciudad8x6`

| Prueba | Resultado esperado |
| --- | --- |
| `newIncidentEntersQueueHeapTreeAndIndexAndOperatorMoveIsStacked` | **Integración**: el incidente aparece en la cola de despacho, el heap, el BST y el índice, y el movimiento del operador queda en la pila |
| `invalidAssignmentsAreRejectedAndEarnNoPoints` | Incompatible, vehículo inexistente, incidente inexistente, incidente en proceso y vehículo ocupado lanzan su excepción; puntaje 0 |
| `suggestionIsTheClosestCompatibleAvailableVehicle` | Sugiere A-2; si está ocupado, P-1 |
| `attendNextTakesTheMostUrgentAndBreaksTiesByAge` | Toma el robo medio más antiguo y lo saca de la cola |
| `attendNextWithoutCompatibleVehicleKeepsTheIncidentPending` | Sin camión de bomberos: excepción y el incendio sigue pendiente |
| `vehicleFollowsTheDijkstraRouteArrivesAndResolutionScores` | La ruta termina en el incidente; resolver antes de llegar falla; al llegar: 100 + 20 + 10 = 130 |
| `sendingAFarVehicleLosesTheRoutingBonusAndIsPenalized` | Sin bonificación de ruta y −15 de penalización |
| `lastActionIsTheTopOfTheActionStack` | Vacía → excepción; luego asignar y resolver en el tope |
| `eventsAreConsumedInTheOrderTheyHappened` | Generado, asignado, cambio de estado; la cola queda vacía |
| `indicatorsCountActiveIncidentsByTypeAndAvailableVehicles` | 3 activos (1 de cada tipo), 2 pendientes, 3 vehículos libres |
| `floydWarshallIsRecomputedWhenARoadIsBlocked` | Distancia (0,0)–(1,0): 1.0 → 4.0 tras bloquear → 1.0 al reabrir |
| `blockingTheOnlyRoadIsolatesTheIncidentOnTheOtherSide` | El incidente queda aislado y sin sugerencia |
| `primAndKruskalCorridorsHaveTheSameCostOnTheCity` | Mismo costo; Kruskal expande todos los vértices |

**`ScoreCalculatorTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `highSeverityWithinTimeWithTheBestVehicleEarnsTheStatementExamplePlusRouting` | Ejemplo del enunciado: 100 + 20, más 10 por ruta óptima = 130 |
| `lateResolutionEarnsOnlyTheBasePoints` | Fuera de tiempo: 100 |
| `baseScoreDependsOnSeverity` | Media 70, baja 40 |
| `farVehicleIsPenalizedAndScoreNeverGoesNegative` | Baja con vehículo 5 veces más lejos: 40 − 15 = 25 |

### 4.5 Simulación y concurrencia

**`AccidentDetectorTest`** — escenario `ciudad10x7`

| Prueba | Resultado esperado |
| --- | --- |
| `twoVehiclesEnteringTheSameCellCollide` | Criterio 1: una colisión en (4,3) con 2 vehículos |
| `aDistrictOverItsCapacityProducesACongestionAccident` | Criterio 2: capacidad 2, tres vehículos → accidente con los 2 vecinos |
| `vehiclesFromDifferentDirectionsConvergingOnARiskPointCrash` | Criterio 3: accidente en el punto de riesgo |
| `theSameCriterionWaitsForItsCooldownInTheSamePlace` | Minuto 10 sí, 12 no, 15 sí |
| `aVehicleThatAlreadyCrashedIsNotCountedTwiceInTheSameMinute` | Un solo reporte aunque se cumplan dos criterios |
| `whenDriversYieldAConflictForTheSameCellIsNotACrash` | Con probabilidad 0 no hay colisión |
| `noAccidentIsRaisedWhereAnIncidentIsAlreadyActive` | Celda ocupada: sin reporte |

**`IncidentGeneratorTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `withCertainProbabilitiesOneRobberyAndOneFireAppearEachMinute` | Un robo y un incendio en celdas distintas |
| `robberiesConcentrateInCommercialZonesAndAvoidMainRoads` | En 2 000 sorteos: comercial > 4 × vía principal |
| `firesNeverStartOnTheRoadwayOrObstacles` | Ningún incendio en vía principal ni obstáculo |
| `generationStopsAtTheMaximumNumberOfActiveIncidents` | No supera el tope configurado |

**`SimulationTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `privateCarsOnlyMoveBetweenAdjacentOpenCellsAndNeverCrossABlockedRoad` | 300 minutos: siempre celdas vecinas transitables, nunca por el tramo bloqueado |
| `aCarInvolvedInAnAccidentStaysStillForAFewMinutes` | Detenido 4 minutos en la misma celda |
| `aLongRunGeneratesJustifiedIncidentsWithinTheLimit` | 300 minutos: incidentes generados, accidentes con su criterio en la descripción |
| `anAssignedVehicleReachesItsIncidentAsTheClockTicks` | El camión llega a (4,5) y queda atendiendo |

**`ConcurrencyTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `producersConsumersAndTheClockKeepTheSharedStructuresConsistent` | 4 hilos productores (200 incidentes), 2 consumidores y el reloj a la vez: estructuras consistentes, activos + resueltos = 200, sin excepciones inesperadas |
| `engineRunsPausesResumesAndStopsItsThreads` | El reloj avanza, se congela en pausa, se reanuda; al detener, los tres hilos terminan |

### 4.6 Persistencia y reglas del proyecto

**`JsonParserTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `parsesNestedObjectsAndArraysKeepingKeyOrder` | Objetos y arreglos anidados; orden de claves conservado |
| `parsesEveryNumberFormat` | Enteros, negativos, decimales y exponentes |
| `decodesEscapeSequencesInStrings` | `ó`, comillas y saltos de línea |
| `malformedDocumentsReportLineAndColumn` | El error nombra la línea 3; otros JSON mal formados fallan |
| `duplicateMembersAreRejected` | Campo repetido rechazado |

**`ConfigLoaderTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `theBundledPalmiraConfigurationIsValid` | 24 × 16, 9 vehículos, 14 carros particulares |
| `aValidDocumentProducesMapVehiclesAndSettings` | 15 vértices; vehículo fuera de servicio respetado |
| `missingFileAndMissingFieldAreReported` | **Existencia** y **estructura**: "No existe" y el campo `monitoringCenter` |
| `wrongTypesAndInconsistentGridsAreRejected` | **Consistencia**: tipo incorrecto, fila irregular, centro en obstáculo |
| `vehiclesMustBeUniqueKnownOnRoadsAndCoverEveryIncidentType` | ID repetido, tipo desconocido, sobre obstáculo, fuera del mapa, sin bomberos |
| `blockedRoadsInTheFileAreAppliedAndValidated` | Vía bloqueada aplicada; tramo inválido rechazado |

**`SimulationRepositoryTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `savedStateIsRestoredCompletelyAndTheSimulationContinues` | Minuto, puntaje, incidentes y su orden, vehículos, vías, operador, historial y última acción restaurados; la simulación sigue corriendo y sugiriendo vehículos |
| `missingFileIsReported` | `PersistenceException` "No existe" |
| `aFileThatIsNotASaveIsRejected` | Un JSON se reconoce como "no es una simulación" |
| `aTruncatedSaveIsReportedInsteadOfCrashing` | Archivo cortado a la mitad: `PersistenceException` |

**`ProjectRulesTest`**

| Prueba | Resultado esperado |
| --- | --- |
| `noJavaCollectionReplacesTheOwnStructures` | Ningún archivo de `src/main` usa colecciones de `java.util` (RT1) |
| `everyClassDocumentsTheComplexityOfAtLeastTwoPublicOperations` | Toda clase con dos o más operaciones públicas documenta la complejidad de al menos dos (RT5) |
