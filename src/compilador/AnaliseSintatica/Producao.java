/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.AnaliseSintatica;

/**
 *
 * @author Mt Castro
 */
public class Producao {
    private String producao;
    private boolean isTerminal;

    public Producao(String producao, boolean isTerminal) {
        this.producao = producao;
        this.isTerminal = isTerminal;
    }

    public String getProducao() {
        return producao;
    }

    public boolean isIsTerminal() {
        return isTerminal;
    }  
}
