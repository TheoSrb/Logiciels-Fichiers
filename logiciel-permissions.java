import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static List<String> allDatas = new ArrayList<>();    // Création d'une liste actuellement vide qui contiendra l'intégralité des données plus tard.

    private static final List<AclEntryPermission> readPermissions = List.of(     // Liste contenant toutes
            AclEntryPermission.READ_DATA,                                        // les permissions brutes pour
            AclEntryPermission.READ_ATTRIBUTES                                   // la permission "Lecture".
    );

    private static final List<AclEntryPermission> writePermissions = List.of(    // Liste contenant toutes
            AclEntryPermission.WRITE_DATA,                                       // les permissions brutes pour
            AclEntryPermission.APPEND_DATA                                       // la permission "Écriture".
    );

    private static boolean canRead(String rawPermissions) {     // Convertie les permissions brutes d'un user en vraie permission "Lecture".
        return rawPermissions.contains(readPermissions.get(0).name()) || rawPermissions.contains(readPermissions.get(1).name());
    }

    private static boolean canWrite(String rawPermissions) {    // Convertie les permissions brutes d'un user en vraie permission "Écriture".
        return rawPermissions.contains(writePermissions.get(0).name()) || rawPermissions.contains(writePermissions.get(1).name());
    }

    private static boolean canShowFolder(String rawPermissions) {   // Convertie les permissions brutes d'un user en vraie permission "Affichage du contenu du dossier".
        return rawPermissions.contains(AclEntryPermission.EXECUTE.name()) || rawPermissions.contains(AclEntryPermission.LIST_DIRECTORY.name());
    }

    private static boolean canReadAndExecute(String rawPermissions) {   // Convertie les permissions brutes d'un user en vraie permission "Lecture et exécution".
        return rawPermissions.contains(AclEntryPermission.EXECUTE.name()) || rawPermissions.contains(AclEntryPermission.READ_DATA.name());
    }

    private static boolean canModify(String rawPermissions) {   // Convertie les permissions brutes d'un user en vraie permission "Modification".
        return canRead(rawPermissions) && canWrite(rawPermissions) && canShowFolder(rawPermissions) && rawPermissions.contains(AclEntryPermission.DELETE.name());
    }

    private static boolean hasTotalControl(String rawPermissions) {     // Convertie les permissions brutes d'un user en vraie permission "Contrôle total".
        return rawPermissions.contains(AclEntryPermission.READ_DATA.name()) && rawPermissions.contains(AclEntryPermission.WRITE_DATA.name()) &&
                rawPermissions.contains(AclEntryPermission.EXECUTE.name()) && rawPermissions.contains(AclEntryPermission.DELETE.name()) &&
                rawPermissions.contains(AclEntryPermission.READ_ACL.name()) && rawPermissions.contains(AclEntryPermission.WRITE_ACL.name()) &&
                rawPermissions.contains(AclEntryPermission.WRITE_OWNER.name());
    }

    private static String returnPermissions(AclEntry group, File file) {    // Crée l'affichage de toutes les permissions d'un utilisateur.
        String permissions = "";
        if (canRead(group.toString())) permissions += "Lecture,";
        if (canWrite(group.toString())) permissions += "Ecriture,";
        if (canShowFolder(group.toString()) && file.isDirectory()) permissions += "Affichage du contenu du dossier,";   // Cette permission est exclusive aux dossiers.
        if (canReadAndExecute(group.toString())) permissions += "Lecture et execution,";
        if (canModify(group.toString())) permissions += "Modification,";
        if (hasTotalControl(group.toString())) permissions += "Controle total,";
        return permissions;
    }

    // Permet de créer un fichier de chemin, nom et d'extension modifiable qui contiendra l'intégralité des données.
    private static void generateFileWithDatas(String path, List<String> lines) {
        String fileInput = path + "\\" + "Permissions_Dossiers_" +
                LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue() + "-" + LocalDate.now().getDayOfMonth() + "-" +
                LocalTime.now().getHour() + "-" +  LocalTime.now().getMinute() + "-"  + LocalTime.now().getSecond() + ".txt";
        File file = new File(fileInput);    // Création du fichier à l'endroit, nom et extension voulue.
        try {
            if (!file.exists()) file.createNewFile();   // On crée le fichier s'il n'existe pas déjà, mais dans tous les cas, on écrit les données dedans.
            BufferedWriter buffer = new BufferedWriter(new FileWriter(file));
            for (String line : lines) {     // On parcourt chaque ligne de la liste contenant toutes les données.
                buffer.write(line);     // On écrit la ligne actuelle dans le fichier.
            }
            buffer.close();     // On ferme le BufferedWriter pour éviter qu'il n'empiète trop sur la mémoire tampon.

            System.out.println("\nLe fichier a bien été créé avec succès au chemin suivant: \n-> " + fileInput);
        } catch (Exception e) {     // On évite les bugs avec catch
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("\n\n\n ---------- Permissions de Dossiers/Fichiers ---------- \n\n\n");

        System.out.println("Quel dossier voulez-vous inspecter ?"); // On demande à l'utilisateur le chemin du dossier où il veut inspecter les permissions de chaque utilisateur.
        String pathInput = input.nextLine(); // On stocke le chemin que l'utilisateur renseigne.

        System.out.println("\nIndiquez le chemin du fichier à créer:");
        String pathFile = input.nextLine();

        File file = new File(pathInput);      // Conversion du chemin en objet fichier, c'est le dossier à analyer.

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(file.getPath()))) {  // On check chaque fichier que contient notre dossier
            for (Path entry : stream) {
                File f = entry.toFile();
                AclFileAttributeView view = Files.getFileAttributeView(Path.of(f.getPath()), AclFileAttributeView.class);   // Permet de regarder le fichier actuel en profondeur grâce à son chemin (Path).

                try {
                    List<AclEntry> groupsWithPermissions = view.getAcl();   // Stocke une liste contenant l'intégralité des noms des groupes/utilisateurs ainsi que l'intégralité de leurs permissions brutes.

                    for (AclEntry group : groupsWithPermissions) {  // Analyse chaque groupe de la liste plus haute.
                        allDatas.add(f.getName().replace('è', 'e').replace('é', 'e').replace('à', 'a').replace('-', ' ') + ", " + group.principal().getName().replace('è', 'e').replace('é', 'e').replace('-', ' ') + "," + returnPermissions(group, f) + "\n"); // On stocke dans la variable exactement la même chose que ce que l'on affiche juste au-dessus, mais en sautant une ligne à la fin.
                    }

                } catch (Exception e) {
                    e.printStackTrace();    // On attrape l'erreur pour éviter des bugs.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        generateFileWithDatas(pathFile, allDatas);    // On génère le fichier, contenant l'intégralité des données dans la liste "allDatas".
    }

}