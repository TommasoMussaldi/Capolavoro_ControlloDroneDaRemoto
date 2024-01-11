package it.davincifascetti.controllosocketudp.command;

import it.davincifascetti.controllosocketudp.errorlog.ErrorLogException;
import it.davincifascetti.controllosocketudp.program.GestoreClientServer;
import it.davincifascetti.controllosocketudp.program.Server;
import it.davincifascetti.controllosocketudp.program.Terminal;

public class CommandNewServer extends CommandI<GestoreClientServer> implements UndoableCommand{

    private String nome;
    private Terminal<Server> terminale;
    private Integer porta = null;
    public CommandNewServer(GestoreClientServer gestore,Terminal<Server> terminale, String nome) throws CommandException {
        super(gestore);
        this.nome = nome;
        this.terminale = terminale;
    }
    public CommandNewServer(GestoreClientServer gestore,Terminal<Server> terminale, String nome,int porta) throws CommandException {
        this(gestore, terminale, nome);
        this.porta = porta;
    }

    @Override
    public void execute() throws CommandException, ErrorLogException {
        try{
            if(porta == null)
                this.getGestore().newServer(terminale,nome);
            else
                this.getGestore().newServer(terminale,nome,porta);
                
        }catch(CommandableException e){
            throw new CommandException(e.getMessage());
        }
    }

    @Override
    public boolean undo() {
       return true;
    }
    
}