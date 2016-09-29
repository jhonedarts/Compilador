/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador;

import compilador.Lexico.Lexico;
import compilador.Lexico.Token;
import compilador.Sintatico.Sintatico;
import java.io.File;
import java.io.IOException;
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
        Lexico lexico = new Lexico();
        Sintatico sintatico = new Sintatico();
        
        File folder = new File("entrada/");
        File[] listOfFiles = folder.listFiles();

        for (int y = 0; y < listOfFiles.length; y++) {
            if (listOfFiles[y].isFile())
                tokensList =  lexico.start(listOfFiles[y]);
            //chamar o sintatico passando tokensList como parametro
            sintatico.start(tokensList, listOfFiles[y].getName());
            //chamar o semantico, em um futuro nao muito distante
        }
    }
    
}
