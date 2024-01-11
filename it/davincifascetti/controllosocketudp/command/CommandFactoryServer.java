package it.davincifascetti.controllosocketudp.command;

import it.davincifascetti.controllosocketudp.program.Server;

/**Factory per la creazione di specifici comandi per il Server

 * 
 */
public class CommandFactoryServer extends CommandFactoryI<Server> implements CommandFactory{
    public CommandFactoryServer(Server gestore) throws CommandException{
        super(gestore);
    }
    public Command getCommand(String []params) throws CommandException {

        String scelta = params == null || params.length == 0 ? "" : params[0];
        switch (scelta) {
            case "h":
            case "?":
            case "help":
                return new CommandHelp( "help server");
            case "change":
                //TODO change name
            case "setport":
            case "setp":
                //TODO set della porta port
            case "en":
            case "enable": 
                return new CommandAttivaServer(this.getGestore());
            case "dis":
            case "disable": 
                return new CommandDisattivaServer(this.getGestore());
            default:    
                return new CommandDefault(params);
        }
    }

}
