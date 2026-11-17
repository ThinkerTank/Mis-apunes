# Decisiones de diseño

## 1. Arquitectura y paquetes (MVC)

```
co.edu.icesi.sgmms
├── SgmmsApp                      punto de entrada JavaFX
├── model                         MODELO — no depende de JavaFX
│   ├── structures                estructuras genéricas propias (desacopladas del dominio)
│   ├── exceptions                excepciones personalizadas
│   ├── simulation                tráfico, generador, accidentes, motor concurrente
│   └── (dominio)                 CityMap, Incident, Vehicle, DispatchCenter, RouteService, ScoreCalculator…
├── persistence                   JSON propio, carga de configuración, serialización
├── controller                    CONTROLADORES — uno por escenario + AppContext
└── view                          VISTA — MapRenderer, Palette (y los FXML/CSS en resources)
```

- **Modelo.** `DispatchCenter` es la fachada: los controladores solo hablan con ella (y con `Simulation` para el tráfico). El paquete `structures` no conoce el dominio; se puede reutilizar en otro proyecto tal cual.
- **Vista.** Los tres escenarios están en FXML (`MainView`, `MonitoringCenterView`, `TrafficMapView`, `IncidentPanelView`) con una hoja de estilos común. El mapa se dibuja en un `Canvas` con `MapRenderer`, que solo lee un `MapScene` preparado por el controlador.
- **Controlador.** Cada escenario tiene su controlador; `MainController` maneja el encabezado, la navegación y los archivos; `AppContext` es el estado compartido y el puente con el motor de simulación.
- **Única excepción a "sin colecciones de Java".** `TableView` y `ListView` de JavaFX exigen un `ObservableList`. Se usa solo como adaptador de la vista, recién copiado desde las estructuras propias en cada refresco; ningún dato del modelo vive allí. `ProjectRulesTest` vigila que `java.util` no aparezca en el código fuente.

## 2. Mapa, congestión y rutas

Ver [tad-grafo.md](tad-grafo.md) para la representación dual y la fórmula de pesos.

- **Distritos.** La ciudad se divide en bloques de `districtSize × districtSize` celdas (6 × 6 en Palmira → 12 distritos). Cada minuto se cuentan los vehículos en movimiento (emergencia y particulares) por distrito; ese conteo alimenta el factor de congestión de las vías y el criterio de accidente 2.
- **Movimiento de vehículos.** Cada minuto un vehículo gana una unidad de "capacidad de viaje" y entrar a la siguiente celda le cuesta el peso actual de ese tramo. Una vía congestionada tarda más en recorrerse, de modo que **el costo de la ruta Dijkstra es también el tiempo de viaje en minutos** y el "tiempo estimado de atención" es `⌈costo⌉`.
- **Vías bloqueadas en plena ruta.** Si un vehículo encuentra su siguiente tramo cerrado, recalcula su ruta con Dijkstra desde donde está; si no hay camino, espera y se reporta como incidente aislado.

## 3. Criterios de accidente (RF4)

Los accidentes nunca aparecen al azar: cada uno es consecuencia del movimiento de los vehículos. Se evalúan cada minuto sobre todos los movimientos (14 carros particulares más los vehículos de emergencia en ruta).

| # | Criterio | Cuándo ocurre | Gravedad | Justificación |
| --- | --- | --- | --- | --- |
| 1 | **Colisión** | Dos vehículos se mueven a la misma celda en el mismo minuto, y ninguno cede el paso (probabilidad configurable `collisionProbability`, 20 % en Palmira) | Media | Es la causa más directa de un choque. En la realidad casi siempre alguien cede; sin la probabilidad, las colisiones salían a 12.8 por hora simulada, un número irreal |
| 2 | **Congestión crítica** | Un distrito supera `zoneCapacity` vehículos (4 en Palmira). El choque ocurre en la celda más densa y solo involucra a los vehículos de esa celda o las vecinas | Baja | El tráfico saturado produce frenadas y cambios de carril: choques leves pero repetidos. Involucrar solo a los vecinos evita un bucle: si se detuviera a todo el distrito, la congestión nunca bajaría y los accidentes se encadenarían |
| 3 | **Cruce en punto crítico** | Dos o más vehículos que vienen de direcciones distintas convergen (llegan a la celda o se acercan a ella) en un punto de riesgo en el mismo minuto | Alta | Los puntos de riesgo son los cruces de la red de vías principales donde DFS detecta que se cierra un ciclo, es decir, donde se encuentran rutas alternativas. Tráfico cruzado a velocidad de vía principal |

Reglas comunes: un criterio no vuelve a disparar en el mismo lugar (o distrito) antes de `accidentCooldownMinutes` (8 minutos; el doble para congestión); nunca hay un accidente nuevo sobre una celda con un incidente activo; un vehículo participa en a lo sumo un choque por minuto; los vehículos involucrados quedan detenidos 4 minutos.

**Calibración medida** (5 corridas de 240 minutos, sin tope de incidentes): colisiones 2.8 por hora simulada, congestión 0.8, cruce en punto crítico 1.8; robos e incendios 4.9. Los accidentes son algo más de la mitad de los incidentes, y en total aparece uno cada ~6 minutos simulados.

## 4. Robos e incendios (RF4)

Probabilidad por minuto: robo 5 %, incendio 2.5 % (configurables en `city.json`). La ubicación se sortea primero por zona, con estos pesos, y luego una celda libre de esa zona:

| Zona | Robo | Incendio | Razón |
| --- | --- | --- | --- |
| Comercial | 5 | 3 | Comercio y dinero concentran los robos; hay locales que pueden incendiarse |
| Residencial | 3 | 4 | Viviendas: robos frecuentes, y es donde más incendios estructurales hay |
| Camino | 1.5 | 1 | Calles internas con poco movimiento |
| Vía principal | 0.5 | 0 | Muy transitadas y vigiladas; no hay edificaciones en la calzada |

La gravedad se sortea: robos 50 % baja, 35 % media, 15 % alta; incendios 30 % baja, 40 % media, 30 % alta. Mientras haya 12 incidentes activos no se generan nuevos, para que la partida siga siendo jugable.

## 5. Puntuación y tiempos máximos (RF14, RF35)

```
total = base(gravedad) + bonificación de tiempo + bonificación de ruta − penalización de ruta   (mínimo 0)

base                    = 100 alta | 70 media | 40 baja
bonificación de tiempo  = +20 si (minuto de resolución − minuto de generación) ≤ tiempo máximo
bonificación de ruta    = +10 si el vehículo enviado tenía el menor costo Dijkstra entre los compatibles disponibles
penalización de ruta    = −15 si el vehículo enviado estaba a más de 1.5 veces la distancia óptima según Floyd-Warshall
```

**Tiempos máximos de atención:** alta 15 min, media 25 min, baja 40 min. Atravesar Palmira por vías principales toma unos 25 minutos y el tiempo promedio de respuesta desde el centro es de ~12 minutos (ver Centro de Monitoreo). Quince minutos para un incidente grave exigen enviar un vehículo cercano y actuar ya; veinticinco y cuarenta dejan margen para que los menos graves esperen detrás de los urgentes.

**Por qué dos referencias para el ruteo.** La bonificación compara costos Dijkstra *con el tráfico del momento*: premia leer bien la congestión. La penalización compara distancias Floyd-Warshall *estructurales*: castiga una mala decisión de fondo (mandar una patrulla del otro extremo de la ciudad), no la mala suerte con el tráfico. El vehículo sugerido es el más cercano según Floyd-Warshall; con mucha congestión puede no ser el de menor costo Dijkstra, y el operador que lo note gana la bonificación.

Una asignación rechazada (vehículo incompatible, ocupado o sin ruta) lanza una excepción antes de crear el registro de asignación, así que nunca genera puntos.

## 6. JSON frente a serialización (RF15, RF16)

| | JSON (`city.json`, `vehicles.json`) | Serialización (`.sgmms`) |
| --- | --- | --- |
| Qué guarda | La **configuración inicial**: cuadrícula, centro de monitoreo, parámetros de rutas y congestión, vías cerradas de entrada, flota y parámetros de simulación | El **estado en ejecución**: incidentes activos y resueltos en su BST e índice hash, cola de despacho, heap de atención, pilas de acciones y movimientos, vehículos con estado y ruta, tráfico particular, vías bloqueadas, congestión, puntaje, reloj, generador aleatorio y enfriamientos de accidentes |
| Quién lo escribe | Una persona, a mano | El programa |
| Por qué ese formato | Texto legible y editable, fácil de validar campo por campo con mensajes claros | Es un grafo de objetos interconectados: el mismo `Incident` vive en cuatro estructuras a la vez. La serialización conserva esas referencias sin escribir un formato propio |
| Errores | `ConfigurationException`: archivo inexistente, JSON inválido (con línea y columna), campo faltante o de tipo incorrecto, contenido inconsistente | `PersistenceException`: archivo inexistente, no es una partida (encabezado `SGMMS-SAVE-1`), archivo cortado o dañado, versión incompatible |

Lo que **no** se serializa: los tres grafos y la matriz de Floyd-Warshall (`transient`), que se reconstruyen desde la cuadrícula y las vías bloqueadas al cargar. El guardado bloquea la simulación y el centro de despacho (en el mismo orden que los hilos), escribe en un archivo temporal y luego lo mueve, así que nunca queda una partida a medio escribir.

## 7. Excepciones (RF19)

Las excepciones de dominio heredan de `SgmmsException` (no verificada): describen operaciones inválidas que el operador pidió y se capturan en un solo lugar, `AppContext.perform`, que muestra el mensaje en la barra de estado sin cerrar la aplicación. Las de archivos (`ConfigurationException`, `PersistenceException`) son verificadas: vienen del sistema de archivos y el compilador obliga a manejarlas; se muestran en un diálogo.

| Excepción | Situación | Dónde se lanza | Prueba |
| --- | --- | --- | --- |
| `EmptyStructureException` | Pila, cola, heap, lista o árbol vacío; deshacer sin movimientos; atender sin pendientes | Estructuras, `OperatorNavigator.undo`, `attendNext` | `StackTest`, `QueueTest`, `VehicleAndNavigatorTest` |
| `IncidentNotFoundException` | ID de incidente que no está activo | `IncidentRepository.findById` | `IncidentRepositoryTest` |
| `VehicleNotFoundException` | ID de vehículo inexistente | `VehicleRepository.findById` | `VehicleAndNavigatorTest` |
| `IncompatibleVehicleException` | Tipo de vehículo no apto para el tipo de incidente | `DispatchCenter.assign` | `DispatchCenterTest` |
| `VehicleNotAvailableException` | Vehículo en ruta, atendiendo o fuera de servicio; ninguno disponible para *Atender siguiente* | `assign`, `attendNext`, `setOutOfService` | `DispatchCenterTest` |
| `InvalidIncidentStateException` | Asignar a un incidente resuelto o en proceso; resolver sin vehículo o antes de que llegue | `Incident`, `DispatchCenter` | `DispatchCenterTest` |
| `InvalidMovementException` | El operador intenta salir del mapa o pisar un obstáculo | `OperatorNavigator.move` | `VehicleAndNavigatorTest` |
| `InvalidMapException` | Cuadrícula irregular, código desconocido, centro en obstáculo, zonas inalcanzables, tramo inválido o ya bloqueado | `CityMap`, `ZoneType` | `CityMapTest` |
| `RouteUnavailableException` | No hay vía abierta entre el vehículo y el incidente | `DispatchCenter.assign` | `DispatchCenterTest` (incidente aislado) |
| `DuplicateVehicleException` | Registrar un vehículo con un ID existente o vacío | `registerVehicle` | — (validado en la interfaz) |
| `VertexNotFoundException` | Operación de grafo sobre un vértice inexistente | `AbstractGraph.index` | `GraphContractTest` |
| `ConfigurationException` | Problemas de existencia, estructura o consistencia del JSON | `ConfigLoader`, `JsonParser` | `ConfigLoaderTest`, `JsonParserTest` |
| `PersistenceException` | Guardar o recuperar una partida falla | `SimulationRepository` | `SimulationRepositoryTest` |

## 8. Concurrencia (RF20)

```
 hilo sgmms-clock ─────┐  cada minuto: Simulation.clockTick()  → avanza el reloj, mueve vehículos,
                       │                                          detecta accidentes, actualiza congestión
 hilo sgmms-generator ─┼─► Simulation ──► DispatchCenter  (recurso compartido, métodos synchronized)
                       │  cada minuto (desfasado ½): generationTick() → robos e incendios
 hilo JavaFX ──────────┤  acciones del operador: asignar, resolver, mover, bloquear
                       │
 hilo sgmms-indicators ┘  cada 250 ms: indicators() + pollEvents() → Platform.runLater(refrescar)
```

- **Recurso compartido.** `DispatchCenter`: todos sus métodos públicos son `synchronized`. El caso típico del enunciado se cumple: el hilo generador inserta un incidente en el BST y el índice hash mientras el hilo de la interfaz resuelve otro y lo quita de ambos; nunca se intercalan.
- **Orden de bloqueo.** Los hilos toman primero el candado de `Simulation` y luego el de `DispatchCenter`; el centro nunca llama de vuelta a la simulación. Siempre el mismo orden → no hay interbloqueo.
- **Productor/consumidor.** La cola de eventos la producen el reloj, el generador y el operador; solo el hilo de indicadores la consume y entrega los eventos a la interfaz con `Platform.runLater`, el único hilo autorizado a tocar JavaFX.
- **Lecturas seguras.** Los métodos de consulta devuelven copias o instantáneas inmutables (`Indicators`), así que la interfaz nunca recorre una estructura que otro hilo está modificando.
- **Pausa.** Las banderas `running` y `paused` son `volatile`; un hilo en pausa espera en un monitor (`wait`/`notifyAll`) en lugar de girar.
- **Verificación.** `ConcurrencyTest` lanza 4 productores, 2 consumidores y el reloj sobre el mismo centro y comprueba la consistencia de todas las estructuras al final; corrió estable en repeticiones sucesivas.
