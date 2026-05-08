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

package model;
public class Cuadrado extends Figura {
    //atributos
    private int lado;

    //metodos
    public Cuadrado (int l){
        lado=l;
    }

    public int getLado(){
        return lado;
    }

    @Override
    public double area(){
        return (lado*lado);
    }

    @Override 
    public String toString(){
       return "El cuadrado con lado igual a:"+lado;
    }
}


package model;
public abstract class Figura {

    // metodos
    public Figura(){

    }

    public double area(){
        return 0;
    }

}






package ui;
import model.Controller;
import java.util.Scanner;
public class FigurApp{
    
    private Controller miControl;
    private Scanner lector;

    //methods
    public FigurApp(){
        miControl=new Controller();
        lector=new Scanner(System.in);
    }

    public static void main(String[] args){
         FigurApp obj=new FigurApp();
         obj.addCirculo();
         obj.addCuadrado();
         obj.addCuadrado();
         obj.mostrar();
         obj.mostrarSumaCuadrados();
    }

    public void addCirculo(){
        System.out.println("Digite el radio:");
        int radio=lector.nextInt();
        lector.nextLine();
        miControl.addCirculo(radio);
    }

    public void addCuadrado(){
        System.out.println("Digite el lado del Cuadrado:");
        int lado=lector.nextInt();
        lector.nextLine();
        miControl.addCuadrado(lado);
    }

    public void mostrar(){
        String mensaje=miControl.mostrarAreas();
        System.out.println(mensaje);
    }

    public void mostrarSumaCuadrados(){
        String mensaje=miControl.mostrarSumaCuadrados();
        System.out.println(mensaje);
    }
}


