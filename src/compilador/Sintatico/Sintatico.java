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
                bloco();
                consumir();
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
            System.out.println(ver().getLexema());// so pra checar se o arquivo acabou, se sim, dara a exception
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
            consumir();
            exp_aritimetica(); // a fazer   
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
            exp_aritimetica();
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
    private void comando(){
        
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
                consumir();
                if(ver().getLexema().equals("(")){
                    consumir();
                    param_decl_list();
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
        if(isTipo())
            consumir();
    }
    
    //<Tipo><Id_Vetor><Param_Decl_List2> | <>
    private void param_decl_list() throws EndTokensException{
        if(igual(ver().getLexema(), "inicio", "var","inteiro", "real", "booleano", "caractere", "cadeia", 
                                "escreva", "leia", "se", "enquanto", "(", "fim", "funcao"))
            return;
        if(isTipo()){
            consumir();
            id_vetor();
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
                consumir();
                id_vetor();
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
        
    }
    
    //<Valor><Param_Cham_List2> | <>
    private void param_cham_list() throws EndTokensException{
        if(ver().getLexema().equals(")"))
            return;
        valor();
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
    
    private void exp_aritimetica() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private void exp_logica() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
