/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.semantico;

import compilador.lexico.Token;
import compilador.sintatico.EndTokensException;
import compilador.sintatico.Funcao;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Jhone e Matheus Castro
 */
public class Semantico {
    private LinkedList<Token> tokens;
    private int atual;
    
    //variaveis usadas no Sematico 
    private String tipo;
    private Funcao funcao;
    private String parametro;
    private HashMap<String, Simbolo> simbolos;
    private LinkedList<Erro> erros;
    private HashMap<String, Funcao> funcoes;

    public Semantico() {
        
    }
 
    public void start(LinkedList<Token> tokens, HashMap<String, Funcao> funcoes, String nomeArq) throws IOException{
        this.tokens = new LinkedList<Token>();
        this.erros = new LinkedList<Erro>();
        this.simbolos = new HashMap<String, Simbolo>();
        this.funcoes = funcoes;
        tipo =null;
        funcao =null;
        parametro = null;
        nomeArq = nomeArq.split(".txt")[0];
        atual=0;
        this.tokens.addAll(tokens);        
        //tirando os comentarios
        for(Token tok: tokens){
            if(tok.getTipo().equals("comentario"))
                this.tokens.remove(tok);
        }
        
        System.out.println("----------------- Analise Semantica -----------------\nArquivo: "+nomeArq+"\n");
        programa();
        if (erros.isEmpty())
            System.out.println("Sucesso!");
        else{
            for(Erro error: erros){
                System.out.println(error.getLinha()+" "+error.getMensagem());               
            }
        }
        
        //gravar no txt
        File file = new File("saida/"+nomeArq+"_semantico.txt");

        if (!file.exists()){
            new File("saida/"+nomeArq+"_semantico.txt").createNewFile();
            file = new File("saida/"+nomeArq+"_semantico.txt");
        }

        PrintWriter gravarArq = new PrintWriter(new FileWriter(file));
        if (erros.isEmpty())
            gravarArq.printf("Sucesso!%n");
        else{
            for(Erro error: erros){
                gravarArq.printf(error.getLinha()+" "+error.getMensagem()+"%n");
            }
        }
        gravarArq.close();
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
                }              
                if (ver().getLexema().equals("fim")){
                    consumir();
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
            tipo = ver().getLexema();
            consumir();
            R();
            varlist();
        }
        
    }
    
    //id <vetor> <R2>
    private void R() throws EndTokensException {
        if (ver().getTipo().equals("identificador")){
            String id = ver().getLexema();
            consumir();
            vetor();
            if(simbolos.get(id)==null){//nao declarado
                simbolos.put(id, new Simbolo(ver(), tipo, 0));
            }else{//já declarado
                erros.add(new Erro("Variavel "+id+" ja declarada", ver().getLinha()));
            }
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
        } 
    }
    //'<<<'<Exp_Aritmetica><vetor2>'>>>' | <>
    private void vetor() throws EndTokensException {
        if(ver().getLexema().equals("<<<")){
            tipo = tipo+"Vetor";
            parametro = parametro+"Vetor";
            consumir();
            exp_aritmetica(); // a fazer   
            vetor2();
            if(ver().getLexema().equals(">>>")){
                consumir();                
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
                }               
                if (ver().getLexema().equals("fim")){
                    consumir();
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
            tipo = ver().getLexema();
            consumir();
            const_decl();
            constlist();
        }       
    }
    
    //id'<<'<Literal><Const_Decl2>
    private void const_decl() throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            String id = ver().getLexema();
            if(simbolos.get(id)==null){//nao declarado
                simbolos.put(id, new Simbolo(ver(), tipo, 0));//zero = global
            }else{//já declarado
                erros.add(new Erro("Variavel "+id+" ja declarada", ver().getLinha()));
            }
            consumir();  
            if(ver().getLexema().equals("<<")){
                consumir(); if(isLiteral()){
                    if(!tipo.equals(checarTipo())){
                        erros.add(new Erro("Tipos incompativeis, "+tipo+" e "+ver().getLexema(), ver().getLinha()));
                    }
                    consumir();
                    const_decl2();
                }
            }          
        }
    }
    // ','id'<<'<Literal><Const_Decl2> | ';'
    private void const_decl2() throws EndTokensException{
        if (ver().getLexema().equals(",")){
            consumir();
            if(ver().getTipo().equals("identificador")){
                if(simbolos.get(ver().getLexema())==null){//nao declarado
                    simbolos.put(ver().getLexema(), new Simbolo(ver(), tipo, 0));
                }else{//já declarado
                    erros.add(new Erro("Variavel "+ver().getLexema()+" ja declarada", ver().getLinha()));
                }
                consumir();
                if (ver().getLexema().equals("<<")){
                    consumir();     
                    if(isLiteral()){
                        
                        consumir();
                        const_decl2();
                    }
                }
            }            
        }else if(ver().getLexema().equals(";")){
            consumir();
        }
    }
    
    ////////////////////////    Bloco      /////////////////////////////
    
    //'inicio'<Corpo_Bloco>'fim'
    private void bloco() throws EndTokensException{
        if(ver().getLexema().equals("inicio")){
            consumir();
            corpo_bloco();
        }
        if(ver().getLexema().equals("fim")){
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
        }
    }
    ////////////////////////    comandos    /////////////////////////////
    
    //<Escreva> | <Leia> | <Se> | <Enquanto>
    private void comando() throws EndTokensException{              
        if (ver().getLexema().equals("escreva")){
            escreva();
        }else if(ver().getLexema().equals("leia")){
            leia();
        }else if (ver().getLexema().equals("se")){
            se();
        }else if (ver().getLexema().equals("enquanto")){
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
                
                if(ver().getLexema().equals(";"))
                    consumir();
                
            }
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
                
                if(ver().getLexema().equals(";"))
                    consumir();
                
            }
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
                }    
            }
        }
    }
    ////////////////////////    atribuicao    /////////////////////////////
    
    //<Id_Vetor>'<<'<Valor>';'
    private void atribuicao() throws EndTokensException{
        id_vetor();
        if(ver().getLexema().equals("<<")){
            consumir();
            valor();
            if(ver().getLexema().equals(";")){
                consumir();
            }
        }
    }
    
    ////////////////////////    funcao    /////////////////////////////
    
    //'funcao'<Funcao_Decl2>id'('<Param_Decl_List>')'<Bloco>
    private void funcao_decl() throws EndTokensException {
        if(ver().getLexema().equals("funcao")){
            consumir();
            funcao_decl2();
            if(ver().getTipo().equals("identificador")){
                consumir();
                if(ver().getLexema().equals("(")){
                    consumir();
                    param_decl_list();
                    if(ver().getLexema().equals(")")){
                        consumir();
                        bloco();
                    }
                }
            }
        }
    }
    
    //<Tipo> | <>
    private void funcao_decl2() throws EndTokensException{
        if(isTipo()){
            consumir();
        }
    }
    
    //<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list() throws EndTokensException{
        if(igual(ver().getLexema(), "inicio", "var", 
                                "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(ver().getLexema().equals(")"))//sem parametros
            return;
        if(isTipo()){
            consumir();
            id_vetor();//ve se é um vetor
            param_decl_list2();
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
                consumir();
                id_vetor();//ve se é um vetor
                param_decl_list2();
            }
        }
    }
    
    //id'('<Param_Cham_List>')'
    private void chamada_funcao() throws EndTokensException{
        if(ver().getTipo().equals("identificador")){
            consumir();
        }
        if(ver().getLexema().equals("(")){
            consumir();
            param_cham_list();
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
    
    //usada para fins semanticos
    private String checarTipo() throws EndTokensException{
        String vTip = ver().getTipo();
        String vLex = ver().getLexema();
        String tipo = vTip;
        
        if(vTip.equals("numero")){
            if(vLex.contains(".")){
                tipo = "real";
            }else{
                tipo = "inteiro";
            }
        }else if(igual(vLex, "verdadeiro", "falso")){
            tipo = "booleano";
        }            
        return tipo;
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
        }
    }
}