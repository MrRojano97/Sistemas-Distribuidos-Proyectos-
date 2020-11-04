import java.util.ArrayList;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase que controla la ejecucion del programa, contiene toda las opciones del usuario y ejecuta
 * los algoritmos de procesamiento segun estas opciones.
 */
public class ImageProcessor {

    private MatrixImage image;              //Matriz generada desde la imagen
    private AtomicInteger matrixImage[][];  //Matriz de imagen generada
    private String tech_selected;           //tecnica seleccionada por el usuario
    private int nstruct_selected;           //estructura seleccionada por el usuario
    /*Arreglo de estructuras disponibles:
    - Son los valores de ubicacion de la esctructura dentro de la matriz, en el formato (i1,j1,i2,j2,i3,j3,...).
    - Por ej para la estructura {0,0,0,1,1,0} se tienen 6 valores, por lo que es una estructura de 3 pixeles (3 pares).
        - pixel principal siempre es el primer par: (0,0) en este caso y es el pixel que se compara con el resto
        - segundo pixel (0,1), tercer pixel (1,0)
        - este crea la figura: |_|_|
                               |_|
    - El arreglo tiene 5 listas correspondientes a las 5 estructuras requeridas, para mas informacion consulte el archivo
    de estructuras.png
    */
    private final Integer[][] structs = {{1,1,0,1,1,0,1,2,2,1},{0,0,0,1,1,0}, {1,0,0,0,1,1},{1,0,0,0,2,0},
            {0,0,0,1},{1,1,0,0,0,2,2,0,2,2}};
    // Estructura base usada en el ejemplo

    //Objeto para ejecutar un proceso concurrente (dilatacion o erosion)
    private ConcurrentProcessImage[] concurrentProcess;
    private int numThreads;                 //numero de threads que se crean PD: NUNCA DEJAR EN 0 O SE HARA UN BUCLE!!!
    private Integer[][] imageTemp;          //Matriz copia auxiliar

    public ImageProcessor(String tech_selected, int struct_selected, MatrixImage imagen, int numThreads) throws InterruptedException {
        System.out.print("Iniciando procesador de imagenes..");
        this.tech_selected = tech_selected;
        this.nstruct_selected = struct_selected;
        this.numThreads = numThreads;
        this.image = imagen;

        //matrixImage es la Matriz de AtomicInteger del PGMFile
        matrixImage = imagen.getMatrix();

        runTech();

    }

    /**
     * Metodo que ejecuta una tecnica de procesamiento
     */
    public void runTech(){
        int rowsMatrix = image.getRows();       //cantidad de filas de la matriz
        int colMatrix=  image.getColumns();     //cantidad de columnas de la matriz
        int row_assigned;                       //fila asignada a cada estructura para recorrerla
        int id_struct;                          //id de cada estructura para diferenciarlas
        int threads_no_creates=0;

        //Crear matriz auxiliar para consultar valores (no se actualiza)
        imageTemp=new Integer[rowsMatrix][colMatrix];
        for (int i = 0; i < rowsMatrix; i++) {
            for (int j = 0; j < colMatrix; j++) {
                imageTemp[i][j]=matrixImage[i][j].get();
            }
        }

        this.numThreads = rowsMatrix;         //NUmero de hilos a ejecutar segun la cantidad de filas
        //this.numThreads = 2;                // cantidad de hilos de prueba, eliminar o comentar en entrega definitiva
        System.out.println("cantidad filas: "+rowsMatrix);
        System.out.println("tecnica de procesamiento elegida: "+tech_selected);
        concurrentProcess = new ConcurrentProcessImage[numThreads]; //Crea una instancia de ejecucion paralela

        //Comienzo el ciclo para crear hilos y estructuras.
        for (int i = 0; i < numThreads ; i++) {

            //Crear una escructura elegida por el usuario
            id_struct = i+1;
            Structure struct_selected = new Structure(structs[nstruct_selected],id_struct);

            //fila asignada para la estructura.
            row_assigned = i;

            // ubica la estructura en la fila requerida.
            struct_selected.setStartPosition(row_assigned);

            // si la estructura tiene coordenadas validas, entonces crea un hilo de ejecucion
            if(struct_selected.isStructValid(rowsMatrix,colMatrix)){
                concurrentProcess[i] = new ConcurrentProcessImage(tech_selected, imageTemp,
                        struct_selected,row_assigned,matrixImage,colMatrix);
                concurrentProcess[i].start();   //iniciar hilo
            }
            else{
                //contador de ayuda para saber cuantos hilos se omitieron.
                threads_no_creates ++;
            }
        }
        //Join del hilo principal
        for (int i = 0; i < numThreads - threads_no_creates ; i++) {
            try {
                concurrentProcess[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
