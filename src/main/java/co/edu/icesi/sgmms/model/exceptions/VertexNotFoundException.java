package co.edu.icesi.sgmms.model.exceptions;

/** Raised when a graph operation refers to a vertex that was never added to the graph. */
public class VertexNotFoundException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public VertexNotFoundException(Object vertex) {
        super("The vertex " + vertex + " does not exist in the graph");
    }
}
