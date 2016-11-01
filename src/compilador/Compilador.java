/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador;

import compilador.lexico.Lexico;
import compilador.lexico.Token;
import compilador.semantico.Semantico;
import compilador.sintatico.Funcao;
import compilador.sintatico.Sintatico;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author jmalmeida
 */
public class Compilador {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        LinkedList<Token> tokensList = new LinkedList();
        LinkedList<Funcao> funcoesList;
        Lexico lexico = new Lexico();
        Sintatico sintatico = new Sintatico();
        Semantico semantico = new Semantico();
        
        File folder = new File("entrada/");
        File[] listOfFiles = folder.listFiles();

        for (int y = 0; y < listOfFiles.length; y++) {
            if (listOfFiles[y].isFile()){
                tokensList =  lexico.start(listOfFiles[y]);
                if(lexico.isOk()){
                    //chamar o sintatico passando tokensList como parametro
                    funcoesList = sintatico.start(tokensList, listOfFiles[y].getName());
                    if(sintatico.isOk())
                        //chamar o semantico, passando a tabela de simbolos das funcoes
                        semantico.start(tokensList, funcoesList, listOfFiles[y].getName());
                }
            }
        }
    }
    
}
