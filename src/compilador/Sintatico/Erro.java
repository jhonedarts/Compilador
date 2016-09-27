/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.Sintatico;

/**
 *
 * @author jmalmeida
 */
public class Erro {
    private String esperado;
    private int linha;

    public Erro(String esperado, int linha) {
        this.esperado = esperado;
        this.linha = linha;
    }

    public String getEsperado() {
        return esperado;
    }

    public int getLinha() {
        return linha;
    }    
    
}
