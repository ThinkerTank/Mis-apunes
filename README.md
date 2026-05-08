package model;
public class Circulo extends Figura {
    //atributes
    private int radio;

    //metodos
    public Circulo(int r){
        radio=r;
    }

    @Override 
    public double area(){
        double resultado=radio*radio*Math.PI;
        return resultado;
    }
    
    @Override 
    public String toString(){
       return "El circulo con radio igual a:"+radio;
    }

}
