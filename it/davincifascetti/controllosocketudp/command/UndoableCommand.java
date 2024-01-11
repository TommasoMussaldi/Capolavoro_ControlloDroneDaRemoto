package it.davincifascetti.controllosocketudp.command;

/**interfaccia Command con metodo undo
 * 
 */
public interface UndoableCommand extends Command{

    /**undo permette di annullare il comando
     * 
     */
    public boolean undo();
} 