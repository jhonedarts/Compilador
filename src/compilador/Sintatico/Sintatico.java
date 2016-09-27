/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.Sintatico;

import compilador.Lexico.Token;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Jhone e Matheus Castro
 */
public class Sintatico {
    private LinkedList<Token> tokens;
    private LinkedList<Erro> erros;
    private int atual;

    public Sintatico() {
        erros = new LinkedList<Erro>();
        tokens = new LinkedList<Token>();
    }
 
    public void start(LinkedList<Token> tokens){        
        atual=0;
        this.tokens.addAll(tokens);        
        //tirando os comentarios
        for(Token tok: tokens){
            if(tok.getTipo().equals("comentario"))
                this.tokens.remove(tok);
        }
        
        System.out.println("----------------- Analise Sintatica -----------------\n");
        programa();
        if (erros.isEmpty())
            System.out.println("Sucesso!");
        else{
            for(Erro error: erros){
                System.out.println("Esperava \""+error.getEsperado()+"\" mas obteve \""+error.getObtido()+"\" na linha: "+error.getLinha());
            }
        }
    }
    //vê o token atual
    private Token ver() throws EndTokensException{
        return verLLX(0);
    }
    //vê qualquer token que vósmicê quiseres
    private Token verLLX(int x) throws EndTokensException{
        if (atual + x >= tokens.size())
            throw new EndTokensException("final não esperado");// mudar tipo de excecao depois
        return tokens.get(atual + x);
    }
    //consome o token atual incrementando o valor de "atual"
    private void consumir(){
        atual++;
    }
    // modo panico
    // vai consumir ate o atual se tornar um token de sync passado por parametro
    private void sincronizar(String... syncC) throws EndTokensException{   
        List<String> sync = Arrays.asList(syncC);
    	while(!sync.contains(ver().getLexema()) && !sync.contains(ver().getTipo())){
    		System.out.println("Pulou Token: " + ver().getLexema());
    		consumir();
    	}
    }
    
    //super equals
    private boolean igual(String... s){
        String referencia = s[0];
        for(int i=1; i<s.length;i++){
            if (referencia.equals(s[i])){
                return true;
            }
        }            
        return false;
    }
    ///// BAGACEIRA //////
    
    // var c | c
    private void programa(){
        variaveis();
        c();
    }
    
    // c programa | programa
    private void c(){
        constantes();
        p();        
    }
    
    // programa | programa funcoes 
    private void p(){
        //programa p
        try {
            if (ver().getLexema().equals("programa")){
                //bloco
                consumir();
            }else{
                erros.add(new Erro("programa", ver().getLexema(), ver().getLinha()));
                sincronizar("inicio", ";", "sync1", "sync2", "sync3");//escolher tokens sync para programa
                //sincronizar leitura com token que vai para bloco
                if (ver().getLexema().equals("inicio")||ver().getLexema().equals(";")){//vericicar todos os tokens que vao pra bloco
                    //bloco();
                    consumir();
                }
                //sincronizar leitura com token que vai para funcao
                else if (ver().getLexema().equals("funcao")){
                    //funcao();
                }
                
            }
            if (ver().getLexema().equals("inicio")){
                //bloco
                consumir();
            }
            if (ver().getLexema().equals("fim")){
                //bloco
                consumir();
            }
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }  
        funcoes();
    }
    
    // funcao funcoes | vazio
    private void funcoes(){
        try {
            System.out.println(ver().getLexema());// so pra checar se o arquivo acabou, se sim, dara a exception
            funcao();// tratar fim de arquivo dentro desta tb, pois ai seria erro
            funcoes();
        } catch (EndTokensException ex) {
            //fim de arquivo
        }  
    }
    
    ////////////////////////    variaveis    /////////////////////////////
    
    // var inicio <varlist> fim
    private void variaveis(){
        try {
            if (ver().getLexema().equals("var")){//se nao tem var pula pra c()                
                consumir();//consome var
                if (ver().getLexema().equals("inicio")){
                    consumir();
                    varlist();// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                }else{
                    //panico pra caso nao tiver inicio
                    //aqui que será tomado a decisao de onde seguir apartir do token sincronizado
                    erros.add(new Erro("inicio", ver().getLexema(), ver().getLinha()));                    
                    sincronizar("inicio", ";", "inteiro", "real", "booleano", "caractere", "cadeia", "fim", "programa");
                    if (igual(ver().getLexema(), ";", "inteiro", "real", "booleano", "caractere", "cadeia"))
                        varlist();
                    else if(igual(ver().getLexema(), "inicio")){ 
                        consumir();
                        varlist();
                    }
                }                
                if (ver().getLexema().equals("fim")){
                    consumir();
                }else{
                    //panico pra caso nao tiver fim
                    erros.add(new Erro("fim", ver().getLexema(), ver().getLinha()));
                    sincronizar("fim", "programa", "const");     
                    if (igual(ver().getLexema(), "fim"))
                        consumir();
                    //deixa passar                    
                }
            } 
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }
    }

    // inteiro | real | booleano | cadeia | caractere
    private boolean isTipo() throws EndTokensException{
        return igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia");
    }
    // <tipo> <R> <varlist2>
    private void varlist() throws EndTokensException {
        if (isTipo()){
            consumir();
            R();
            varlist2();
        }else{
            //panico -tipo
            erros.add(new Erro("tipo", ver().getLexema(), ver().getLinha()));
            sincronizar(";", "fim", "programa", "const", "inicio");
            if (igual(ver().getLexema(), ";"))
                varlist2(); 
            // se for fim progrma ou const nao fazer nada, pois assim retorna pra variaveis e ela se vira
        }            
    }
    // <varlist> | <>
    private void varlist2() throws EndTokensException{
        if(isTipo())
            varlist();
    }
    //id <vetor> <R2>
    private void R() throws EndTokensException {
        if (ver().getTipo().equals("identificador")){
            consumir();
            vetor();
            R2();
        }else{
            //panico pra falta de id
            erros.add(new Erro("identificador", ver().getLexema(), ver().getLinha()));
            sincronizar(";", "fim", "programa", "const");
            if (igual(ver().getLexema(), ";"))
                consumir();
        }
    }
    //','<R> | ';'
    private void R2() throws EndTokensException{
        if(ver().getLexema().equals(",")){
            consumir();
            R();
        } else if(ver().getLexema().equals(";")){
            consumir();
        } else{
            //panico
            erros.add(new Erro(";", ver().getLexema(), ver().getLinha()));
            sincronizar(";", "fim", "programa", "const");
            if (igual(ver().getLexema(), ";"))
                consumir();            
        }
    }
    //'<<<'<Exp_Aritmetica>'>>>'<vetor> | <>
    private void vetor() throws EndTokensException {
        if(ver().getLexema().equals("<<<")){
            consumir();
            exp_aritimetica(); // a fazer              
            if(ver().getLexema().equals(">>>")){
                consumir();
                vetor();
            }else{
                //panico
                erros.add(new Erro(">>>",ver().getLexema(), ver().getLinha()));
                //só
            }
        }
    }
    
    ////////////////////////    constantes    /////////////////////////////
    
    //const inicio <constlist> fim
    private void constantes(){
        try {
            if (ver().getLexema().equals("const")){//se nao tem var pula pra c()                
                consumir();//consome var
                if (ver().getLexema().equals("inicio")){
                    consumir();
                    constlist();// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                }else{
                    //panico pra caso nao tiver inicio
                    //aqui que será tomado a decisao de onde seguir apartir do token sincronizado
                    erros.add(new Erro("inicio",ver().getLexema(),ver().getLinha()));                    
                    sincronizar("inicio", ";", "inteiro", "real", "booleano", "caractere", "cadeia", "fim", "programa");
                    if (igual(ver().getLexema(), ";", "inteiro", "real", "booleano", "caractere", "cadeia"))
                        constlist();
                    else if(igual(ver().getLexema(), "inicio")){ 
                        consumir();
                        constlist();
                    }
                }                
                if (ver().getLexema().equals("fim")){
                    consumir();
                }else{
                    //panico pra caso nao tiver fim
                    erros.add(new Erro("fim",ver().getLexema(), ver().getLinha()));
                    sincronizar("fim", "programa", "inicio");     
                    if (igual(ver().getLexema(), "fim"))
                        consumir();
                    //deixa passar                    
                }
            } 
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }
    }
    
    //<tipo> <constatrib> ';' <constlist2>
    private void constlist() throws EndTokensException{
        if (isTipo()){
            consumir();
            constatrib();
        }else{
            //panico -tipo
        }
        if(ver().equals(";")){
            consumir();
            constlist2();
        }else{
            //panico -;
        }
    }
    //<constlist> | <>
    private void constlist2() throws EndTokensException{
        //se nao tiver um tipo depois do
        if(isTipo() || ver().getTipo().equals("identificador")){
            constlist();
        }
    }
    //id '<''<' <Literal><constatrib2> | id '<''<' <Literal>
    private void constatrib() throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            consumir();            
        }else{
            //panico -id
        }
        if(ver().getLexema().equals("<<")){
            consumir();
            
        }else{
            //panico -<<
        }
    }

    ////////////////////////    funcoes    /////////////////////////////
    private void funcao() {
        
    }

    
    
    ////////////////////////    expressoes    /////////////////////////////
    
    private void exp_aritimetica() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
