/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.AnaliseSintatica;
import compilador.AnaliseLexica.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Stack;

/**
 *
 * @author Mt Castro
 */
public class Sintatico {
    private File outArq;
    private LinkedList tableTokens;
    private Stack pilha;

    public Sintatico(File outArq, LinkedList tableTokens) {
        this.outArq = outArq;
        this.tableTokens = tableTokens;
        this.pilha = new Stack();
        
        Token t = new Token("$", 0, true);
        this.tableTokens.add(t);
        
        Producao p = new Producao("Programa", false);
        Producao p2 = new Producao("$", true);
        this.pilha.add(p2);
        this.pilha.add(p);
    }
 
    public void start(){
        
    }
}
