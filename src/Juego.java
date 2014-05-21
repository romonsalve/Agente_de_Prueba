
import ComandosAgente.*;
import ComandosAgente.ACCEPT;
import ComandosAgente.ArgumentsRegister;
import ComandosAgente.ArgumentsSessionStart;
import ComandosAgente.REGISTER;
import ComandosAgente.REG_SUCESS;
import ComandosAgente.RULES;
import ComandosAgente.SESSION_START;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kerux
 */
public class Juego {
    
//reglas y características del tablero
    
    public static String id; // id asignado al cliente durante el proceso de registro
    public static int turnDuration; // tiempo que duran los turnos, solo si timedTurn es verdadero
    public static int maxRoundTime; //limite de duracion de un round
    public static int roundsPerMatch; // numero de rondas a jugar por cada enfrentamiento entre jugadores distintos
    public static int boardSize;

    
    public static int[][] tablero;
    public static boolean jugando4 = true;
    
    /*Nombre y características del Agente*/
    
    public static String nombreAgente = "Agente 1";
    public static String passAgente = "TresTristesTigres";
    public static String tipoAgente = "IA";
    public static int turno = 3;//1 = primero, 2 = segundo, 3 = aun no asignado (no es necesario el 3 pero se quedara así mientras).
    
    /*nombre y caracteristicas del adversario*/
    public static String nombreAdversario;
    public static String idAdversario;
    public static String nombrePartida;
    
    public static float cantTurnos = 0;
    
    
    
    public void aplicacion()throws IOException{
    
        /*creamos las variables necesatias para la comunicacion*/
        
        Socket socket = new Socket("localhost",7075);
    
        DataOutputStream salida = new DataOutputStream(socket.getOutputStream());

        BufferedReader  entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
          
        /*Comunicacion generada*/
        
        /*Comenzando proceso de registro*/
        
        Conversion conversion = new Conversion();
        
        /*iniciando nuevo registro*/
        
        REGISTER nuevoRegistro = new REGISTER();
        nuevoRegistro = conversion.creaRegistro("Agente1", "abcdef", "IA"); // creando json de registro
        Gson gson = new Gson();
        String jsonRegistro = gson.toJson(nuevoRegistro); // creando string json
        
        
        /*enviando registro al GM*/
        salida.writeUTF(jsonRegistro); // enviando registro al gm
        System.out.println(jsonRegistro);
        String resRegistro = entrada.readLine(); // recibindo respuesta
        
       
        
        if(resRegistro.equals("{\"command\":\"ERR_UNKOWN_COMMAND\",\"arguments\":()")){
        
            System.out.println("Error 1: Registro Invalido");
        
        }
        else{
        /*zona de registro correcto*/
            System.out.println(resRegistro);
            
            REG_SUCESS registroValido = new REG_SUCESS();
            
            registroValido = conversion.registroValido(resRegistro);
            
            System.out.println(registroValido.getCommand());
            

            System.out.println("estoy aca");
            id = registroValido.getId(); // obteniendo la id enviada por el GM
            
            /*Iniciando nueva sesion*/
            SESSION_START nuevaSesion = new SESSION_START();
            nuevaSesion = conversion.inicioSesion(id);
            String jsonSesionStart = gson.toJson(nuevaSesion);
            
            //System.out.println(jsonSesionStart);
            salida.writeUTF(jsonSesionStart);
            String recSessionSucess = entrada.readLine();
            
            //System.out.println(recSessionSucess);
            /*recibiendo reglas*/
            RULES reglas = new RULES();
            reglas = conversion.reglas(recSessionSucess);
            
          //  System.out.println(reglas.getCommand());
            
            /*casteando reglas*/
            boardSize = Integer.parseInt(reglas.getBoardSize()); 
            turnDuration = Integer.parseInt(reglas.getTurnDuration()); 
            maxRoundTime = Integer.parseInt(reglas.getMaxRoundTime()); 
            roundsPerMatch = Integer.parseInt(reglas.getRoundsPerMatch()); 
            
            
            /*Aceptando las reglas*/
            
            ACCEPT aceptando = new ACCEPT();
            aceptando = conversion.aceptando(id);  
            String aceptandoStr = gson.toJson(aceptando);
            
            salida.writeUTF(aceptandoStr);
            
            String aceptandoACKStr = entrada.readLine();
           
           // System.out.println(aceptandoACKStr);
            
            if(!aceptandoACKStr.equals("\"sin definir\"")){
            
                System.out.println("Error 2: mensaje desde GM \"sin definir\" no llego => ack no llego ");
                
            }
            else{
                
              /*ack de aceptacion de reglas logrado*/
                MATCH_LOOK_UP matchUP = new MATCH_LOOK_UP();
                matchUP = conversion.matchLookup(nombreAgente, id);
                String matchUPStr = gson.toJson(matchUP);
                
               // System.out.println(matchUPStr);
                
                salida.writeUTF(matchUPStr);
                
                String matchOK = entrada.readLine();
                
              //  System.out.println(matchOK);
                
                /*match Look UP OK*/
                RULES msjMatchOK = new RULES();
                msjMatchOK = conversion.reglas(matchOK);
                
               
                if(!msjMatchOK.getCommand().equals("MATCH_LOOKUP_OK")){
                
                    System.out.println("Error 3: se esta repitiendo el proceso de matchLookup\n o hubo un error en el envio del comando");
                    
                }
                else{
                    
                    /*iniciando proceso de notificacion (Match Notify)*/

                    String notifys = entrada.readLine();
                    MATCH_NOTIFY matchNotify = new MATCH_NOTIFY();
                    matchNotify = conversion.notificacion(notifys);
                    
                    /*sacando datos del json matchNotify*/
                    nombreAdversario = matchNotify.getAdvName(); // nombre del adversario actual
                    idAdversario = matchNotify.getAdvId(); // id del adversario actual
                    nombrePartida = matchNotify.getMatchName(); // nombre de la partida actual
                
                    /*enviando aceptacion de match*/
                    MATCH_READY iniciando = new MATCH_READY();
                    iniciando = conversion.matchListo(id);
                    String matchReadyStr = gson.toJson(iniciando);
                    
                    salida.writeUTF(matchReadyStr);
                    
                    String OK = entrada.readLine(); // donde recibo un ok
                    
                    if(!OK.equals("OK")){
                    
                    System.out.println("Error 4: Ok no recibido");
                    }
                    else{
                    
                    /*Comienzo del match*/
                        
                        String roundStartStr = entrada.readLine();
                        
                        ROUND_START roundStart = new ROUND_START();
                        roundStart = conversion.roundStart(roundStartStr);
                        
                        if(convStrABool(roundStart.getFirstMove())==true){
                       
                            turno = 1;
                        }
                        else{
                
                            turno = 2;
                        }
                                
                        ROUND_START_ACK roundSAck = new ROUND_START_ACK();
                        roundSAck = conversion.roundStartACK(id);
                        String rsAckStr = gson.toJson(roundSAck);
                        
                        salida.writeUTF(rsAckStr);
                        
                        /*comienza el envio de turns*/
                
                        String strTurn = entrada.readLine();
                        
                        TURN turnData = new TURN();
                        
                
                        turnData = conversion.turno(strTurn);
                
                        cantTurnos = cantTurnos+1;
                
                        //int jugada1 = 1;
                        int x,y;
                        /*Comenzando el juego*/
                        while(jugando4){
                    
                            // if(jugada1 == 1 && turno == 1){
                            //  if(turno == 1 && turnData.getArguments().getAdvMove().getMove().equals("FIRST")){   
                            if(turno == 1 && cantTurnos%2 !=0){    
                         
                                x = randomico(boardSize);
                                y = randomico(boardSize);
                          
                                PUT move = new PUT();
                                move = conversion.put(x, y, id);
                                strTurn = gson.toJson(move);
                          
                          
                                 salida.writeUTF(strTurn);
                          //     tablero[x][y] = 1;
                          
                          
                            //   jugada1 = 0;  
                                }
                            else{
                    
                            //if(turno == 2 && turnData.getArguments().getAdvMove().getMove().equals("WAIT") ){
                            if(turno==2 && cantTurnos%2==0){
                        
                        
                                x = randomico(boardSize);
                                y = randomico(boardSize);
                          
                                PUT move = new PUT();
                                move = conversion.put(x, y, id);
                                strTurn = gson.toJson(move);
                          
                                salida.writeUTF(strTurn);
                   //           tablero[x][y] = 1;
                        
                    
                            //jugada1 = 0;
                                }
                            else{
                    
                             /*espero por nuevo turno*/
                             strTurn = entrada.readLine();
                        
                        
                                }
                    
                            }
                
                
                    
                    
                }// fin while del juego
                        
                    
                    } // recepcion del ok después del match Ready
                    
                } // match lock up ok
                
            } // ACK aceptado (mensaje sin definir desde GM)
        }// comando invalido en inicio de registro
    }
   
    public boolean convStrABool(String x){
    if(x.equals("true")){
    
        return true;
    }
        
    return false;
    }
    /*gemera numero random entre 0 y el largo, se utilizara para ubicar posiciones en el tablero*/
    public int randomico(int largo){
        
        Random x = new Random();
    
    return x.nextInt(largo+1);
    }
    


    private int[][] iniciarMatriz(int[][] m, int x, int y) {
              for(int i=0;0<x;i++){
            for(int j=0;j<y;j++){
                m[x][y]=0;
            }
        
        }
        
    return m;//To change body of generated methods, choose Tools | Templates.
    }
    
}
