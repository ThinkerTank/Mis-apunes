# Diagramas UML

Fuentes en PlantUML (`.puml`), renderizadas en PNG, SVG y PDF. Para regenerarlas: `plantuml -tpng -tsvg -tpdf doc/uml/*.puml`.

| Diagrama | Contenido |
| --- | --- |
| [clases-estructuras](clases-estructuras.png) | Estructuras propias y jerarquía del grafo (`IGraph` → `AbstractGraph` → lista / matriz) |
| [clases-modelo](clases-modelo.png) | Dominio (`DispatchCenter` como fachada) y paquete de simulación concurrente |
| [clases-arquitectura](clases-arquitectura.png) | MVC: controladores, vista, persistencia y jerarquía de excepciones |
| [casos-de-uso](casos-de-uso.png) | Actores (operador, hilo reloj, hilo generador) y casos de uso |
| [secuencia-asignacion](secuencia-asignacion.png) | Asignar un vehículo, con las excepciones posibles |
| [secuencia-minuto](secuencia-minuto.png) | Un minuto de simulación con los tres hilos |
| [secuencia-guardar](secuencia-guardar.png) | Guardar y recuperar una partida |

> El enunciado pide el diagrama de clases en Visual Paradigm (`.vpp`). Ese formato no se puede generar fuera de la herramienta: hay que importar o redibujar estos diagramas en Visual Paradigm y exportar el `.vpp` y su PDF antes de la entrega.
