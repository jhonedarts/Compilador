/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.sintatico;

import compilador.lexico.Token;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
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
    
    //variaveis usadas no Sematico funcoes 
    private String tipo;
    private Funcao funcao;
    private String parametro;
    private boolean isVetor;
    private LinkedList<Funcao> funcoes;
    private boolean ok =false;

    public Sintatico() {
        
    }
 
    public LinkedList<Funcao> start(LinkedList<Token> tokens, String nomeArq) throws IOException{   
        this.erros = new LinkedList<Erro>();
        this.tokens = new LinkedList<Token>();
        this.funcoes = new LinkedList<Funcao>();
        
        tipo =null;
        funcao =null;
        parametro = null;
        isVetor= false;
        nomeArq = nomeArq.split(".txt")[0];
        atual=0;
        this.tokens.addAll(tokens);        
        //tirando os comentarios
        for(Token tok: tokens){
            if(tok.getTipo().equals("comentario"))
                this.tokens.remove(tok);
        }
        
        //System.out.println("----------------- Analise Sintatica -----------------\nArquivo: "+nomeArq+"\n");
        programa();
        if (erros.isEmpty())
            //System.out.println("Sucesso!");
            System.out.println("");
        else{
            for(Erro error: erros){
                System.out.println("Esperava \""+error.getEsperado()+"\" mas obteve \""+error.getObtido()+"\" na linha: "+error.getLinha());               
            }
        }
        
        //gravar no txt
        File file = new File("saida/"+nomeArq+"_sintatico.txt");

        if (!file.exists()){
            new File("saida/"+nomeArq+"_sintatico.txt").createNewFile();
            file = new File("saida/"+nomeArq+"_sintatico.txt");
        }

        PrintWriter gravarArq = new PrintWriter(new FileWriter(file));
        if (erros.isEmpty())
            gravarArq.printf("Sucesso!%n");
        else{
            for(Erro error: erros){
                gravarArq.printf(error.getLinha()+" Esperava \""+error.getEsperado()+"\" mas obteve \""+error.getObtido()+"\"%n");
            }
        }
        gravarArq.close();
        if(erros.isEmpty())
            ok = true;
        return funcoes;
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
    /////////////////////////    BAGACEIRA    //////////////////////////
    
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
                consumir();
                bloco();                
            }else{
                erros.add(new Erro("programa", ver()));
                sincronizar("inicio", ";", "sync1", "sync2", "sync3");//escolher tokens sync para programa
                //sincronizar leitura com token que vai para bloco
                if (ver().getLexema().equals("inicio")||ver().getLexema().equals(";")){//vericicar todos os tokens que vao pra bloco
                    bloco();                    
                }
                //sincronizar leitura com token que vai para funcao
                else if (ver().getLexema().equals("funcao")){
                    //deixa apssar para ir pra funcoes ali em baixo
                }
                
            }
        } catch (EndTokensException ex) {
            System.out.println(ex);
        }  
        funcoes();
    }
    
    // funcao funcoes | vazio
    private void funcoes(){
        try {
            //System.out.println(ver().getLexema());// so pra checar se o arquivo acabou, se sim, dara a exception
            funcao_decl();// tratar fim de arquivo dentro desta tb, pois ai seria erro
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

    // <tipo> <R> <varlist> | <>
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
    //'<<<'<Exp_Aritmetica><vetor2>'>>>' | <>
    private void vetor() throws EndTokensException {
        if(ver().getLexema().equals("<<<")){
            parametro = "vetor "+parametro;
            isVetor= true;
            consumir();
            exp_aritmetica(); // a fazer   
            vetor2();
            if(ver().getLexema().equals(">>>")){
                consumir();                
            }else{
                //panico
                erros.add(new Erro(">>>",ver()));
                sincronizar(">>>", ";");
                if(ver().getLexema().equals(">>>"))
                    consumir();
                //só
            }
        }
    }
    
    //','<Exp_Aritmetica><Vetor2> | <>
    private void vetor2() throws EndTokensException{
        if(ver().getLexema().equals(",")){
            consumir();
            exp_aritmetica();
            vetor2();
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
        if(ver().getTipo().equals("identificador")){consumir();  
            if(ver().getLexema().equals("<<")){
                consumir(); if(isLiteral()){
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
    
    ////////////////////////    Bloco      /////////////////////////////
    
    //'inicio'<Corpo_Bloco>'fim'
    private void bloco() throws EndTokensException{
        if(ver().getLexema().equals("inicio")){
            consumir();
            corpo_bloco();
        }else{
            //panico -inicio
            erros.add(new Erro("inicio", ver()));
            sincronizar("inicio","var","inteiro", "real", "booleano", "caractere", "cadeia",
                    "escreva", "leia", "se", "enquanto", "identificador", "fim", "(");
            if(ver().getLexema().equals("inicio"))
                consumir();
            corpo_bloco();
        }
        if(ver().getLexema().equals("fim")){
            consumir();
        }else{
            //panico -fim
            erros.add(new Erro("fim", ver()));
            sincronizar("fim", "funcao", "inicio");
            if(ver().getLexema().equals("fim"))
                consumir();
        }
    }
    //<Var_Local><Corpo_Bloco> | <Comando><Corpo_Bloco> 
    // | <Chamada_Funcao>';'<Corpo_Bloco> | <Atribuicao><Corpo_Bloco> | <>
    private void corpo_bloco() throws EndTokensException{
        //System.out.println("corpo_bloco");
        if (igual(ver().getLexema(),"fim", "funcao")){            
            return;
        }
        if (igual(ver().getLexema(), "var","inteiro", "real", "booleano", "caractere", "cadeia")){
            var_local();            
        } else if (igual(ver().getLexema(), "escreva", "leia", "se", "enquanto")){
            comando();
        } else if (ver().getTipo().equals("identificador") ){
            if (verLLX(1).getLexema().equals("(")){
                chamada_funcao();
                if(ver().getLexema().equals(";"))
                    consumir();
                else{
                    //panico - ;
                    //deixa passar somente ai corpo_bloco resolvera
                }
            }else{
                atribuicao();
            }
        } else if (ver().getLexema().equals("(")){
            //pode ser comandos ou chamada de funcao
            erros.add(new Erro("identificador|comando", ver()));
            sincronizar(";", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                        
        } else{
            //sei nem que erro é que da aqui
            // caractere inesperado?!?!
            // panico para sincronizar com algo valido
            erros.add(new Erro("Algum inicio de instrucao valida", ver()));
            sincronizar(";", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
            
        }
        corpo_bloco();
    }
    ////////////////////////    variaveis locais    //////////////////////////
    
    //'var'<Tipo><Id_Vetor><Var_Local2>
    private void var_local() throws EndTokensException{
        if (ver().getLexema().equals("var")){
            consumir();
            if(isTipo()){
                consumir();
                id_vetor();
                var_local2();
            }else{
                //panico -tipo
                erros.add(new Erro("tipo", ver()));
                sincronizar(";",",", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                if(ver().getLexema().equals(","))
                    var_local2();
                //else so deixa passar
            }
        }else{
            //panico -var
            erros.add(new Erro("var", ver()));
            //repetindo o que esta acima
            if(isTipo()){
                consumir();
                id_vetor();
                var_local2();
            }else{
                //panico -tipo
                erros.add(new Erro("tipo", ver()));
                sincronizar(";",",", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                if(igual(ver().getLexema(),",", ";"))
                    var_local2();
                //else so deixa passar
            }
        }
    }
    
    //','<Id_Vetor><Var_Local2> | ';'
    private void var_local2() throws EndTokensException{
        if (ver().getLexema().equals(",")){
            consumir();
            id_vetor();
            var_local2();
        }else if(ver().getLexema().equals(";")){
            consumir();
        }else{
            //panico - , ou ;
            erros.add(new Erro(",|;", ver()));
            sincronizar(";",",", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
            if(igual(ver().getLexema(),",", ";"))
                var_local2();
                //else so deixa passar
        }
    }
    ////////////////////////    comandos    /////////////////////////////
    
    //<Escreva> | <Leia> | <Se> | <Enquanto>
    private void comando() throws EndTokensException{              
        if (ver().getLexema().equals("escreva")){
            escreva();
        }

        else if(ver().getLexema().equals("leia")){
            leia();
        }
        else if (ver().getLexema().equals("se")){
            se();
        }
        else if (ver().getLexema().equals("enquanto")){
            enquanto();
        }        
    }
    
    //'escreva''('<Escreva_Params>')'';'
    private void escreva() throws EndTokensException{
        if (ver().getLexema().equals("escreva")){               
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                escreva_params();
                if(ver().getLexema().equals(")"))
                    consumir();
                else{
                    erros.add(new Erro(")", ver()));
                    sincronizar(";");
                    consumir();
                }
                if(ver().getLexema().equals(";"))
                    consumir();
                else{
                    erros.add(new Erro(";", ver()));
                    sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", "identificador",
                            "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                            "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                }
            }else{
                erros.add(new Erro("(", ver()));
                sincronizar(";");
                consumir();
            }
        }else{
            erros.add(new Erro("escreva", ver()));
            sincronizar("(", ";");     
            if (igual(ver().getLexema(), "(")){
                consumir();
                escreva_params();
            }
            else 
                consumir();
            //deixa passar                    
        }        
    }
    //<Exp_Aritmetica><Escreva_Param2> | caractere_t<Escreva_Param2> | cadeia_t<Escreva_Param2>
    private void escreva_params() throws EndTokensException{
        //System.out.println(ver().getLexema()+" escreva_params");
        if(ver().getTipo().equals("cadeia")){
            consumir();
            escreva_params2();
        }//<Valor_Numerico> ::= '('<Exp_Aritmetica>')' | <Id_Vetor> | <Chamada_Funcao> | numero_t
        else if(ver().getTipo().equals("caractere")){
            consumir();
            escreva_params2();
        }else{
            exp_aritmetica(); 
            escreva_params2();
        }
    }

    //<Escreva_Param2> ::= ','<Escreva_Params> | <>
    private void escreva_params2() throws EndTokensException {
        if(ver().getLexema().equals(","))
            escreva_params();    
    }
    //'leia''('<Leia_Params>')'';'
    
    private void leia() throws EndTokensException{
        if (ver().getLexema().equals("leia")){               
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                leia_params();
                if(ver().getLexema().equals(")"))
                    consumir();
                else{
                    erros.add(new Erro(")", ver()));
                    sincronizar(";");
                    consumir();
                }
                if(ver().getLexema().equals(";"))
                    consumir();
                else{
                    erros.add(new Erro(";", ver()));
                    sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", "identificador",
                            "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                            "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                }
            }else{
                erros.add(new Erro("(", ver()));
                sincronizar(";");
                consumir();
            }
        }else{
            erros.add(new Erro("leia", ver()));
            sincronizar("(", ";");     
            if (ver().getLexema().equals("(")){
                consumir();
                escreva_params();
            }
            else
                consumir();
            //deixa passar                    
        }         
    }
    
    //<Id_Vetor><Leia_Param2>
    private void leia_params() throws EndTokensException {
        if(ver().getLexema().equals(")"))
            return;
        else{
            id_vetor();
            leia_params2();
        }
    }
    
    //<Leia_Param2> ::= ','<Leia_Params> | <>
    private void leia_params2() throws EndTokensException {
        if(ver().getLexema().equals(",")){
            consumir();
            leia_params();
        }     
    }
    
    //'se''('<Exp_Logica>')''entao'<Bloco><Senao>
    private void se() throws EndTokensException {
        if (ver().getLexema().equals("se")){
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                exp_logica();
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("entao")){
                        consumir();
                        bloco();
                        senao();
                    }else{
                        erros.add(new Erro("entao", ver()));
                        sincronizar(";", "inicio");
                        bloco();
                        senao();
                    }
                }else{
                    erros.add(new Erro(")", ver()));
                    sincronizar("entao", ";", "inicio");
                    if(ver().getLexema().equals(")")){
                        consumir();
                        if(ver().getLexema().equals("entao")){
                            consumir();
                            bloco();
                            senao();
                        }else{
                            erros.add(new Erro("entao", ver()));
                            sincronizar(";", "inicio");
                            bloco();
                            senao();
                        }
                    }

                }
            }
            else{
                erros.add(new Erro("(", ver()));
                sincronizar(")", "entao", ";", "inicio");
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("entao")){
                        consumir();
                        bloco();
                        senao();
                    }
                    else{
                        erros.add(new Erro("entao", ver()));
                        sincronizar(";", "inicio");
                        bloco();
                        senao();
                    }
                }
                    
            }
        }
    }
    
    //<Senao> ::= 'senao'<Bloco> | <>
    private void senao() throws EndTokensException {
        if(ver().getLexema().equals("senao")){
            consumir();
            bloco();
        }
            
    }
    
    //<Enquanto> ::= 'enquanto''('<Exp_Logica>')''faca'<Bloco>
    private void enquanto() throws EndTokensException{
        if(ver().getLexema().equals("enquanto")){
            consumir();
            if(ver().getLexema().equals("(")){
                consumir();
                exp_logica();
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("faca")){
                        consumir();
                        bloco();
                    }
                    else{
                        erros.add(new Erro("faca", ver()));
                        sincronizar(";", "inicio");
                        bloco();
                    }
                }
                else{
                    erros.add(new Erro(")", ver()));
                    sincronizar("faca", ";", "inicio");
                    if(ver().getLexema().equals("faca")){
                        consumir();
                        bloco();
                    }
                    else
                        bloco();
                }    
            }
            else{
                erros.add(new Erro("(", ver()));
                sincronizar(")", "faca", ";", "inicio");
                if(ver().getLexema().equals(")")){
                    consumir();
                    if(ver().getLexema().equals("faca")){
                        consumir();
                        bloco();
                    }else{
                        erros.add(new Erro("faca", ver()));
                        sincronizar(";", "inicio");
                        bloco();
                    }
                }
                else if(ver().getLexema().equals("faca")){
                    consumir();
                    bloco();
                }
                else
                    bloco();
                    
            }
        }
    }
    ////////////////////////    atribuicao    /////////////////////////////
    
    //<Id_Vetor>'<<'<Valor>';'
    private void atribuicao() throws EndTokensException{
        String id = ver().getLexema();
        isVetor = false;
        id_vetor();
        if(funcoesContainsKey(id)&&!isVetor&&!funcoesGet(id).getTipoRetorno().equals("vazio")){
            funcoesGet(id).setOk(true);
        }
        if(ver().getLexema().equals("<<")){
            consumir();
            valor();
            if(ver().getLexema().equals(";")){
                consumir();
            }else{
                //panico -;
                erros.add(new Erro(";", ver()));
                //nada, retorna pra camada superior (corpo_bloco)
            }
        }else{
            //panico -<<
            erros.add(new Erro("<<", ver()));
            sincronizar("<<", ";");
            if(ver().getLexema().equals("<<")){
                consumir();
                valor();
            }else
                consumir();
        }
    }
    
    ////////////////////////    funcao    /////////////////////////////
    
    //'funcao'<Funcao_Decl2>id'('<Param_Decl_List>')'<Bloco>
    private void funcao_decl() throws EndTokensException {
        if(ver().getLexema().equals("funcao")){
            consumir();
            funcao_decl2();
            if(ver().getTipo().equals("identificador")){
                funcao = new Funcao(ver().getLexema(), tipo, ver().getLinha());
                consumir();
                if(ver().getLexema().equals("(")){
                    consumir();
                    param_decl_list();
                    funcoes.add(funcao);
                    if(ver().getLexema().equals(")")){
                        consumir();
                        bloco();
                    }else{
                        //panico -)
                        erros.add(new Erro(")", ver()));
                        sincronizar(")", "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                                        "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                        if(ver().getLexema().equals(")"))
                            consumir();
                        bloco();
                    }
                }else{
                    //panico -(
                    erros.add(new Erro("(", ver()));
                    sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", "identificador",
                            "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                            "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                    if (ver().getLexema().equals("(")){
                        consumir();
                        param_decl_list();
                    }else if(igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia"))
                        param_decl_list();
                    else if (igual(ver().getTipo(),"identificador")){
                        consumir();
                        param_decl_list();
                    }else
                        bloco();
                }
            }else{
                //panico -id
                erros.add(new Erro("identificador", ver()));
                sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", "identificador",
                            "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                            "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
                if (ver().getLexema().equals("(")){
                    consumir();
                    param_decl_list();
                }else if(igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia"))
                    param_decl_list();
                else if (igual(ver().getTipo(),"identificador")){
                    consumir();
                    param_decl_list();
                }else
                    bloco();
            }
        }else{
            //panico -funcao
            erros.add(new Erro("funcao", ver()));
            sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", "identificador",
                            "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                            "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
            if (ver().getLexema().equals("(")){
                consumir();
                param_decl_list();
            }else if(igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia"))
                funcao_decl2();
            else if (igual(ver().getTipo(),"identificador")){
                consumir();
                param_decl_list();
            }else
                bloco();
        }
    }
    
    //<Tipo> | <>
    private void funcao_decl2() throws EndTokensException{
        if(isTipo()){
            tipo = ver().getLexema();
            consumir();
        }else
            tipo = "vazio";
    }
    
    //<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list() throws EndTokensException{
        if(igual(ver().getLexema(), "inicio", "var", 
                                "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(")"))//sem parametros
            return;
        if(isTipo()){
            parametro = ver().getLexema();
            consumir();
            id_vetor();//ve se é um vetor
            funcao.addParametros(parametro);
            param_decl_list2();
        }else{
            //panico -tipo
            erros.add(new Erro("tipo", ver()));
            sincronizar( "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                                "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");            
            //nada
        }
    }

    //','<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list2() throws EndTokensException{
        if(igual(ver().getLexema(), ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(",")){
            consumir();
            if(isTipo()){
                parametro = ver().getLexema();
                consumir();
                id_vetor();//ve se é um vetor
                funcao.addParametros(parametro);
                param_decl_list2();
            }else{
                //panico -tipo
                erros.add(new Erro(",", ver()));
                sincronizar(",", "inteiro", "real", "booleano", "caractere", "cadeia", ")", "inicio", "var", 
                                            "escreva", "leia", "se", "enquanto", "(", "fim", "funcao");
                if (igual(ver().getLexema(),",")){
                    param_decl_list2();
                }
            }
        }else{
            //panico -,
            erros.add(new Erro(",", ver()));
            sincronizar("inteiro", "real", "booleano", "caractere", "cadeia", ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao");
            if (igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia")){
                consumir();
                id_vetor();
                param_decl_list2();
            }
            //else nada
        }
    }
    
    //id'('<Param_Cham_List>')'
    private void chamada_funcao() throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            consumir();
        }else{
            //panico -tipo
            erros.add(new Erro("tipo", ver()));
        }
        if(ver().getLexema().equals("(")){
            consumir();
            param_cham_list();
        }else{
            //panico -(
            erros.add(new Erro("(", ver()));
            sincronizar("(", ")", "identificador", "numero", ",",";", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
            if(igual(ver().getLexema(), ",", "(")){
                consumir();
                param_cham_list();
            }
            //else nada
        }
        if(ver().getLexema().equals(")"))
            consumir();
        
    }
    
    //<Valor><Param_Cham_List2> | <>
    private void param_cham_list() throws EndTokensException{
        //System.out.println(ver().getLexema()+" param_cham_list");
        if(ver().getLexema().equals(")"))
            return;
        valor();
        //System.out.println(ver().getLexema()+" param_cham_list2");
        param_cham_list2();
    }
    
    //','<Valor><Param_Cham_List2> | <>
    private void param_cham_list2() throws EndTokensException{
        if(igual(ver().getLexema(), ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(",")){
            consumir();
            valor();
            param_cham_list2();            
        }else{
            //panico -,
            erros.add(new Erro(",", ver()));
            sincronizar("identificador", "numero", "caractere", "cadeia", ")", "inicio", "var", 
                                        "escreva", "leia", "se", "enquanto", "(", "fim", "funcao");
            if (igual(ver().getLexema(), "identificador", "numero", "caractere", "cadeia")){
                valor();
                param_cham_list2();
            }
            //else nada
        }
    }
    
    ////////////////////////    expressoes    /////////////////////////////
    
    //<Exp_Aritmetica2><Exp_SomaSub>
    private void exp_aritmetica() throws EndTokensException {
        exp_aritmetica2();
        exp_somasub();
    }
    
    //<Operador_A1><Exp_Aritmetica2><Exp_SomaSub> | <>
    private void exp_somasub() throws EndTokensException{
        if (igual(ver().getLexema(),"+", "-")){//<operador_a1> ::= + | -
            consumir();
            exp_aritmetica2();
            exp_somasub();
        }        
    }
    
    //<Valor_Numerico><Exp_MultDiv>
    private void exp_aritmetica2() throws EndTokensException{
        valor_numerico();
        exp_muldiv();
    }
    
    //<Operador_A2><Valor_Numerico><Exp_MultDiv> | <>
    private void exp_muldiv() throws EndTokensException{
        if (igual(ver().getLexema(),"/", "*")){//<operador_a2> ::= * | /
            consumir();
            valor_numerico();
            exp_muldiv();
        }
    }
    
    //'('<Exp_Aritmetica>')' | <Id_Vetor> | <Chamada_Funcao> | numero_t
    private void valor_numerico() throws EndTokensException{
        if(ver().getLexema().equals("(")){
            consumir();
            exp_aritmetica();
            if(ver().getLexema().equals(")")){
                consumir();
            }else{
                //panico -(
                erros.add(new Erro(")", ver()));
                //nada
            }
        }else if(ver().getTipo().equals("identificador")){
            if(verLLX(1).getLexema().equals("("))                
                chamada_funcao();
            else
                id_vetor();
        }else if (ver().getTipo().equals("numero"))
            consumir();
            
    }
    
    //<Exp_Logica2><Exp_Ou>
    private void exp_logica() throws EndTokensException {
        exp_logica2();
        exp_ou();
        if(igual(ver().getLexema(), "=", "<>")){
            consumir();
            exp_logica();
        }
    }
    
    //'ou'<Exp_Logica2><Exp_Ou> | <>
    private void exp_ou() throws EndTokensException{
        if(ver().getLexema().equals("ou")){
            consumir();
            exp_logica2();
            exp_ou();
        }
    }
    
    //<Exp_Nao><Exp_E>
    private void exp_logica2() throws EndTokensException{
        exp_nao();
        exp_e();
    }
    
    //'e'<Exp_Nao><Exp_E> | <>
    private void exp_e() throws EndTokensException{
        if(ver().getLexema().equals("e")){
            consumir();
            exp_nao();
            exp_e();
        }
    }
    
    //'nao'<Valor_Booleano> | <Valor_Booleano>
    private void exp_nao() throws EndTokensException{
        //System.out.println(ver().getLexema()+" nao");
        if(ver().getLexema().equals("nao")){
            consumir();
            valor_booleano();
        }else
            valor_booleano();
    }
    
    //booleano_t | <Exp_Relacional>
    private void valor_booleano() throws EndTokensException{
        if(igual(ver().getLexema(),"verdadeiro", "falso")){
            consumir();
        }else
            exp_relacional();
    }
    
    //<Exp_Aritm_Logica><Exp_Relacional2>
    private void exp_relacional() throws EndTokensException{
        //System.out.println(ver().getLexema()+" exp_relacional");
        exp_arim_logica();
        exp_relacional2();
    }
    
    //<Operador_Relacional><Exp_Aritm_Logica> | <>
    private void exp_relacional2() throws EndTokensException{
        //System.out.println(ver().getLexema()+" exp_relacional 2");
        if(igual(ver().getLexema(), "<", "<=", ">", ">=", "<>", "=")){
            consumir();
            exp_arim_logica();
        }
    }
    
    //<Exp_Aritm_Logica2><Exp_SomaSub_Logica>
    private void exp_arim_logica() throws EndTokensException{
        exp_arim_logica2();
        exp_somasub_logica();
    }
    
    //<Operador_A1><Exp_Aritm_Logica2><Exp_SomaSub_Logica> | <>
    private void exp_somasub_logica() throws EndTokensException{
        if(igual(ver().getLexema(), "+", "-")){
            consumir();
            exp_arim_logica2();
            exp_somasub_logica();
        }        
    }
        
    //<Numerico_Logico><Exp_MultDiv_Logica>
    private void exp_arim_logica2() throws EndTokensException{
        numero_logico();
        exp_multdiv_logica();
    }
    
    //<Operador_A2><Numerico_Logico><Exp_MultDiv_Logica> | <>
    private void exp_multdiv_logica() throws EndTokensException{
        if(igual(ver().getLexema(), "/", "*")){
            consumir();
            numero_logico();
            exp_multdiv_logica();
        }
    }
    
    //'('<Exp_Aritm_Logica>')' | <Id_Vetor> | <Chamada_Funcao> | numero_t
    private void numero_logico() throws EndTokensException{
        //System.out.println(ver().getLexema()+" numero_logico");
        if(ver().getLexema().equals("(")){
            consumir();
            if (igual(verLLX(1).getLexema(),"+", "-", "/", "*"))
                exp_arim_logica();
            else
                exp_logica();
            //System.out.println(ver().getLexema()+" -------------------------------------------");
            if(ver().getLexema().equals(")")){
                consumir();
            }else{
                //panico-)
                erros.add(new Erro(")", ver()));
                //nada
            }
            exp_logica();
        }else if(ver().getTipo().equals("identificador")){
            if(verLLX(1).getLexema().equals("("))
                chamada_funcao();                
            else
                id_vetor();
        }else if(ver().getTipo().equals("numero")){
            consumir();
        }else if (igual(ver().getLexema(),"verdadeiro", "falso")){
            valor_booleano();
        }
    }
    ////////////////////////    outros    /////////////////////////////
    
    // inteiro | real | booleano | cadeia | caractere
    private boolean isTipo() throws EndTokensException{
        return igual(ver().getLexema(), "inteiro", "real", "booleano", "caractere", "cadeia");
    }
    // caractere | cadeia | numero | verdadeiro | falso
    private boolean isLiteral() throws EndTokensException {
        return igual(ver().getTipo(), "caractere", "cadeia", "numero")|| igual(ver().getLexema(), "verdadeiro", "falso");
    }
    
    //vetor definido em variaveis
    
    //<Exp_Logica> | caractere_t | cadeia_t
    private void valor() throws EndTokensException{
        //System.out.println(ver().getLexema()+" valor");
        if (igual(ver().getTipo(),"caractere", "cadeia"))
            consumir();
        else
            exp_logica();
    }
    //id<Vetor>
    private void id_vetor() throws EndTokensException{        
        if(ver().getTipo().equals("identificador")){
            consumir();
            vetor();
        }else{
            //panico -id
            erros.add(new Erro("identificador", ver()));
            sincronizar("<<<", ";",",", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                    "escreva", "leia", "se", "enquanto", "identificador", "(", "fim", "funcao");
            if (ver().getLexema().equals("<<<")){
                consumir();
                vetor();
            }
        }
    }

    private boolean funcoesContainsKey(String id) {
        for(Funcao funcao: funcoes){
            if (funcao.getNome().equals(id))
                return true;
        }
        return false;
    }

    private Funcao funcoesGet(String id) {
        Funcao r = null;
        for(Funcao funcao: funcoes){
            if (funcao.getNome().equals(id))
                r = funcao;
        }
        return r;
    }

    public boolean isOk() {
        return ok;
    }
    
    
}
