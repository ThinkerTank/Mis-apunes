# Tipos Abstractos de Datos — estructuras lineales, tabla hash, cola de prioridad y árbol

Especificación de cada estructura **antes** de su implementación (RT4). Todas son genéricas, viven en el paquete `co.edu.icesi.sgmms.model.structures` y no dependen del dominio del SGMMS. Ninguna usa colecciones de `java.util`; la prueba `ProjectRulesTest` lo verifica sobre el código fuente.

Convenciones: `⟨e₁, …, eₙ⟩` es una secuencia; `|s|` su longitud; `s′` el estado después de la operación. Las operaciones son **constructoras** (crean), **modificadoras** (cambian el estado) o **analizadoras** (consultan sin modificar).

---

## TAD LinkedList⟨T⟩

Base de todas las estructuras lineales del proyecto: pilas, colas, listas de adyacencia y buckets de la tabla hash se construyen sobre ella (RT2).

**Valores.** Secuencias finitas `L = ⟨e₁, e₂, …, eₙ⟩` con `n ≥ 0` y `eᵢ ∈ T`.

**Invariante.** `size = n`. `head = null ⇔ tail = null ⇔ n = 0`. Desde `head` se alcanzan exactamente `n` nodos y el último es `tail`.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `LinkedList()` | Constructora | — | `L = ⟨⟩` |
| `addFirst(e)` | Modificadora | — | `L′ = ⟨e, e₁, …, eₙ⟩` |
| `addLast(e)` | Modificadora | — | `L′ = ⟨e₁, …, eₙ, e⟩` |
| `insert(i, e)` | Modificadora | `0 ≤ i ≤ n` | `e` queda en la posición `i`; los siguientes se corren una posición |
| `removeFirst()` | Modificadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `e₁`; `L′ = ⟨e₂, …, eₙ⟩` |
| `removeLast()` | Modificadora | `n > 0` | Retorna `eₙ`; `L′ = ⟨e₁, …, eₙ₋₁⟩` |
| `removeAt(i)` | Modificadora | `0 ≤ i < n` | Retorna `eᵢ₊₁` y lo quita |
| `remove(x)` | Modificadora | — | Quita la primera aparición de `x`; retorna si existía |
| `removeIf(p)` | Modificadora | — | Quita todo `eᵢ` con `p(eᵢ)`; retorna si quitó alguno |
| `sort(c)` | Modificadora | `c` es un orden total | `L′` es una permutación ordenada y estable de `L` |
| `getFirst()`, `getLast()` | Analizadora | `n > 0` | Retorna `e₁` o `eₙ` |
| `get(i)` | Analizadora | `0 ≤ i < n` | Retorna `eᵢ₊₁` |
| `contains(x)`, `indexOf(x)` | Analizadora | — | Pertenencia / primera posición o −1 |
| `find(p)`, `filter(p)`, `count(p)` | Analizadora | — | Primer elemento que cumple `p` / nueva lista con los que cumplen / cuántos cumplen |
| `size()`, `isEmpty()` | Analizadora | — | `n` / `n = 0` |

---

## TAD Stack⟨T⟩ (pila, LIFO)

Guarda el historial de movimientos del operador (deshacer con U, RF21) y las acciones administrativas (RF22).

**Valores.** Secuencias `S = ⟨e₁, …, eₙ⟩` donde `eₙ` es el **tope**.

**Invariante.** `size = n`. El tope es la cabeza de la lista enlazada interna.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `Stack()` | Constructora | — | `S = ⟨⟩` |
| `push(e)` | Modificadora | — | `S′ = ⟨e₁, …, eₙ, e⟩` |
| `pop()` | Modificadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `eₙ`; `S′ = ⟨e₁, …, eₙ₋₁⟩` |
| `peek()` | Analizadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `eₙ`; `S′ = S` |
| `isEmpty()` | Analizadora | — | `n = 0` |
| `size()` | Analizadora | — | `n` |

---

## TAD Queue⟨T⟩ (cola, FIFO)

Modela la cola de despacho de incidentes sin vehículo (RF23) y la cola de eventos que consume la interfaz (RF24).

**Valores.** Secuencias `Q = ⟨e₁, …, eₙ⟩` donde `e₁` es el **frente** y `eₙ` el **final**.

**Invariante.** `size = n`. El frente es la cabeza de la lista interna y el final es su cola (`tail`), de modo que ambas operaciones principales son O(1).

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `Queue()` | Constructora | — | `Q = ⟨⟩` |
| `enqueue(e)` | Modificadora | — | `Q′ = ⟨e₁, …, eₙ, e⟩` |
| `dequeue()` | Modificadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `e₁`; `Q′ = ⟨e₂, …, eₙ⟩` |
| `front()` | Analizadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `e₁`; `Q′ = Q` |
| `remove(x)` | Modificadora | — | Quita `x` de cualquier posición. No pertenece al TAD clásico: la cola de despacho la necesita porque un incidente puede recibir vehículo antes de llegar al frente |
| `isEmpty()`, `size()` | Analizadora | — | `n = 0` / `n` |

---

## TAD HashTable⟨K, V⟩

Registro de vehículos por ID (RF25) e índice de incidentes por ID (RF26). También representa los objetos del parser JSON.

**Valores.** Funciones parciales finitas `M : K → V`, es decir, conjuntos de pares `{(k₁, v₁), …, (kₙ, vₙ)}` con claves distintas y no nulas.

**Representación.** Un arreglo de `m` buckets (`m` potencia de dos); cada bucket es una `LinkedList` de `HashEntry⟨K, V⟩` (encadenamiento separado). La clave `k` vive en el bucket `h(k) = (c ⊕ (c ≫ 16)) ∧ (m − 1)` con `c = k.hashCode()`.

**Invariante.** Cada par está en el bucket `h(k)`; no hay dos pares con la misma clave; `size = n`; `n / m ≤ 0.75` (factor de carga máximo).

**Función hash, justificación.** `m` potencia de dos convierte el módulo en un `AND` con `m − 1`, una sola instrucción. El problema de esa máscara es que solo mira los bits bajos de `hashCode`; el `XOR` con los 16 bits altos hace que también influyan, de modo que IDs que solo difieren al final o al principio (`VEH-10`, `VEH-20`, `INC-0001` … `INC-0099`) se reparten entre buckets distintos. Con factor de carga ≤ 0.75 la longitud esperada de cada cadena es constante, y de ahí sale el O(1) promedio.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `HashTable(m₀)` | Constructora | `m₀ ≥ 1` | `M = ∅`, `m` = menor potencia de dos ≥ `m₀` |
| `put(k, v)` | Modificadora | `k ≠ null` | `M′ = M ∖ {(k, ·)} ∪ {(k, v)}`; retorna el valor anterior o `null`. Si `(n + 1)/m > 0.75`, `m′ = 2m` y todos los pares se redistribuyen |
| `get(k)` | Analizadora | `k ≠ null` | Retorna `M(k)` o `null` si `k ∉ dom(M)` |
| `remove(k)` | Modificadora | `k ≠ null` | `M′ = M ∖ {(k, ·)}`; retorna el valor quitado o `null` |
| `containsKey(k)` | Analizadora | `k ≠ null` | `k ∈ dom(M)` |
| `size()`, `isEmpty()` | Analizadora | — | `n` / `n = 0` |
| `keys()`, `values()` | Analizadora | — | Listas con `dom(M)` / `img(M)` |
| `loadFactor()`, `capacity()` | Analizadora | — | `n / m` / `m` |

---

## TAD PriorityQueue⟨T⟩ (heap binario)

Cola de atención de incidentes pendientes (RF27), selección de vehículo candidato (RF28) y min-heap interno de Dijkstra y Prim (RF32, RF34).

**Valores.** Multiconjuntos finitos `P = {e₁, …, eₙ}` con un orden de prioridad dado por un comparador `c`, donde `c(a, b) > 0` significa que `a` va primero. Con el comparador invertido se obtiene un min-heap.

**Representación.** Arreglo propio `heap[0..n−1]` que crece al doble cuando se llena. Los hijos de `i` están en `2i + 1` y `2i + 2`; su padre en `⌊(i − 1)/2⌋`.

**Invariante (propiedad de heap).** Para todo `i > 0`: `c(heap[i], heap[⌊(i−1)/2⌋]) ≤ 0`. Por lo tanto `heap[0]` es el de mayor prioridad. Además el árbol es completo (sin huecos en `0..n−1`).

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `PriorityQueue(c)` | Constructora | `c ≠ null` | `P = ∅` |
| `insert(e)` | Modificadora | — | `P′ = P ∪ {e}`; se conserva la propiedad de heap (sift-up) |
| `extractMax()` | Modificadora | `n > 0` (si no: `EmptyStructureException`) | Retorna `x = max_c(P)`; `P′ = P ∖ {x}` (la última hoja pasa a la raíz y baja con sift-down) |
| `peekMax()` | Analizadora | `n > 0` | Retorna `max_c(P)` sin modificar |
| `remove(e)` | Modificadora | — | Quita `e` si está y restaura el heap |
| `isEmpty()`, `size()` | Analizadora | — | `n = 0` / `n` |

**Comparador de incidentes.** `IncidentPriorityComparator` (gravedad Alta > Media > Baja; empate → el más antiguo; empate → el creado primero) es el mismo objeto que ordena el BST, así que los dos cursos usan exactamente el mismo criterio.

---

## TAD BinarySearchTree⟨T⟩

Almacena los incidentes activos ordenados por gravedad (requisito de APO II) y responde "¿cuál es el más grave?" y "listar por prioridad".

**Valores.** Conjuntos finitos `B ⊆ T` con un orden total dado por el comparador.

**Representación.** Árbol binario de nodos `TreeNode⟨T⟩` con hijos izquierdo y derecho.

**Invariante.** Para todo nodo `x`: todo elemento del subárbol izquierdo es menor que `x` y todo elemento del derecho es mayor. No hay dos elementos que comparen igual; por eso el comparador de incidentes termina desempatando por secuencia de creación.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `BinarySearchTree(c)` | Constructora | `c ≠ null` | `B = ∅` |
| `insert(x)` | Modificadora | — | `B′ = B ∪ {x}`; retorna `false` si ya existía |
| `remove(x)` | Modificadora | — | `B′ = B ∖ {x}`; un nodo con dos hijos se reemplaza por su sucesor en orden |
| `contains(x)` | Analizadora | — | `x ∈ B` |
| `max()`, `min()` | Analizadora | `B ≠ ∅` (si no: `EmptyStructureException`) | Mayor / menor elemento |
| `inOrder()`, `reverseInOrder()` | Analizadora | — | Lista ascendente / descendente |
| `findAll(p)` | Analizadora | — | Elementos que cumplen `p`, de mayor a menor (búsqueda por gravedad) |
| `height()`, `size()`, `isEmpty()` | Analizadora | — | Altura / `|B|` / `|B| = 0` |

---

## TAD UnionFind (conjuntos disjuntos)

Usado por Kruskal para saber si una arista cerraría un ciclo.

**Valores.** Particiones del conjunto `{0, …, n − 1}` en clases disjuntas.

**Representación.** Bosque en arreglos `parent[]` y `rank[]`; la raíz de cada árbol representa su clase.

**Invariante.** `parent[r] = r` si y solo si `r` es raíz; `rank[r]` es una cota superior de la altura del árbol de `r`; `componentCount` = número de raíces.

| Operación | Tipo | Precondición | Poscondición |
| --- | --- | --- | --- |
| `UnionFind(n)` | Constructora | `n ≥ 0` | `n` clases unitarias |
| `find(x)` | Analizadora* | `0 ≤ x < n` | Retorna la raíz de la clase de `x`. *Comprime el camino: los nodos visitados apuntan directo a la raíz (no cambia la partición) |
| `union(a, b)` | Modificadora | `0 ≤ a, b < n` | Une las clases de `a` y `b` colgando el árbol de menor rango del de mayor; retorna `false` si ya estaban juntas |
| `connected(a, b)` | Analizadora | — | `find(a) = find(b)` |
| `componentCount()` | Analizadora | — | Número de clases |
