package it.davincifascetti.controllosocketudp.command;

import it.davincifascetti.controllosocketudp.program.ServerThread;

/** stampa a video usando Server.stampaVideo(String) così da avere i vantaggi di quel metodo (vedi Server.stampaVideo)
 *  invia al client risposta di successo
 *  @author Mussaldi Tommaso, Mattia Bonfiglio
 *  @version 1.0
 */
public class CommandStampaVideoServerThread implements Command{
    private String msg;
    private ServerThread gestore = null;
    /**
     * 
     * @param msg messaggio da stampare 
     * @param gestore thread di risposta che si occuperà di stampare e inviare msg risposta
     * @throws CommandException
     */
    public CommandStampaVideoServerThread(String msg,ServerThread gestore) throws CommandException{
        this.msg = msg;
        this.gestore = gestore;
        if(msg == null) throw new CommandException("Errore, il msg inserito risulta essere null");
        if(gestore == null) throw new CommandException("errore, il gestore inserito è null");
    }
    public void execute() throws CommandException {
        this.gestore.stampaVideo("il client dice: " + msg);
        new CommandHelp("operazione andata a buon fine",this.gestore).execute();
    }

}
