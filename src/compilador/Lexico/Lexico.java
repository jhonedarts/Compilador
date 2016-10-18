/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.Lexico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 *
 * @author jmalmeida
 */
public class Lexico {
    private String numeros;
    private String delimitadores;
    private String delimitadoresEspeciais;
    private String delimitadoresUnitarios;
    private String tabulacao;
    private String arqSaida;

    public Lexico() {
        numeros = ".0123456789";
        delimitadores = "-+*/=( );,<>\"'{\r\t\n\0";//todos eles
        delimitadoresEspeciais = "<>";//podem ter complementos
        delimitadoresUnitarios = "+*/=();,";//tokens unitarios em qualquer situação
        tabulacao = "\r\t\n";
        arqSaida = "saida/";
    }
    
    public LinkedList<Token> start(File fileIN) throws IOException{
        LinkedList<Token> tokensList = new LinkedList();
        //System.out.println("----------------- Analise Lexica -----------------\n");
        //System.out.println("File: " + fileIN.getName());
        arqSaida = arqSaida.concat(fileIN.getName());

        TokenTipo analisador = new TokenTipo();
        //leitor
        BufferedReader reader = new BufferedReader(new FileReader(fileIN));
        String line;
        int linha = 0;//linha 
        int v =0;//variacao da coluna
        boolean comentario = false;
        String lexema =null;
        while ((line = reader.readLine()) != null){//ler todas linhas
            //e para cada linha separar os tokens
            linha++;
            char a ;
            if(!comentario)//nao reseto o valor ate fechar o comentario
                lexema = null;

            //System.out.println("linha:\n"+line);
            //System.out.println(line.length());
            //separação dos tokens da linha
            for (int i=0; i<line.length(); i++){
                if (comentario){  
                    //System.out.println(i+" coment");
                    if (line.charAt(i)=='}'){                        
                        comentario = false;
                        lexema = lexema.concat(line.charAt(i)+"");                        
                        //System.out.println(lexema);
                        Token token = analisador.analise(lexema);
                        token.setColuna(v);
                        token.setLinha(linha);
                        tokensList.add(token);
                    } else 
                        lexema = lexema.concat(line.charAt(i)+"");
                }else{  
                    v=0;
                    a = line.charAt(i);//apenas para inicio de lexema
                    lexema = "";//reinicia a string lexema
                    // delimitadores unitários
                    if (delimitadoresUnitarios.contains(a+"")){
                        lexema = a+"";
                        //System.out.println(i+"unitario");
                    }

                    else if(delimitadoresEspeciais.contains(a+"")){//sao binarios
                        lexema = a+"";
                        try{
                            if( (a == '<' && (line.charAt(i+1) == '>' || line.charAt(i+1) == '=')) ||
                                    (a == '>' && (line.charAt(i+1) == '=' )) ){
                                i++; v++;
                                lexema = lexema.concat(line.charAt(i)+"");

                            }
                            if (a == '<' && (line.charAt(i+1) == '<') ){                                        
                                i++; v++;
                                lexema = lexema.concat(line.charAt(i)+"");
                                if(line.charAt(i+1) == '<'){
                                    i++; v++;
                                    lexema = lexema.concat(line.charAt(i)+"");
                                }
                            }
                            if(a == '>' && (line.charAt(i+1) == '>') && (line.charAt(i+2) == '>')){
                                i+=2; v+=2;
                                lexema = lexema.concat(line.charAt(i-1)+""+line.charAt(i));                                        
                            }
                            //System.out.println(i+"<>");
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada

                        }
                    }else if (a == '-'){//numeros negativos ou operador -
                        lexema = a+"";
                        try{
                            if (numeros.contains(line.charAt(i+1)+"")){
                                while(!delimitadores.contains(line.charAt(i+1)+"")){   
                                    i++; v++;
                                   lexema = lexema.concat(line.charAt(i)+""); 
                                   //System.out.println(i+"-");
                                }          
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada

                        }
                    //caractere
                    }else if (a == '\''){
                        lexema = a+"";
                        //System.out.println(i+"'");
                        try {
                            while(!(line.charAt(i+1) == '\'' || line.charAt(i+1) == ' ' || tabulacao.contains(line.charAt(i+1)+""))){   
                                i++; v++;
                               lexema = lexema.concat(line.charAt(i)+"");    
                               //System.out.println(i+"'");
                            }
                            if (line.charAt(i+1) == '\''){
                                i++; v++;
                               lexema = lexema.concat(line.charAt(i)+"");
                               //System.out.println(i+"'");
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de ' ser a ultima coisa do arquivo

                        }
                    //cadeia de caractere  
                    }else if (a == '"'){
                        lexema = a+"";
                        //System.out.println(i+"\"");
                        try{
                            while(!(line.charAt(i+1) == '"' || tabulacao.contains(line.charAt(i+1)+""))){   
                                i++; v++;
                                //System.out.println(i+"\"");
                               lexema = lexema.concat(line.charAt(i)+"");                                              
                            }
                            if (line.charAt(i+1) == '"'){
                                i++; v++;
                                //System.out.println(i+"\"");
                               lexema = lexema.concat(line.charAt(i)+"");
                            }
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada

                        }
                    //comentarios
                    }else if (a=='{'){
                        lexema = a+"";
                        comentario = true;
                        v=i;//para usar v como coluna da posicao do {
                    }else if (a=='}'){//caractere perdido
                        lexema = a+"";
                    }else if(tabulacao.contains(a+"") || a==' '){
                        //System.out.println(i+" space");
                        lexema = null;//so pra nao entra na formação do token, sera reiniciado no inicio do for
                    }
                    //qualquer coisa que vier
                    else {
                        //concatene ate um delimitador

                        lexema = a+"";
                        try{
                            while(!delimitadores.contains(line.charAt(i+1)+"")){   
                                i++; v++;
                               lexema = lexema.concat(line.charAt(i)+"");                                              
                            } 
                        }catch(StringIndexOutOfBoundsException ex){//pro caso de " nao ser fechada

                        }

                    }

                    //lexema formado se nao for comentario ou tabulacao
                    if (!comentario && lexema!=null){
                        //System.out.println(lexema);
                        Token token = analisador.analise(lexema);
                        //System.out.println(" token formado: "+lexema);                        
                        token.setColuna(i-v);
                        token.setLinha(linha);
                        tokensList.add(token);
                    }
                }
            }
        }

        //gravar no txt
        arqSaida = arqSaida.split(".txt")[0];
        File file = new File(arqSaida+"_lexico.txt");

        if (!file.exists()){
            new File(arqSaida+"_lexico.txt").createNewFile();
            file = new File(arqSaida+"_lexico.txt");
        }

        PrintWriter gravarArq = new PrintWriter(new FileWriter(file));

        LinkedList<Token> errosList = new LinkedList();
        //escrevendo no txt
        //gravarArq.printf("Tabela de Simbolos:%n%nn# | lexema - tipo | linha%n");   
        ////System.out.println("Tabela de Simbolos:\n\nn# | lexema - tipo | linha");
        for(int i=0; i<tokensList.size();i++){
            Token t= tokensList.get(i);
            if(t.isValido()){
                gravarArq.printf(t.getLinha()+" "+t.getLexema()+" "+t.getTipo()+"%n");
                //System.out.println(t.getLinha()+" "+t.getLexema()+" "+t.getTipo());
            }else
                errosList.add(t);
        }        
        //gravarArq.printf("%n%nTabela de Erros:%n%nn# | lexema | tipo | linha%n");
        gravarArq.printf("%n");
        ////System.out.println("\n\nTabela de Erros:\n\nn# | lexema | tipo | linha");
        //System.out.println();
        for(int i=0; i<errosList.size();i++){
            Token t= errosList.get(i);
            gravarArq.printf(t.getLinha()+" "+t.getLexema()+" "+t.getTipo()+" "+"%n");
            //System.out.println(t.getLinha()+" "+t.getLexema()+" "+t.getTipo());
        }
        gravarArq.close();
         
        
        return tokensList;
    }
    
}
