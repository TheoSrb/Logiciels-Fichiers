import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static List<String> allDatas = new ArrayList<>();

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("\n\n\n ---------- Autorisations de Partages ---------- \n\n\n");

        System.out.println("Quel dossier voulez-vous inspecter ?");
        String pathInput = input.nextLine();

        System.out.println("\nIndiquez le chemin du fichier à créer:");
        String filePath = input.nextLine();

        String fileInput = filePath + "\\" + "Autorisations_Partages_" +
                LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue() + "-" + LocalDate.now().getDayOfMonth() + "-" +
                LocalTime.now().getHour() + "-" +  LocalTime.now().getMinute() + "-"  + LocalTime.now().getSecond() + ".txt";


        Path pathFile = Path.of(pathInput);
        File file = new File(pathFile.toString());

        System.out.println("\n\nCréation du fichier en cours...\n\n");

        for (File f : file.listFiles()) {
            String command = "net share \"" + f.getName() + "\""; // La commande actuelle qu'on veut exécuter.
            try {
                executeAndCatchCMDCommand(command, f); // On affiche le résultat de notre commande en évitant les erreurs.
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();    // On attrape l'erreur s'il y en a une.
            }
        }
        allDatas = allDatas.stream()
                .map(s -> s.replace('ô', 'o')
                        .replace('é', 'e')
                        .replace('è', 'e')
                        .replace('ê', 'e')
                        .replace('à', 'a')
                        .replace('â', 'a'))
                .toList();

        createFileWithDatas(fileInput);

        System.exit(0);
    }

    private static void createFileWithDatas(String fileInput) {
        File file = new File(fileInput);

        try {
            if (!file.exists()) file.createNewFile();
            BufferedWriter buffer = new BufferedWriter(new FileWriter(file));

            for (String line : allDatas) {
                buffer.write(line + "\n");
            }

            buffer.close();

            System.out.println("\nLe fichier a bien été créé avec succès au chemin suivant: \n-> " + fileInput);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO Fonction qui permet d'exécuter une commande dans le CMD et de récupérer le texte de sortie de la commande, son résultat.
    private static StringBuilder executeAndCatchCMDCommand(String command, File f) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("cmd /c " + command); // On exécute la commande dans le CMD.
        StringBuilder output = new StringBuilder(); // On crée une variable qui va stocker le résultat de cette dernière.
        BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream())); // On lit la sortie de la commande.

        String line = buffer.readLine();    // On stocke la ligne actuelle de la sortie de la commande.

        while (line != null) {  // On parcourt chaque ligne de la sortie de la commande et on l'ajoute au StringBuilder.
            if (line.contains("Autorisation") || line.contains("Remarque") || line.contains("CHANGE") || line.contains("READ") || line.contains("FULL"))
                allDatas.add(f + (line.contains("Remarque") ? ",Remarque: " : "") + "," + line.trim()
                        .replaceAll("Autorisation", "")
                        .replaceAll("Remarque", "")
                        .replaceAll(" {14}", "")
                        .replaceAll("READ", "Lecture")
                        .replaceAll("CHANGE", "Modification")
                        .replaceAll("FULL", "Controle total")
                );

            line = buffer.readLine();
            output.append(line + "\n");
        }

        buffer.close();

        return output;  // On renvoie ce que la sortie de la commande donne.
    }
}