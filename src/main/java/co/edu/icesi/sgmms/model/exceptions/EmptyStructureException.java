package co.edu.icesi.sgmms.model.exceptions;

/**
 * Raised when an element is requested from an empty structure: popping or peeking an empty stack,
 * dequeuing an empty queue, extracting from an empty priority queue, or undoing a movement when
 * the operator has no movement history.
 */
public class EmptyStructureException extends SgmmsException {

    private static final long serialVersionUID = 1L;

    public EmptyStructureException(String message) {
        super(message);
    }
}
