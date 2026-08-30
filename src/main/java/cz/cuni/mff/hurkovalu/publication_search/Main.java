/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package cz.cuni.mff.hurkovalu.publication_search;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/**
 * Main class of the PubMed Search application.
 * @author Lucie Hurkova
 */
public class Main {
    
    private static String argumentsInfo
            = """
            Incorrect arguments. Correct arguments:
            1. argument: directory containing XML files (database)
            2. argument: directory containg serialized database and models or directory for future serialization
            3. argument: name of compiled C program for computation of SVD for LSI model (optional)
            """;
    
    public static void main(String[] args) {
            
            if (args.length < 2) {
                System.err.println(argumentsInfo);
                System.exit(1);
            }
            Path dataDir = Path.of(args[0]);
            Path serDir = Path.of(args[1]);
            
            if (!Files.isDirectory(dataDir) && !Files.isReadable(dataDir)) {
                System.err.println(argumentsInfo);
                System.exit(1);
            }
            
            if (!Files.isDirectory(dataDir) && !Files.isWritable(dataDir) && !Files.isReadable(dataDir)) {
                System.err.println(argumentsInfo);
                System.exit(1);
            }
            
            Path svdsPath = null;
            if (args.length > 2) {
                svdsPath = Path.of(args[2]);
                if (!Files.isRegularFile(svdsPath) && !Files.isExecutable(svdsPath)) {
                    System.err.println(argumentsInfo);
                    System.exit(1);
                }
            }

            GUI gui = new GUI(1000, 700, "PubMed Search", dataDir, serDir, svdsPath);
            SwingUtilities.invokeLater(() -> gui.createGUI());
    }
}
