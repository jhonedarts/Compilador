/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compilador.sintatico;

import java.util.LinkedList;

/**
 *
 * @author jhone
 */
public class Funcao {
    private String nome;
    private LinkedList<String> parametros;
    private String tipoRetorno;

    public Funcao(String nome, String tipoRetorno) {
        this.nome = nome;
        this.parametros = new LinkedList<String>();
        this.tipoRetorno = tipoRetorno;
    }

    public String getNome() {
        return nome;
    }

    public LinkedList<String> getParametros() {
        return parametros;
    }

    public void addParametros(String parametro) {
        this.parametros.add(parametro);
    }

    public String getTipoRetorno() {
        return tipoRetorno;
    }
    
    
}
