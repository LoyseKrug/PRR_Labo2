import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LamportStarter {

    public static void main(String[] args){

        // console version
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        String line = "";

        try {
            System.out.println("Please enter a number of Lamport instances you desire.");
            line = br.readLine();

            int nb = Integer.parseInt(line);

            ILamport[] lamports = new ILamport[nb];

            for(int i = 0; i < nb; ++i) {// init Lamport
                System.out.println("Lamport server " + i + " is starting ...");

                lamports[i] = new Lamport(nb, i);

                System.out.println("Lamport server " + i + " is binded!");
            }

            for(int i = 0; i < nb; ++i){
                lamports[i].Init();
                System.out.println("Lamport server " + i + " is ready!");
            }

            System.out.println("Type 'exit' to quit");

            while ((line = br.readLine()) != null && !line.equals("exit") ){
                System.out.println("Type 'exit' to quit");
            }

            isr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
