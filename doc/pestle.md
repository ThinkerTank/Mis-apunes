# Análisis PESTLE y consideraciones éticas

El SGMMS es un prototipo académico, pero modela una decisión pública real: a quién se atiende primero cuando los recursos de emergencia no alcanzan. Este análisis revisa el impacto que tendría un sistema así en Palmira y qué decisiones del prototipo responden a cada riesgo.

## Político

- **Rendición de cuentas.** Si la Alcaldía usa un sistema para despachar, alguien debe poder explicar por qué un barrio esperó más que otro. El prototipo hace visible el criterio: prioridad por gravedad y luego por antigüedad, sugerencia por distancia, y cada decisión queda en la pila de acciones y en el historial de eventos.
- **Presión sobre las métricas.** Un puntaje por incidente resuelto puede empujar a los operadores a "cerrar rápido" en lugar de "atender bien". En un despliegue real, el puntaje debería usarse para entrenamiento, no para evaluar el desempeño de personas.
- **Coordinación entre entidades.** Policía, salud y bomberos dependen de autoridades distintas. Las reglas de compatibilidad (patrulla, ambulancia, bomberos) tendrían que acordarse entre ellas, no decidirse en el software.

## Económico

- **Costo frente a beneficio.** Una herramienta centralizada reduce tiempos de respuesta sin comprar más vehículos; los corredores de emergencia (MST) indican qué vías conviene priorizar en mantenimiento con un presupuesto limitado.
- **Sin dependencias externas.** El prototipo no usa servicios en la nube, APIs pagas ni mapas comerciales: funciona sin conexión y no genera costos recurrentes.
- **Costo de los errores.** Un despacho equivocado tiene costo económico (vehículos ocupados en el lugar incorrecto) y humano. Por eso la asignación se valida y las asignaciones inválidas se rechazan antes de ocurrir.

## Social

- **Equidad territorial.** Si los vehículos se ubican siempre cerca del centro, los barrios periféricos reciben peor servicio. El Centro de Monitoreo muestra la cobertura (saltos BFS) y el tiempo estimado de respuesta por zona precisamente para que esa desigualdad sea visible.
- **Sesgo en la generación de datos.** El generador asigna más robos a zonas comerciales. En un sistema real, si esas probabilidades salieran de datos históricos de denuncias, podrían reflejar dónde se denuncia más, no dónde ocurre más, y terminar concentrando la vigilancia en ciertos barrios. Los pesos del prototipo están documentados y son configurables justamente para poder revisarlos.
- **Confianza ciudadana.** La gente confía en un sistema cuando entiende cómo decide. Las reglas son simples y están escritas en la documentación y en la interfaz.

## Tecnológico

- **Simplificaciones.** No hay GPS ni mapas reales: el mapa es una cuadrícula y el tráfico es simulado. Es suficiente para entrenar y comparar estrategias, no para operar una ciudad.
- **Robustez.** Errores de archivos y de operación no cierran la aplicación; el guardado es atómico; los hilos están sincronizados y probados bajo carga.
- **Algoritmos con trade-offs explícitos.** Floyd-Warshall precalculado frente a Dijkstra bajo demanda, BST frente a heap: cada elección está documentada con su costo, lo que permite cambiarla si la ciudad crece.

## Legal

- **Datos personales.** El prototipo no guarda datos de personas: un incidente tiene tipo, ubicación, gravedad y una descripción breve. Un sistema real que registrara nombres, teléfonos o placas debería cumplir la Ley 1581 de 2012 de protección de datos personales en Colombia (finalidad, consentimiento, seguridad y acceso).
- **Responsabilidad.** El sistema **sugiere** un vehículo, pero el operador **confirma**. Mantener la decisión final en una persona deja clara la responsabilidad y evita delegar en el software una decisión que puede afectar la vida de alguien.
- **Trazabilidad.** El historial de acciones y la partida guardada permiten reconstruir qué se decidió y cuándo, algo exigible en investigaciones posteriores a una emergencia.

## Ambiental

- **Menos recorridos.** Enviar el vehículo más cercano por la ruta de menor costo reduce kilómetros recorridos, combustible y emisiones; la puntuación penaliza los despachos lejanos.
- **Congestión.** El modelo encarece las vías congestionadas y las rutas las evitan cuando es posible, lo que en la realidad también reduce el tiempo de los vehículos con el motor encendido.
- **Incendios.** Atender rápido los incendios estructurales reduce daños y contaminación del aire en zonas residenciales.

## Consideraciones éticas y decisiones del prototipo

| Riesgo ético | Decisión en el SGMMS |
| --- | --- |
| Decisiones opacas sobre a quién se atiende | Criterio único y documentado (`IncidentPriorityComparator`), visible en la interfaz |
| El software decide por la persona | Sugerencia + confirmación del operador; *Atender siguiente* es un atajo explícito, no automático |
| Desigualdad entre zonas | Indicadores de cobertura y tiempo de respuesta por zona en el Centro de Monitoreo |
| Sesgo en las probabilidades de incidentes | Pesos explícitos, justificados y configurables en `city.json` |
| Incentivos perversos del puntaje | El puntaje premia atender a tiempo y con el vehículo adecuado, no cerrar sin atender; documentado como herramienta de entrenamiento |
| Pérdida de información en una falla | Guardado atómico, mensajes claros y recuperación consistente del estado |
