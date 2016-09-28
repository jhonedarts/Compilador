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
                erros.add(new Erro("programa", ver()));
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
                    if (!ver().getLexema().equals("fim")) // caso esteja vazio
                        varlist();// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                    else if (igual(ver().getLexema(),"programa", "const", "inicio")){
                        //nothing, deixar passar
                    }
                }else{
                    //panico pra caso nao tiver inicio
                    //aqui que será tomado a decisao de onde seguir apartir do token sincronizado
                    erros.add(new Erro("inicio", ver()));                    
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
                    erros.add(new Erro("fim", ver()));
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
    // <tipo> <R> <varlist2> | <>
    private void varlist() throws EndTokensException {
        if (igual(ver().getLexema(),"fim","programa","inicio")){
            return;
        }
        if (isTipo()){
            consumir();
            R();
            varlist();
        }else{
            //panico -tipo
            erros.add(new Erro("tipo", ver()));                
            sincronizar(",",";", "fim", "programa", "inicio","const", "identificador");
            if (igual(ver().getLexema(), ";")){
                consumir();
                varlist(); 
            }else if (igual(ver().getLexema(), ",")){
                consumir();
                R(); 
            }else if (igual(ver().getTipo(), "identificador")){
                R();
            }
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
            erros.add(new Erro("identificador", ver()));
            sincronizar(",",";", "fim", "programa", "const");
            if (igual(ver().getLexema(), ";", ","))
                R2();
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
            //panico - ou ;        
            sincronizar(",", "identificador", ";", "fim", "programa", "const");
            if(igual(ver().getLexema(), "identificador", ","))
                erros.add(new Erro(",",ver()));
            else
                erros.add(new Erro(";", ver()));
            if (igual(ver().getLexema(), ";", ","))
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
                erros.add(new Erro(">>>",ver()));
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
                    if (!ver().getLexema().equals("fim"))// se nao tiver vazio
                        constlist();// fica dentro do if mesmo, pq se der erro no else 
                                //escolho pra onde ir e nao obritoriamente ir pro proximo
                    else if (igual(ver().getLexema(),"programa","inicio")){
                        //nothing
                    }
                }else{
                    //panico pra caso nao tiver inicio
                    //aqui que será tomado a decisao de onde seguir apartir do token sincronizado
                    erros.add(new Erro("inicio",ver()));                    
                    sincronizar("inicio", ";", ",", "inteiro", "real", "booleano", "caractere", "cadeia", "fim", "programa");
                    if (igual(ver().getLexema(), ",", ";", "inteiro", "real", "booleano", "caractere", "cadeia"))
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
                    erros.add(new Erro("fim",ver()));
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
    
    //<Tipo><Const_Decl><Constlist> | <>
    private void constlist() throws EndTokensException{
        if (igual(ver().getLexema(),"fim","programa","inicio")){
            return;
        }
        if (isTipo()){
            consumir();
            const_decl();
            constlist();
        }else{
            //panico -tipo
            erros.add(new Erro("tipo", ver()));
            sincronizar(",",";", "fim", "programa", "inicio", "identificador");
            if (igual(ver().getLexema(), ";")){
                consumir();
                constlist(); 
            }else if (igual(ver().getLexema(), ",")){
                consumir();
                const_decl(); 
            }else if (igual(ver().getTipo(), "identificador"))
                const_decl();
            // se for fim progrma ou const nao fazer nada, pois assim retorna pra variaveis e ela se vira
        }         
    }
    
    //id'<<'<Literal><Const_Decl2>
    private void const_decl() throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            consumir();  
            if(ver().getLexema().equals("<<")){
                consumir(); 
                if(isLiteral()){
                    consumir();
                    const_decl2();
                }else{
                    //panico -valor literal
                    erros.add(new Erro("valor literal", ver()));
                    sincronizar(",", ";", "programa", "inicio", "fim");
                    if (igual(ver().getLexema(), ";", ","))
                        const_decl2();
                }
            }else{
                //panico -<<
                erros.add(new Erro("<<", ver()));
                sincronizar(",",";", "fim", "programa", "inicio", "caractere", "cadeia", "numero","verdadeiro", "falso");
                if (igual(ver().getLexema(), ";", ","))
                    const_decl2();
                else if (igual(ver().getLexema(),"verdadeiro", "falso")||igual(ver().getTipo(), "caractere", "cadeia", "numero")){
                    consumir();
                    const_decl2();
                }
            }            
        }else{
            //panico -id
            erros.add(new Erro("identificador", ver()));
            sincronizar(",",";", "fim", "programa", "inicio");
            if (igual(ver().getLexema(), ";", ","))
                const_decl2();
        } 
    }
    // ','id'<<'<Literal><Const_Decl2> | ';'
    private void const_decl2() throws EndTokensException{
        if (ver().getLexema().equals(",")){
            consumir();
            if(ver().getTipo().equals("identificador")){
                consumir();
                if (ver().getLexema().equals("<<")){
                    consumir();     
                    if(isLiteral()){
                        consumir();
                        const_decl2();
                    }else{
                        //panico - valor literal
                        erros.add(new Erro("valor literal", ver()));
                        sincronizar(",", ";", "programa", "inicio", "fim");
                        if (igual(ver().getLexema(), ";", ","))
                            const_decl2();
                    }
                }else{
                    //panico - <<
                    erros.add(new Erro("<<", ver()));
                    sincronizar(",",";", "fim", "programa", "inicio", "caractere", "cadeia", "numero","verdadeiro", "falso");
                    if (igual(ver().getLexema(), ";", ","))
                        const_decl2();
                    else if (igual(ver().getLexema(),"verdadeiro", "falso")||igual(ver().getTipo(), "caractere", "cadeia", "numero")){
                        consumir();
                        const_decl2();
                    }
                }
            }else{
                //panico - id
                erros.add(new Erro("identificador", ver()));
                sincronizar(",",";", "fim", "programa", "inicio");
                if (igual(ver().getLexema(), ";", ","))
                    const_decl2();
            }            
        }else if(ver().getLexema().equals(";")){
            consumir();
        }else{
            //panico - ou ;        
            sincronizar(",", "identificador", ";", "fim", "programa", "inicio", "inteiro", "real", "booleano", "caractere", "cadeia");
            if(igual(ver().getLexema(), "identificador", ","))
                erros.add(new Erro(",",ver()));
            else
                erros.add(new Erro(";", ver()));
            if (igual(ver().getLexema(), ";", ","))
                const_decl2();
        }
    }

    ////////////////////////    funcoes    /////////////////////////////
    private void funcao() {
        
    }

    
    
    ////////////////////////    expressoes    /////////////////////////////
    
    private void exp_aritimetica() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean isLiteral() throws EndTokensException {
        return igual(ver().getTipo(), "caractere", "cadeia", "numero")|| igual(ver().getLexema(), "verdadeiro", "falso");
    }
    
}
