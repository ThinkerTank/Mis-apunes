# SGMMS — Sistema de Gestión y Monitoreo de Movilidad y Seguridad

Prototipo de escritorio en Java para el centro de monitoreo urbano de Palmira. El operador observa la ciudad en un mapa 2D, recibe incidentes (accidentes, robos e incendios), los prioriza por gravedad y despacha vehículos de atención por la ruta de menor costo.

Tarea integradora 2026-2 de los cursos Algoritmos y Programación II, Computación y Estructuras Discretas I e Ingeniería de Software II — Universidad Icesi.

## Equipo

| Nombre | Código |
| --- | --- |
| _(pendiente)_ | _(pendiente)_ |
| _(pendiente)_ | _(pendiente)_ |
| _(pendiente)_ | _(pendiente)_ |

## Requisitos para ejecutar

- JDK 17 o superior
- Maven 3.8 o superior (descarga JavaFX 17 y JUnit 5 automáticamente)

## Cómo ejecutar

```bash
mvn javafx:run     # abre la aplicación con la configuración de Palmira
mvn test           # corre las 142 pruebas unitarias
```

**IntelliJ IDEA:** abrir la carpeta del proyecto como proyecto Maven y ejecutar `mvn javafx:run` desde la ventana Maven (Plugins › javafx › javafx:run). No hace falta configurar JavaFX a mano.

## Uso

La ventana tiene un encabezado con el reloj de simulación, el puntaje del operador, el botón Iniciar / Pausar, la velocidad del reloj y los botones para cambiar de escenario. La simulación arranca en pausa: pulse **Iniciar** para que corra el tiempo (en pausa se puede seguir asignando y resolviendo).

| Escenario | Qué se hace allí |
| --- | --- |
| **Centro de Monitoreo** | Indicadores en tiempo real (incidentes activos por tipo, vehículos disponibles, puntaje), incidente más grave (BST), siguiente en la cola de prioridad y en la cola de despacho, estado de la red vial (componentes, incidentes aislados, puntos de riesgo, tiempo de Floyd-Warshall), cobertura y tiempo de respuesta por zona, actividad reciente |
| **Mapa de Tráfico** | Mapa 2D de Palmira con capas activables: rutas de los vehículos (Dijkstra), carros particulares, puntos de riesgo (DFS) y corredores de emergencia (MST, Prim o Kruskal). Modo para bloquear y reabrir tramos de vía. Clic en una celda para ver su contenido |
| **Panel de Incidentes** | Tabla de incidentes activos por prioridad, registrar un incidente manual, consultar por ID, vehículo sugerido, asignar, *Atender siguiente*, resolver, historial de acciones, gestión de la flota e incidentes resueltos |

**Controles del operador (Mapa de Tráfico):** `W` `A` `S` `D` o flechas para moverse, `U` para deshacer el último movimiento.

**Menú Archivo:** nueva simulación con la configuración de Palmira, cargar configuración JSON (`city.json` + `vehicles.json`), guardar y cargar una partida (`.sgmms`).

Las configuraciones de ejemplo están en `src/main/resources/co/edu/icesi/sgmms/config/`.

## Arquitectura

```
co.edu.icesi.sgmms
├── model/structures    LinkedList, Stack, Queue, HashTable, PriorityQueue (heap), BinarySearchTree,
│                       UnionFind, IGraph → AbstractGraph → AdjacencyListGraph | AdjacencyMatrixGraph
├── model               CityMap, DispatchCenter (fachada sincronizada), RouteService, ScoreCalculator,
│                       repositorios de incidentes y vehículos, navegador del operador, enumeraciones
├── model/exceptions    SgmmsException y 11 excepciones de dominio
├── model/simulation    tráfico particular, generador de incidentes, detector de accidentes, motor de 3 hilos
├── persistence         parser JSON propio, carga de configuración, serialización de partidas
├── controller          un controlador por escenario + AppContext
└── view                MapRenderer (Canvas), FXML y CSS en resources
```

Todas las colecciones del modelo son estructuras propias; no se usa ninguna colección de `java.util` (una prueba lo verifica). Detalles en [doc/diseno.md](doc/diseno.md).

## Cumplimiento del enunciado

| Requisito | Dónde |
| --- | --- |
| RF1–RF20 (generales) | Ver la tabla de requerimientos en [doc/especificacion.md](doc/especificacion.md) |
| Pilas: movimientos (RF21) y acciones (RF22) | `OperatorNavigator`, `DispatchCenter.actions` |
| Colas: despacho (RF23) y eventos (RF24) | `DispatchCenter.dispatchQueue`, `DispatchCenter.events` |
| Tablas hash: vehículos (RF25) e incidentes (RF26) | `VehicleRepository`, `IncidentRepository` |
| Heap de atención (RF27) y vehículo candidato (RF28) | `DispatchCenter.attentionQueue`, `suggestVehicle` |
| Mapa como grafo, dos representaciones (RF29) | `CityMap`, `AdjacencyListGraph`, `AdjacencyMatrixGraph` |
| BFS de conectividad (RF30), DFS de componentes y ciclos (RF31) | `CityMap`, `DispatchCenter.connectedComponents`, `riskPoints` |
| Dijkstra (RF32), Floyd-Warshall (RF33), Prim y Kruskal (RF34) | `AbstractGraph`, `RouteService` |
| Estrategia de puntaje basada en grafos (RF35) | `ScoreCalculator` |
| JSON y serialización | `ConfigLoader`, `SimulationRepository` |
| Concurrencia | `SimulationEngine` (hilos reloj, generador e indicadores) |
| Pruebas unitarias | `src/test/java` — [reporte](doc/reporte-pruebas.md) |

## Documentación

| Documento | Contenido |
| --- | --- |
| [especificacion.md](doc/especificacion.md) | Requerimientos funcionales y no funcionales, historias de usuario, diseño de casos de prueba |
| [diseno.md](doc/diseno.md) | Arquitectura MVC, criterios de accidente, puntuación, JSON vs serialización, excepciones, concurrencia |
| [tad-estructuras.md](doc/tad-estructuras.md) | TAD de las estructuras propias |
| [tad-grafo.md](doc/tad-grafo.md) | TAD del grafo y modelado del mapa |
| [analisis-complejidad.md](doc/analisis-complejidad.md) | Complejidad temporal y espacial, comparaciones medidas (BST vs heap, Dijkstra vs Floyd-Warshall, Prim vs Kruskal) |
| [metodo-ingenieria.md](doc/metodo-ingenieria.md) | Método de la ingeniería: problema, alternativas, criterios y selección |
| [pestle.md](doc/pestle.md) | Análisis PESTLE y consideraciones éticas |
| [reporte-pruebas.md](doc/reporte-pruebas.md) | Resultados de la suite de pruebas |
| [uml/](doc/uml/README.md) | Diagramas de clases, casos de uso y secuencia (PlantUML, PNG, SVG, PDF) |

## Flujo de trabajo (gitflow)

`main` guarda las versiones entregadas; `develop` integra el trabajo; cada funcionalidad se desarrolla en una rama `feat/*` o `docs/*` y se integra a `develop` con un merge sin fast-forward. Tras cada integración se corren las pruebas y se registra una fila en la tabla de indicadores.

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
| 13 | `74449a96034078fc32c79ffafd628745f6b5edc6` | Documentación: TAD, complejidad, método de la ingeniería | 142 | 0 | 0.0000 | 1.0000 | 4.06 |
| 14 | `4fe74a55724e7e8c37dd69f18c3f8dba72d39e0e` | Documentación: especificación, historias, casos de prueba, diseño, PESTLE, UML | 142 | 0 | 0.0000 | 1.0000 | 4.06 |
| 15 | `9511df2b91ef26e8b74fd053392ef42758ced797` | README final, reporte de pruebas y verificación de la entrega | 142 | 0 | 0.0000 | 1.0000 | 4.06 |
