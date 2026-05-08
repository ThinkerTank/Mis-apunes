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

package model;
import java.util.ArrayList;
public class Controller{
    
    //relations
    private ArrayList<Figura> listaFiguras;

    //methods
    public Controller(){
        listaFiguras=new ArrayList<Figura>();
    }

    public boolean addCuadrado(int l){
         Cuadrado obj=new Cuadrado(l);
         listaFiguras.add(obj);
         return true;
    }

    public boolean addCirculo(int r){
         Circulo obj= new Circulo(r);
         listaFiguras.add(obj);
         return true;
    }

    public String mostrarAreas(){
        String mensaje="Las areas de las figuras son:";
        for(int i=0; i<listaFiguras.size();i++){
            Figura obj=listaFiguras.get(i);
            mensaje += "\n"+obj.toString()+" y el area es:"+obj.area();
        }
        return mensaje;
    }

    public String mostrarSumaCuadrados(){
        String mensaje="\n \n LA SUMA DE LOS CUADRADOS :";
        int sumaLado=0;
        for(int i=0; i<listaFiguras.size();i++){
            Figura obj=listaFiguras.get(i);
            if (obj instanceof Cuadrado){
                 Cuadrado objCuad=(Cuadrado)obj;
                 sumaLado+= objCuad.getLado();
            }
        }
        mensaje += sumaLado;
        return mensaje;
    }

    
}


