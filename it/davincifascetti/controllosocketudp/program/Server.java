package it.davincifascetti.controllosocketudp.program;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

import it.davincifascetti.controllosocketudp.command.CommandException;
import it.davincifascetti.controllosocketudp.command.CommandFactory;
import it.davincifascetti.controllosocketudp.command.Commandable;
import it.davincifascetti.controllosocketudp.command.CommandableException;
import it.davincifascetti.controllosocketudp.errorlog.ErrorLogException;
/**classe server, crea un thread che si occupa della ricezione e uno separato (ServerThread) che si occupa di rispondere
 * 
 * @throws CommandableExceptio errori stampati sul terminale
 * @throws ErrorLogException errori stampati sul file
 * @author Mussaldi Tommaso, Mattia Bonfiglio
 * @version 1.0
 */
public class Server implements Runnable,Commandable{
    
    public static final int LunghezzaBuffer = 1024;
    private DatagramSocket socket;
    private String nome;
    private int porta;
    private boolean statoAttivo=false;
    private Thread threadAscolto = null;
    private ServerThread threadRisposta = null;
    private Terminal<Server> riferimentoTerminale;
    private ArrayList<String> StoriaMsg;
    private FileLogger fileLogger = null;

    /**permette di impostare la stampa su file di default
     * 
     * @param append true se si vuole stampare in modalità append altrimenti false
     * @param nomeFile nome del file su cui si vuole scrivere, se ha valore "this" , il file prende il nome del server
     * @throws CommandableException se il nome del file è vuoto oppure null
     */
    public void SuFile(boolean append,String nomeFile) throws CommandableException {
        
        if(nomeFile != null && nomeFile.equals("this")) this.fileLogger = new FileLogger(this.getNome() + ".txt");
        else this.fileLogger = new FileLogger(nomeFile);
        this.fileLogger.setAppend(append);
        
    }

    /**permette di disabilitare la stampa su file di default
     * (non avviene se il fileLogger è null a meno che non si specifichi il comando $file, in quel caso il file prende il nome del server)
     */
    public void disableSuFile(){this.fileLogger = null;}


    public FileLogger getFileLogger() {
        return fileLogger;
    }

    /**costruttore che prende due parametri, quindi se si usa questo non si può attivare il server, va settato il socket
     * 
     * @param nomeClient nome del server
     * @param t terminale che verra usato da questo server
     * @throws CommandableException
     */
    public Server(String nomeServer,Terminal<Server> t) throws CommandableException{
        if(t == null)throw new CommandableException("il terminale inserito non è valido");
        this.riferimentoTerminale = t;
        this.setNome(nomeServer);
        this.StoriaMsg = new ArrayList<String>();
    }
    /**se uso quest costruttore, posso attivare il server
     * 
     * @param nomeClient nome del server
     * @param porta porta locale su cui aprire il socket
     * @param t riferimento terminale che verra usato da questo server
     * @throws CommandableException
     * @throws ErrorLogException
     */
    public Server(String nomeServer, String porta,Terminal<Server> t) throws CommandableException,ErrorLogException{
        this(nomeServer,t);
        this.setPorta(porta);
        try {
            this.socket = new DatagramSocket(this.porta);
        } catch (SocketException e) {
            throw new ErrorLogException(e.getMessage());
        }
    }
    
    /**implementa la logica di ricezione e attiva il thread di risposta
     * 
     */
    @Override
    public void run(){
        if(!this.isAttivo())return;
        byte[] bufferIN = new byte[Server.LunghezzaBuffer];
        while(this.isAttivo()){
            DatagramPacket pacchetto = new DatagramPacket(bufferIN, Server.LunghezzaBuffer);
            try {
                this.socket.receive(pacchetto);
                if(this.isAttivo()){
                    this.threadRisposta = null;
                    this.threadRisposta = new ServerThread(pacchetto, this.socket,this.StoriaMsg,this.riferimentoTerminale,this.getNome(),this.fileLogger,this);
                    this.threadRisposta.start(); 
                }
            } catch (Exception e) {
                this.riferimentoTerminale.errorLog(e.getMessage(),false);
            }
        } 
        if(!this.socket.isClosed())this.socket.close();
    }

    /**permette di avviare il server (crea e avvia il thread)
     * 
     * @throws CommandableException
     * @throws ErrorLogException 
     */
    public void iniziaAscolto()throws CommandableException, ErrorLogException{
        if(this.socket == null) throw new CommandableException("Errore, la socket è null non può essere avviato, imposta una porta prima");
        if(this.socket.isClosed())
            try {
                this.socket = new DatagramSocket(this.porta);
            } catch (SocketException e) {
                throw new ErrorLogException(e.getMessage());
            }
        this.statoAttivo = true;
        if(this.threadAscolto == null)this.threadAscolto = new Thread(this);
        if(!this.threadAscolto.isAlive())this.threadAscolto.start(); 
    }

    /**permette di terminare l'ascolto del server (interrompe il thread creato in iniza ascolto)
     * chiude la socket
     */
    public void terminaAscolto(){
        this.statoAttivo = false;
        if(this.socket != null){
            if(!this.socket.isClosed())this.socket.close();
        }
        if(this.threadAscolto != null ) this.threadAscolto.interrupt();
        this.threadAscolto = null;
    }

    @Override
    public String toString() {
        return "Name: " + this.getNome() + "\t" + (this.socket == null?"Port: - ":("Port: "+this.getPorta()))  + "\t" +  (this.fileLogger == null?"ToFile: - ":("ToFile: "+this.fileLogger.getFileName())) +"\tStatus: "+ (this.isAttivo() ? "attivo" : "disattivo");
    }

    /**restitsce stringa contenente tutti i msg ricevuti dal client concatenati (aggiunge \n)
     * 
     * @return stringa contenente tutti i msg ricevuti dal client concatenati (aggiunge \n)
     */
    public String stampaStoriaMsg(){
        String temp = "";
        for (String string : StoriaMsg) {
            temp +=string+"\n";
        }
        return temp;
    }

    @Override
    public void startTerminal() throws CommandException {
        this.riferimentoTerminale.main(this);
    }

    public boolean isAttivo(){return this.statoAttivo;}

    public String getNome(){return this.nome;}
    /**imposta il nome (deve avere almeno una lettera, può contenere numeri, lettere e _ min 1 carattere e max 18)
     * 
     * @param nome nuovo nome del server
     * @throws CommandableException
     */
    public void setNome(String nome) throws CommandableException{
        //è valido solo se non ci sono spazi ed è possibile usare solo lettere e _ (deve esserci almeno una lettera e almeno 2 a 18 caratteri)
        if(nome.matches("^(?=.*[a-zA-Z])[a-zA-Z_0-9]{1,18}$"))
            this.nome = nome;
        else 
            throw new CommandableException("Errore, il nome '"+nome+"' inserito non è valido (deve contenere almeno una lettera, può contenere numeri da 0 a 9, lettere maiusc e minusc e '_')");
    }
    /**imposta la porta locale
     * 
     * @param port porta come Stringa perchè l'utente potrebbe scrivere una lettera e mandare in crash
     * @throws CommandableException
     */
    private void setPorta(String port)throws CommandableException{
        int p;
        try{
            p = Integer.valueOf(port);
        }catch(NumberFormatException e){
            throw new CommandableException("Errore, '" + port + "' non è un numero, specifica il numero della porta");
        }
        if(p < 0 || p > 65535)throw new CommandableException("Errore, la porta inserita non è valida (0-65535)");
        else this.porta = p;
    }
    public int getPorta(){return this.porta;}

    /**si occupa di terminare l'ascolto,modificare il socket e se era attivo riattivare la socket
     * 
     * @param porta nuova porta locale
     * @throws CommandableException
     * @throws ErrorLogException
     */
    public void setSocket(String porta) throws CommandableException, ErrorLogException{
        boolean wasActive = this.isAttivo();
        if(this.isAttivo())
            this.terminaAscolto();
        this.setPorta(porta);
        try {
            this.socket = new DatagramSocket(this.porta);
        } catch (SocketException e) {
            throw new ErrorLogException(e.getMessage());
        }
        if(wasActive)this.iniziaAscolto();
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Server){
            return ((Server)o).getNome().equals(this.getNome());
        }
        return false;
    }

    //possibilmente impostarlo da file xml?
    public void registraComandi() throws CommandException{
        String path = "it.davincifascetti.controllosocketudp.command.";
        //normali
        Server.comandi.registraComando( "^\b(he?l?p?[ ]*)|[?][ ]*$",path + "CommandHelp");
        Server.comandi.registraComando( "^\bin?f?o?[ ]*$",path + "CommandHelp");
        //set
        Server.comandi.registraComando( "^\bse?t?[ ]+po?r?t?[ ]+.*$",path + "CommandSetNomeServer");
        Server.comandi.registraComando( "^\bse?t?[ ]+na?m?e?[ ]+.*$",path + "CommandSetSocketServer");
        //$
        Server.comandi.registraComando( "^\b\\$lo?g?[ ]*$",path + "CommandStampaVideoServerThread");
                /* 
        String scelta = params == null || params.length == 0 ? "" : params[0];
        switch (scelta) {
            case "h":
            case "?":
            case "help":
                return new CommandHelp(          
                    "Comandi Terminale Server\n\n"+
                    "help\t\tpermette di visualizzare tutti i comandi \n" + 
                    "quit\t\tpermette di tornare al Terminale Generale \n" + 
                    "undo\t\tpermette di annullare l'ultima operazione significativa eseguita \n" + 
                    "info\t\tpermette di visualizzare le informazioni di questo server\n" +
                    "enable\t\tpermette di avviare questo server\n" +
                    "disable\t\tpermette di disattivare questo server\n" +
                    "set\t\tpermette di modificare la socket oppure il nome del server\n\t\t(set name nuovoNome) permette di cambiare il nome del server\n\t\t(set port nuovaPorta) permette di cambiare la porta del server\n"+
                    "file\t\tpermette di abilitare la stampa su file in maniera automatica di tutto ciò che viene inviato al server\n\t\t(file nomefile modalità) se si vuole stampare sul file che prende il nome di questo server , si usa 'this' al posto del nomeFile \n\t\tla modalità può essere append oppure overwrite\n\t\t(file disable) permette di disabilitare la stampa su file, una volta disabilitata,\n\t\tsarà necessario usare il comando (file nomefile modalita) per riattivarla\n"
                );
            case "info":
            case "i":
                return new CommandHelp(this.getGestore().toString());
            case "s":
            case "set":
                switch (params == null || params.length <= 2 ? "" : params[1]) {
                case "name":
                case "n":                      
                    if(params.length == 3)return new CommandSetNomeServer(this.getGestore(),params[2]);
                    throw new CommandException("Errore,non è stato specificato cosa creare");
                case "port":
                case "p":
                    if(params.length == 3)return new CommandSetSocketServer(this.getGestore(),params[2]);
                    throw new CommandException("Errore,non è stato specificato cosa creare");
                default:
                    throw new CommandException("Errore, non è stato specificato cosa selezionare");
                }
            case "en":
            case "enable": 
                return new CommandAttivaServer(this.getGestore());
            case "dis":
            case "disable": 
                return new CommandDisattivaServer(this.getGestore());
            case "file":
            case "f":
                if(params == null || params.length <= 1 || params.length > 3) throw new CommandException("Errore, non è stato specificato cosa selezionare");
                if(params.length == 2){
                    switch(params[1]){
                        case "dis":
                        case "disable":
                            return new CommandDisableToFile(this.getGestore());
                        default:  
                            throw new CommandException("Errore, non è stato specificato cosa selezionare");
                    }
                }
                switch(params[2]){
                    case "append":
                    case "a":
                        return new CommandEnableToFile(this.getGestore(),params[1],true);
                    case "overwrite":
                    case "o":
                        return new CommandEnableToFile(this.getGestore(),params[1],false);
                    default:  
                        throw new CommandException("Errore, non è stato specificato cosa selezionare");
                }
                
            default:    
                return new CommandDefault(params);
        }
        */
    }

}
